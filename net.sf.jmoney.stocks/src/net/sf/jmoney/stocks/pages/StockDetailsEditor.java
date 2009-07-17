/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2009 Nigel Westbury <westbury@users.sourceforge.net>
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

import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.views.AccountEditorInput;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;

public class StockDetailsEditor extends EditorPart {

//	static public final String ID = "net.sf.jmoney.stocks.stockBalancesEditor";
	
	/**
	 * The account being shown in this page.
	 */
	private StockAccount account;
    
	/**
	 * The stock being shown in this page.
	 */
	private Stock stock;
    
	public StockDetailsEditor(Stock stock) {
		this.stock = stock;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		
		setSite(site);
		setInput(input);
		
    	// Set the account that this page is viewing and editing.
		AccountEditorInput input2 = (AccountEditorInput)input;
        DatastoreManager sessionManager = (DatastoreManager)site.getPage().getInput();
        account = (StockAccount)sessionManager.getSession().getAccountByFullName(input2.getFullAccountName());
	}

	@Override
	public boolean isDirty() {
		// Page is never dirty
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// Will never be called because editor is never dirty.
	}

	@Override
	public void doSaveAs() {
		// Will never be called because editor is never dirty and 'save as' is not allowed anyway.
	}

	@Override
	public void createPartControl(Composite parent) {
        FormToolkit toolkit = new FormToolkit(parent.getDisplay());
    	ScrolledForm form = toolkit.createScrolledForm(parent);
        form.getBody().setLayout(new GridLayout());
        
		// Get the handler service and pass it on so that handlers can be activated as appropriate
		IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);

		
		SectionPart section = new SectionPart(form.getBody(), toolkit, ExpandableComposite.TITLE_BAR);
        section.getSection().setText("All Balances");
        section.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite contents = createContents(section.getSection());
        
		// Activate the handlers
//		IHandler handler = new NewTransactionHandler(rowTracker, fEntriesControl);
//		handlerService.activateHandler("net.sf.jmoney.newTransaction", handler);		

        section.getSection().setClient(contents);
        toolkit.paintBordersFor(contents);
        section.refresh();  // ?????

        
        
        StringBuffer formTitle = new StringBuffer();
        formTitle.append("Activity for ").append(stock.getName());
        if (stock.getSymbol() != null) {
        	formTitle.append(" (").append(stock.getSymbol()).append(")");
        }
        form.setText(formTitle.toString());
	}
 	
	private Composite createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.None);
		
		TableViewer balancesViewer = new TableViewer(composite, SWT.BORDER);
		
		return composite;
	}

	@Override
	public void setFocus() {
		// Don't bother to do anything.  User can select as required.
	}
}
