/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.xugu.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Xugu physical table
 * 加载表相关物理信息（分区信息）
 */
public abstract class XuguTablePhysical extends XuguTableBase implements DBSObjectLazy<XuguDataSource>
{
    private static final Log log = Log.getLog(XuguTablePhysical.class);

    public static final String CAT_STATISTICS = "Statistics";

    //private boolean valid;
    private long rowCount;
    private Long realRowCount;
    private Object tablespace;
    private Integer partitioned;
    private PartitionInfo partitionInfo;
    private PartitionCache partitionCache;

    protected XuguTablePhysical(XuguSchema schema, String name)
    {
        super(schema, name, false);
    }

    protected XuguTablePhysical(
        XuguSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
//        this.rowCount = JDBCUtils.safeGetLong(dbResult, "NUM_ROWS");
        //this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));

        //加载表分区信息
        this.partitioned = JDBCUtils.safeGetInteger(dbResult, "PARTI_TYPE");
        this.partitionCache = this.partitioned==null ? null : new PartitionCache();
    }

    @Property(category = CAT_STATISTICS, viewable = true, order = 20)
    public long getRowCount()
    {
        return rowCount;
    }

    @Property(category = CAT_STATISTICS, viewable = false, expensive = true, order = 21)
    public synchronized Long getRealRowCount(DBRProgressMonitor monitor)
    {
        if (realRowCount != null) {
            return realRowCount;
        }
        if (!isPersisted()) {
            // Do not count rows for views
            return null;
        }

        // Query row count
        try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Read row count")) {
            realRowCount = countData(new AbstractExecutionSource(this, session.getExecutionContext(), this), session, null, DBSDataContainer.FLAG_NONE);
        } catch (DBException e) {
            log.debug("Can't fetch row count", e);
        }
        if (realRowCount == null) {
            realRowCount = -1L;
        }

        return realRowCount;
    }

    @Override
    public Object getLazyReference(Object propertyId)
    {
        return tablespace;
    }

    @Property(viewable = true, order = 22, editable = true, updatable = true, listProvider = TablespaceListProvider.class)
    @LazyProperty(cacheValidator = XuguTablespace.TablespaceReferenceValidator.class)
    public Object getTablespace(DBRProgressMonitor monitor) throws DBException
    {
        return XuguTablespace.resolveTablespaceReference(monitor, this, null);
    }

    public Object getTablespace() {
        return tablespace;
    }

    public void setTablespace(XuguTablespace tablespace) {
        this.tablespace = tablespace;
    }

    @Override
    @Association
    public Collection<XuguTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        // Read indexes using cache
        return this.getContainer().indexCache.getObjects(monitor, getContainer(), this);
    }

    public XuguTableIndex getIndex(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return this.getContainer().indexCache.getObject(monitor, getContainer(), this, name);
    }

    //获取表分区信息
    @PropertyGroup
    @LazyProperty(cacheValidator = PartitionInfoValidator.class)
    public PartitionInfo getPartitionInfo(DBRProgressMonitor monitor) throws DBException
    {
        if (partitionInfo == null && partitioned != null) {
            try (final JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load partitioning info")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM ALL_PARTIS WHERE TABLE_ID=(SELECT TABLE_ID FROM ALL_TABLES WHERE TABLE_NAME=?)")) {
                    dbStat.setString(1, getName());
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        if (dbResult.next()) {
                            partitionInfo = new PartitionInfo(monitor, this.getDataSource(), dbResult);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBException(e, getDataSource());
            }
        }
        return partitionInfo;
    }

    @Association
    public Collection<XuguTablePartition> getPartitions(DBRProgressMonitor monitor)
        throws DBException
    {
        if (partitionCache == null) {
            return null;
        } else {
            this.partitionCache.getAllObjects(monitor, this);
            this.partitionCache.loadChildren(monitor, this, null);
            return this.partitionCache.getAllObjects(monitor, this);
        }
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().indexCache.clearObjectCache(this);
    	//return this;
        return super.refreshObject(monitor);
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
        this.valid = XuguUtils.getObjectStatus(monitor, this, XuguObjectType.TABLE);
    }

    private static class PartitionCache extends JDBCStructCache<XuguTablePhysical, XuguTablePartition, XuguTablePartition> {

        protected PartitionCache()
        {
            super("PARTITION_NAME");
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguTablePhysical table) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM ALL_PARTIS " +
                "WHERE TABLE_ID=(SELECT TABLE_ID FROM ALL_TABLES WHERE TABLE_NAME=?) " +
                "ORDER BY PARTI_NO");
            dbStat.setString(1, table.getName());
            return dbStat;
        }

        @Override
        protected XuguTablePartition fetchObject(@NotNull JDBCSession session, @NotNull XuguTablePhysical table, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new XuguTablePartition(table, false, resultSet);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull XuguTablePhysical table, @Nullable XuguTablePartition forObject) throws SQLException
        {
        	String sql = "SELECT * FROM ALL_SUBPARTIS " +
                    "WHERE TABLE_ID=(SELECT TABLE_ID FROM ALL_TABLES WHERE TABLE_NAME='"+ table.getName() +"') ";
        	if(forObject != null) {
        		sql += "AND PARTITION_NAME='" + forObject.getName() + "'";
        	}
        	sql += "ORDER BY SUBPARTI_NO";
            final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
            return dbStat;
        }

        @Override
        protected XuguTablePartition fetchChild(@NotNull JDBCSession session, @NotNull XuguTablePhysical table, @NotNull XuguTablePartition parent, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
        {
            return new XuguTablePartition(table, true, dbResult);
        }

    }

    public static class PartitionInfo extends XuguPartitionBase.PartitionInfoBase {

        public PartitionInfo(DBRProgressMonitor monitor, XuguDataSource dataSource, ResultSet dbResult)
            throws DBException
        {
            super(monitor, dataSource, dbResult);
        }
    }

    public static class PartitionInfoValidator implements IPropertyCacheValidator<XuguTablePhysical> {
        @Override
        public boolean isPropertyCached(XuguTablePhysical object, Object propertyId)
        {
            return object.partitioned!=null && object.partitionInfo != null;
        }
    }

    public static class TablespaceListProvider implements IPropertyValueListProvider<XuguTablePhysical> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(XuguTablePhysical object)
        {
            final List<XuguTablespace> tablespaces = new ArrayList<>();
            try {
                tablespaces.addAll(object.getDataSource().getTablespaces(new VoidProgressMonitor()));
            } catch (DBException e) {
                log.error(e);
            }
            tablespaces.sort(DBUtils.<XuguTablespace>nameComparator());
            return tablespaces.toArray(new XuguTablespace[tablespaces.size()]);
        }
    }
}
