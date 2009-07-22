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

import java.util.HashMap;
import java.util.Map;

import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.model.StockEntry;
import net.sf.jmoney.stocks.model.StockEntryInfo;
import net.sf.jmoney.views.AccountEditorInput;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;

public class StockBalancesEditor extends EditorPart {

	static public final String ID = "net.sf.jmoney.stocks.stockBalancesEditor";
	
	/**
	 * The account being shown in this page.
	 */
	private StockAccount account;
    
	private class StockWrapper {
		private Stock stock;
		public long total = 0;

		public StockWrapper(Stock stock) {
			this.stock = stock;
		}
		
	}
	
	private Map<Stock, StockWrapper> totals = new HashMap<Stock, StockWrapper>();

	private TableViewer balancesViewer;

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

        
        
        
        form.setText("Investment Account Balances");
	}
	
	private Composite createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.None);
		composite.setLayout(new GridLayout());

		Control table = createTable(composite);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		for (Entry entry : account.getEntries()) {
			StockEntry entry2 = entry.getExtension(StockEntryInfo.getPropertySet(), false);
			if (entry2 != null) {
				if (entry2.isStockChange()) {
					Stock stock = entry2.getStock();
					StockWrapper stockWrapper = totals.get(stock);
					if (stockWrapper == null) {
						stockWrapper = new StockWrapper(stock);
						totals.put(stock, stockWrapper);
					}
					
					stockWrapper.total += entry.getAmount();
				}
			}
		}
		
		balancesViewer.setInput(totals.values());
		
		return composite;
	}

	private Control createTable(Composite parent) {
		Composite casesComposite = new Composite(parent, SWT.NONE);
		casesComposite.setLayout(new GridLayout());

		balancesViewer = new TableViewer(casesComposite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint = 300;
		gridData.heightHint = 100;
		balancesViewer.getTable().setLayoutData(gridData);
		
		balancesViewer.getTable().setHeaderVisible(true);
		balancesViewer.getTable().setLinesVisible(true);

		balancesViewer.setContentProvider(new ArrayContentProvider());
		
		// Sort by stock name
		balancesViewer.setComparator(new ViewerComparator() {
			@Override
		    public int compare(Viewer viewer, Object element1, Object element2) {
				StockWrapper stockWrapper1 = (StockWrapper)element1;
				StockWrapper stockWrapper2 = (StockWrapper)element2;
				return stockWrapper1.stock.getName().compareTo(stockWrapper2.stock.getName());
			}
		});
		
		TableViewerColumn stockNameColumn = new TableViewerColumn(balancesViewer, SWT.LEFT);
		stockNameColumn.getColumn().setText("Stock");
		stockNameColumn.getColumn().setWidth(300);

		stockNameColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				StockWrapper stockWrapper = (StockWrapper)cell.getElement();
				cell.setText(stockWrapper.stock.getName());
			}
		});

		TableViewerColumn balanceColumn = new TableViewerColumn(balancesViewer, SWT.LEFT);
		balanceColumn.getColumn().setText("Number of Shares");
		balanceColumn.getColumn().setWidth(100);

		balanceColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				StockWrapper stockWrapper = (StockWrapper)cell.getElement();
				cell.setText(Long.toString(stockWrapper.total));
			}
		});

		// Create the pop-up menu
		MenuManager menuMgr = new MenuManager();
		// TODO implement this action
//		menuMgr.add(new ShowDetailsAction(getSite().getWorkbenchWindow(), balancesViewer));
		menuMgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		getSite().registerContextMenu(menuMgr, balancesViewer);
			
		Control control = balancesViewer.getControl();
		Menu menu = menuMgr.createContextMenu(control);
		control.setMenu(menu);		
		
		return casesComposite;
	}

	@Override
	public void setFocus() {
		// Don't bother to do anything.  User can select as required.
	}
}
