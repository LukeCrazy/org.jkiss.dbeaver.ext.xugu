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
import org.jkiss.dbeaver.ext.xugu.model.source.XuguSourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Map;

/**
 * OracleTrigger
 */
public abstract class XuguTrigger<PARENT extends DBSObject> extends XuguObject<PARENT> implements DBSTrigger, DBPQualifiedObject, XuguSourceObject
{
    public enum BaseObjectType {
        TABLE
    }

    public enum ActionType implements DBPNamedObject {
        PLSQL("PL/SQL"),
        CALL("CALL");

        private final String title;

        ActionType(String title)
        {
            this.title = title;
        }

        @NotNull
        @Override
        public String getName()
        {
            return title;
        }
    }

    private BaseObjectType objectType;
    private int triggerType;
    private int triggeringEvent;
//    private String columnName;
    private int obj_id;
    
//    private String refNames;
//    private String whenClause;
    private XuguObjectStatus status;
    private boolean valid;
    private boolean deleted;
    private String define;
//    private ActionType actionType;
//    private String sourceDeclaration;

    public XuguTrigger(PARENT parent, String name)
    {
        super(parent, name, false);
    }

    public XuguTrigger(
        PARENT parent,
        ResultSet dbResult)
    {
        super(parent, JDBCUtils.safeGetString(dbResult, "TRIG_NAME"), true);
        //只有一种类型触发器
        this.objectType = BaseObjectType.TABLE;
        this.triggerType = JDBCUtils.safeGetInt(dbResult, "TRIG_TYPE");
        this.triggeringEvent = JDBCUtils.safeGetInt(dbResult, "TRIG_EVENT");
        //根据obj_id获取？是否代表列id？
        this.obj_id = JDBCUtils.safeGetInt(dbResult, "TABLE_ID");
//        this.columnName = JDBCUtils.safeGetString(dbResult, "COL_NAME");
//        this.refNames = JDBCUtils.safeGetString(dbResult, "REFERENCING_NAMES");
//        this.whenClause = JDBCUtils.safeGetString(dbResult, "WHEN_CLAUSE");
        this.status = JDBCUtils.safeGetBoolean(dbResult, "ENABLE")?XuguObjectStatus.ENABLED:XuguObjectStatus.DISABLED;
        this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
//        this.deleted = JDBCUtils.safeGetBoolean(dbResult, "DELETED");
        this.define = JDBCUtils.safeGetString(dbResult, "DEFINE");
//        this.description = JDBCUtils.safeGetString(dbResult, "DESCRIPTION");
//        this.actionType = "CALL".equals(JDBCUtils.safeGetString(dbResult, "ACTION_TYPE")) ? ActionType.CALL : ActionType.PLSQL;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 5)
    public BaseObjectType getObjectType()
    {
        return objectType;
    }

    @Property(viewable = true, order = 5)
    public int getTriggerType()
    {
        return triggerType;
    }

    @Property(viewable = true, order = 6)
    public int getTriggeringEvent()
    {
        return triggeringEvent;
    }

//    @Property(viewable = true, order = 7)
//    public String getColumnName()
//    {
//        return columnName;
//    }

//    @Property(order = 8)
//    public String getRefNames()
//    {
//        return refNames;
//    }
//
//    @Property(order = 9)
//    public String getWhenClause()
//    {
//        return whenClause;
//    }
//
//    @Property(viewable = true, order = 10)
//    public XuguObjectStatus getStatus()
//    {
//        return status;
//    }

    @Nullable
    @Override
    @Property(multiline = true, order = 11)
    public String getDescription()
    {
        return define;
    }

//    @Property(viewable = true, order = 12)
//    public ActionType getActionType()
//    {
//        return actionType;
//    }

    @Override
    public XuguSourceType getSourceType()
    {
        return XuguSourceType.TRIGGER;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
//        if (define == null && monitor != null) {
//            define = XuguUtils.getSource(monitor, this, false, false);
//        }
        return this.define;
    }

    public void setObjectDefinitionText(String source)
    {
        this.define = source;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return valid? DBSObjectState.NORMAL: DBSObjectState.INVALID;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
        this.status = (XuguUtils.getObjectStatus(monitor, this, XuguObjectType.TRIGGER) ? XuguObjectStatus.ENABLED : XuguObjectStatus.DISABLED);
    }

    @Override
    public DBEPersistAction[] getCompileActions()
    {
        return new DBEPersistAction[] {
            new XuguObjectPersistAction(
                XuguObjectType.TRIGGER,
                "Compile trigger",
                "ALTER TRIGGER " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
            )};
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getSchema(),
            this);
    }

    @Override
    public String toString() {
        return getFullyQualifiedName(DBPEvaluationContext.DDL);
    }
}
