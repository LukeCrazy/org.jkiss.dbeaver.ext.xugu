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

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKeyColumn;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

import java.util.List;

/**
 * GenericConstraintColumn
 */
public class XuguTableForeignKeyColumn extends XuguTableConstraintColumn implements DBSTableForeignKeyColumn
{

    public XuguTableForeignKeyColumn(
        XuguTableForeignKey constraint,
        XuguTableColumn tableColumn,
        int ordinalPosition)
    {
        super(constraint, tableColumn, ordinalPosition);
    }

    @Override
    @Property(id = "reference", viewable = true, order = 4)
    public XuguTableColumn getReferencedColumn()
    {
        XuguTableConstraint referencedConstraint = ((XuguTableForeignKey) getParentObject()).getReferencedConstraint();
        if (referencedConstraint != null) {
            List<XuguTableConstraintColumn> ar = referencedConstraint.getAttributeReferences(new VoidProgressMonitor());
            //修改了列下标位置计算方式
            if (ar != null) {
                return ar.get(getOrdinalPosition()).getAttribute();
            }
        }
        return null;
    }

}
