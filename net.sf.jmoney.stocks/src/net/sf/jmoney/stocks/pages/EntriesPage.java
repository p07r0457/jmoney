/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Johann Gyger <jgyger@users.sf.net>
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
package net.sf.jmoney.stocks.pages;

import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.stocks.StockAccount;
import net.sf.jmoney.views.NodeEditor;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntriesPage extends FormPage implements IBookkeepingPage {

    public static final String PAGE_ID = "entries";
    
	protected NodeEditor fEditor;

	/**
	 * The account being shown in this page.
	 */
	private StockAccount account;
	
    /**
     * Create a new page to edit entries.
     * 
     * @param editor Parent editor
     */
    public EntriesPage(NodeEditor editor) {
        super(editor, PAGE_ID, "Entries");
        fEditor = editor;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.forms.editor.FormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
     */
    @Override	
    protected void createFormContent(IManagedForm managedForm) {
    	// Set the account that this page is viewing and editing.
    	account = (StockAccount) fEditor.getSelectedObject();

    	ScrolledForm form = managedForm.getForm();
        GridLayout layout = new GridLayout();
        form.getBody().setLayout(layout);
        
        final EntriesSection fEntriesSection = new EntriesSection(form.getBody(), account, getManagedForm().getToolkit());
        fEntriesSection.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
        managedForm.addPart(fEntriesSection);
        fEntriesSection.initialize(managedForm);

        form.setText("Accounting Entries");
/* We need to get this working so we can remove that row of buttons.
 * 
 
        IToolBarManager toolBarManager = form.getToolBarManager();
        
		Action deleteAction = new Action("delete", JMoneyPlugin.createImageDescriptor("icons/TreeView.gif")) {
			public void run() {
//TODO:       					fEntriesSection.deleteTransaction();
			}
		};
		deleteAction.setToolTipText("Delete the Selected Transaction");
        toolBarManager.add(deleteAction);

        Action duplicateAction = new Action("duplicate", JMoneyPlugin.createImageDescriptor("icons/TableView.gif")) {
			public void run() {
//TODO:       					fEntriesSection.duplicateTransaction();
			}
		};
		duplicateAction.setToolTipText("Duplicate the Selected Transaction");
        toolBarManager.add(duplicateAction);

        // create the New submenu, using the same id for it as the New action
        String newText = "TTEXT";
        String newId = "TTid";
        MenuManager newMenu = new MenuManager(newText, newId) {
            public String getMenuText() {
                String result = "first text";
                String shortCut = "A";
                return result + "\t" + shortCut; //$NON-NLS-1$
            }
        };
        newMenu.add(deleteAction);
        newMenu.add(new Separator(newId));
        newMenu.add(duplicateAction);
        toolBarManager.add(newMenu);
        
		IActionBars bars = getEditorSite().getActionBars();
		
//		fillLocalPullDown(bars.getMenuManager());
		IMenuManager manager = bars.getMenuManager();
		manager.add(deleteAction);
		manager.add(new Separator());
		manager.add(duplicateAction);
		
//		fillLocalToolBar(bars.getToolBarManager());
        
        
        toolBarManager.update(false);
*/        
    }
    
    public Account getAccount () {
    	return account;
    }

	public void saveState(IMemento memento) {
		// Save view state (e.g. the sort order, the set of extension properties that are
		// displayed in the table).
	}
}