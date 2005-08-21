/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2005 Johann Gyger <jgyger@users.sourceforge.net>
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package net.sf.jmoney.actions;

import java.util.HashMap;
import java.util.Map;

import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.handlers.ShowViewHandler;

public class ShowErrorLogAction extends Action {

    public static final String ERROR_LOG_VIEW_ID = "org.eclipse.pde.runtime.LogView";

    public ShowErrorLogAction() {
        setText("Show Error Log");
        setId("net.sf.jmoney.action.showErrorLog");
    }

    public void run() {
        Map params = new HashMap();
        params.put("org.eclipse.ui.views.showView.viewId", ERROR_LOG_VIEW_ID);
        ExecutionEvent event = new ExecutionEvent(params, null, null);
        ShowViewHandler handler = new ShowViewHandler();
        try {
            handler.execute(event);
        } catch (ExecutionException e) {
            JMoneyPlugin.log(e);
        }
    }

}