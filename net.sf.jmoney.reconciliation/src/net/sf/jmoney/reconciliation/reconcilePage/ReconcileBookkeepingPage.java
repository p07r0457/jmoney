/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.reconciliation.reconcilePage;

import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.IBookkeepingPageFactory;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.views.NodeEditor;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;

/**
 * Provides an implement of the IBookkeepingPageFactory interface,
 * providing an exension to the net.sf.jmoney.pages extension point.
 * This extension adds the page that reconciles bank accounts.
 * 
 * @author Nigel Westbury
 */
public class ReconcileBookkeepingPage implements IBookkeepingPageFactory {

    /* (non-Javadoc)
     * @see net.sf.jmoney.IBookkeepingPage#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
     */
    public IBookkeepingPage createFormPage(NodeEditor editor, IMemento memento) {
    	// Create the page only if the properties for this account
    	// indicate that this account has statements that can be
    	// reconciled.
    	ReconcilePage formPage = new ReconcilePage(editor);
    	
    	try {
    		editor.addPage(formPage);
    	} catch (PartInitException e) {
    		JMoneyPlugin.log(e);
    		// TODO: cleanly leave out this page.
    	}
    	
    	return formPage;
    }
}