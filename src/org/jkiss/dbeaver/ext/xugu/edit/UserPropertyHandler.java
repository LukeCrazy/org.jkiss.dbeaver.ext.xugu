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
package org.jkiss.dbeaver.ext.xugu.edit;

import org.jkiss.dbeaver.ext.xugu.model.XuguUser;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyReflector;
import org.jkiss.utils.CommonUtils;

/**
* User property handler
*/
public enum UserPropertyHandler implements DBEPropertyHandler<XuguUser>, DBEPropertyReflector<XuguUser> {
    NAME,
    HOST,
    PASSWORD,
    PASSWORD_CONFIRM,
    MAX_QUERIES,
    MAX_UPDATES,
    MAX_CONNECTIONS,
    MAX_USER_CONNECTIONS;


    @Override
    public Object getId()
    {
        return name();
    }

    @Override
    public XuguCommandChangeUser createCompositeCommand(XuguUser object)
    {
        return new XuguCommandChangeUser(object);
    }

    @Override
    public void reflectValueChange(XuguUser object, Object oldValue, Object newValue)
    {
        if (this == NAME) {
            if (this == NAME) {
                object.setName(CommonUtils.toString(newValue));
            }
            DBUtils.fireObjectUpdate(object);
        }
    }
}
