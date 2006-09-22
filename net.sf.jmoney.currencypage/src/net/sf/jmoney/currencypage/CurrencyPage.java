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

package net.sf.jmoney.currencypage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;

import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.IBookkeepingPageFactory;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.CurrencyInfo;
import net.sf.jmoney.fields.SessionInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.SessionChangeListener;
import net.sf.jmoney.views.NodeEditor;
import net.sf.jmoney.views.SectionlessPage;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * There are two big flaws in this page implementation.
 * - We do not listen for changes that may indicate that a currency
 *   that was unused is now in use, or a currency that was in use
 *   is now unused.
 * - All changes to the tables should be made inside the session
 *   listener.  This ensures that if another plug-in makes a change
 *   to the data, the change is reflected in this page.
 * 
 * @author Nigel Westbury
 */
public class CurrencyPage implements IBookkeepingPageFactory {
	
	private static final String PAGE_ID = "net.sf.jmoney.currencypage.currencies";
	
	
	public void init(IMemento memento) {
		// No view state to restore
	}
	
	public void saveState(IMemento memento) {
		// No view state to save
	}
	
	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPage#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public IBookkeepingPage createFormPage(NodeEditor editor, IMemento memento) {
		SectionlessPage formPage = new CurrencyFormPage(
				editor,
				PAGE_ID);
		
		try {
			editor.addPage(formPage);
		} catch (PartInitException e) {
			JMoneyPlugin.log(e);
			// TODO: cleanly leave out this page.
		}
		
		return formPage;
	}
	
	private class CurrencyFormPage extends SectionlessPage {

		class SelectedContentProvider implements IStructuredContentProvider {
	        public void dispose() {
	        }
	        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	        }
	        /** 
	         * Returns the currencies that are available in this accounting datastore.
	         */
	        public Object[] getElements(Object parent) {
	            Vector<ISOCurrencyData> currencies = new Vector<ISOCurrencyData>();

	            for (Iterator iter = allIsoCurrencies.iterator(); iter.hasNext(); ) {
	            	ISOCurrencyData isoCurrency = (ISOCurrencyData)iter.next();
	            	if (isoCurrency.currency != null) {
	            		currencies.add(isoCurrency);
	            	}
	    		}
	    			
	            return currencies.toArray();
	        }
	    }

		class ISOCurrencyData {
			String name;
			String code;
			int decimals;
			
			/** null if this currency is not selected into the session */
			Currency currency;
			
			public String toString() {
				return name + " (" + code + ")";
			}
		}
		
		class AvailableContentProvider implements IStructuredContentProvider {
	        public void dispose() {
	        }
	        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	        }
	        /** 
	         * Returns the currencies that are available in this accounting datastore.
	         */
	        public Object[] getElements(Object parent) {
	            Vector<ISOCurrencyData> currencies = new Vector<ISOCurrencyData>();

	            for (Iterator iter = allIsoCurrencies.iterator(); iter.hasNext(); ) {
	            	ISOCurrencyData isoCurrency = (ISOCurrencyData)iter.next();
	            	if (isoCurrency.currency == null) {
	            		currencies.add(isoCurrency);
	            	}
	    		}
	    			
	            return currencies.toArray();
	        }
	    }

	    class CurrencyLabelProvider extends LabelProvider implements ITableLabelProvider {
	        protected NumberFormat nf = DecimalFormat.getCurrencyInstance();

	        public String getColumnText(Object obj, int index) {
	        	switch (index) {
	        	case 0: // name
	        		return obj.toString();
	        	case 1: // 'used', 'unused', or 'default'
		        	ISOCurrencyData currencyData = (ISOCurrencyData)obj;
		        	Currency currency = currencyData.currency;
	        		if (currency.equals(CurrencyFormPage.this.session.getDefaultCurrency())) {
	        			return "default";
	        		} else {
	        			return usedCurrencies.contains(currency) 
						? "in use"
								: "unused";
	        		}
	        	}
	        	
	        	return ""; //$NON-NLS-1$
	        }

	        public Image getColumnImage(Object obj, int index) {
	            return null;
	        }
	    }


		
		private class CurrencySorter extends ViewerSorter {
			public boolean isSorterProperty(Object element, String property) {
				return property.equals("name");
			}
		}
		
		private TableViewer availableListViewer;

		private Label countLabel;

		protected TableViewer selectedListViewer;

		private Session session;

		/**
		 * Set of currencies that are used in some way in the current session.
		 * These currencies cannot be removed from the session.
		 */
		private Set<Currency> usedCurrencies = new HashSet<Currency>();
		
		private Vector<ISOCurrencyData> allIsoCurrencies = new Vector<ISOCurrencyData>();
		
		private SessionChangeListener listener =
			new SessionChangeAdapter() {
			public void objectChanged(ExtendableObject changedObject, ScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
				if (changedObject.equals(session)
						&& changedProperty == SessionInfo.getDefaultCurrencyAccessor()) {
					TableItem[] items = selectedListViewer.getTable().getItems();
					for (int i = 0; i < items.length; i++) {
						ISOCurrencyData currencyData = (ISOCurrencyData)items[i].getData();
						if (currencyData.currency.equals(oldValue)
								|| currencyData.currency.equals(newValue)) {
							selectedListViewer.update(currencyData, null);
						}
					}
				}
			}
			
			public void objectInserted(ExtendableObject newObject) {
				// TODO: currency added
			}

			public void objectRemoved(ExtendableObject deletedObject) {
				// TODO: currency removed
			}

		};
		
		CurrencyFormPage(
				NodeEditor editor,
				String pageId) {
			super(editor,
					pageId, 
					CurrencyPagePlugin.getResourceString("CurrencyPage.tabText"), 
					CurrencyPagePlugin.getResourceString("CurrencyPage.title"));
			
			this.session = JMoneyPlugin.getDefault().getSession();
		}
		
		public Composite createControl(Object nodeObject, Composite parent, FormToolkit toolkit, IMemento memento) {
			Composite container = toolkit.createComposite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.numColumns = 3;
			layout.makeColumnsEqualWidth = false;
			layout.horizontalSpacing = 5;
			layout.verticalSpacing = 10;
			container.setLayout(layout);
					
			// Calculate the list of used currencies.
			findUsedCurrencies(session.getAccountCollection(), usedCurrencies);

			// Build the list of available ISO 4217 currencies.
    	    ResourceBundle NAME =
    	    	ResourceBundle.getBundle("net.sf.jmoney.resources.Currency");

            InputStream in = JMoneyPlugin.class.getResourceAsStream("Currencies.txt");
    		BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
    		try {
    			String line = buffer.readLine();
    			while (line != null) {
    				String code = line.substring(0, 3);
    				
    				int decimals;
    				try {
    					decimals = Byte.parseByte(line.substring(4, 5));
    				} catch (Exception ex) {
    					decimals = 2;
    				}
    				
    				ISOCurrencyData isoCurrency = new ISOCurrencyData();
    				isoCurrency.name = NAME.getString(code);
    				isoCurrency.code = code;
    				isoCurrency.decimals = decimals;
    				isoCurrency.currency = session.getCurrencyForCode(code);
    				allIsoCurrencies.add(isoCurrency);
    				
    				line = buffer.readLine();
    			}
    		} catch (IOException e) {
    			
    		}
			
			
			createAvailableList(toolkit, container).setLayoutData(new GridData(GridData.FILL_BOTH));
			createButtonArea(toolkit, container);
			createSelectedList(toolkit, container).setLayoutData(new GridData(GridData.FILL_BOTH));
			updateCount();
			
			// Listen for changes to the session data.
			session.getObjectKey().getSessionManager().addChangeListener(listener, parent);
			
			/*
			 * Listen for events on the tables. The user may double click
			 * currencies from the available list to add them or may click
			 * double click on currencies in the selected list (provided the
			 * currency is not in use) to de-selected the currency.
			 */
			availableListViewer.addDoubleClickListener(new IDoubleClickListener() {
				public void doubleClick(DoubleClickEvent event) {
					handleAdd();
				}
			});
					
			selectedListViewer.addDoubleClickListener(new IDoubleClickListener() {
				public void doubleClick(DoubleClickEvent event) {
					handleRemove();
				}
			});
			
			Dialog.applyDialogFont(container);
			
			// Set up the context menus.
			hookContextMenu(fEditor.getSite());
			
			return container;
		}
		
		/**
		 * Scan through an iteration of accounts and find all the
		 * currencies that are used.  Sub accounts are searched too.
		 * 
		 * @param accountIterator
		 * @param usedCurrencies
		 */
		private void findUsedCurrencies(Collection accounts, Set<Currency> usedCurrencies) {
			for (Iterator accountIterator = accounts.iterator(); accountIterator.hasNext(); ) {
				Account account = (Account)accountIterator.next();
				if (account instanceof IncomeExpenseAccount) {
					IncomeExpenseAccount a = (IncomeExpenseAccount)account;
					if (a.isMultiCurrency()) {
						// TODO: The ability to iterate over entries is currently only
						// available for capital accounts.  We need it for income and
						// expense accounts too.
/*						
						for (Iterator iter2 = a.getEntries.iterator(); iter2.hasNext(); ) {
							Entry entry = (Entry)iter2.next();
							Commodity commodity = entry.getCommodity();
							if (commodity instanceof Currency) {
								usedCurrencies.add(commodity);
							}
						}
*/
					} else {
						usedCurrencies.add(a.getCurrency());
					}
				} else if (account instanceof CurrencyAccount) {
					usedCurrencies.add(((CurrencyAccount)account).getCurrency());
				} else {
					CapitalAccount a = (CapitalAccount)account;
					for (Iterator iter2 = a.getEntries().iterator(); iter2.hasNext(); ) {
						Entry entry = (Entry)iter2.next();
						Commodity commodity = entry.getCommodity();
						if (commodity instanceof Currency) {
							usedCurrencies.add((Currency)commodity);
						}
					}
				}
				
				// Search the sub-accounts
				findUsedCurrencies(account.getSubAccountCollection(), usedCurrencies);
			}
		}

		public void saveState(IMemento memento) {
			// We could save the current category selection
			// and the expand/collapse state of each node
			// but it is not worthwhile.
		}

		private Composite createAvailableList(FormToolkit toolkit, Composite parent) {
			Composite container = toolkit.createComposite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			container.setLayout(layout);
			container.setLayoutData(new GridData());

			toolkit.createLabel(container, CurrencyPagePlugin
                    .getResourceString("CurrencyPage.isoCurrencyList"), //$NON-NLS-1$ 
                    SWT.NONE);

			Table table = toolkit.createTable(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
			GridData gd = new GridData(GridData.FILL_BOTH);
			gd.widthHint = 225;
			gd.heightHint = 200;
			table.setLayoutData(gd);

			availableListViewer = new TableViewer(table);
			availableListViewer.setLabelProvider(new LabelProvider());
			availableListViewer.setContentProvider(new AvailableContentProvider());
			availableListViewer.setInput(session);
			availableListViewer.setSorter(new CurrencySorter());

			return container;
		}
		
		
		private Composite createButtonArea(FormToolkit toolkit, Composite parent) {
			Composite comp = toolkit.createComposite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginWidth = layout.marginHeight = 0;
			comp.setLayout(layout);
			comp.setLayoutData(new GridData(GridData.FILL_VERTICAL));
			
			Composite container = toolkit.createComposite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.marginWidth = 0;
			layout.marginHeight = 30;
			container.setLayout(layout);
			container.setLayoutData(new GridData(GridData.FILL_BOTH));
			
			Button button;

			button = toolkit.createButton(container, CurrencyPagePlugin.getResourceString("CurrencyPage.add"), SWT.PUSH); //$NON-NLS-1$
			button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleAdd();
				}
			});
			setButtonDimensionHint(button);
			
			button = toolkit.createButton(container, CurrencyPagePlugin.getResourceString("CurrencyPage.addAll"), SWT.PUSH); //$NON-NLS-1$
			button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleAddAll();
				}
			});
			setButtonDimensionHint(button);
			
			button = toolkit.createButton(container, CurrencyPagePlugin.getResourceString("CurrencyPage.remove"), SWT.PUSH); //$NON-NLS-1$
			button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleRemove();
				}
			});
			setButtonDimensionHint(button);
			
			button = toolkit.createButton(container, CurrencyPagePlugin.getResourceString("CurrencyPage.removeUnused"), SWT.PUSH); //$NON-NLS-1$
			button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleRemoveUnused();
				}
			});
			setButtonDimensionHint(button);
			
			// Create some extra space between groups of buttons
			toolkit.createLabel(container, null, SWT.NONE);
			
			button = toolkit.createButton(container, CurrencyPagePlugin.getResourceString("CurrencyPage.setDefault"), SWT.PUSH); //$NON-NLS-1$
			button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleSetDefault();
				}
			});
			setButtonDimensionHint(button);
			
			countLabel = toolkit.createLabel(comp, null, SWT.NONE);
			countLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));		
			return container;
		}
		
		/**
		 * Sets width and height hint for the button control.
		 * <b>Note:</b> This is a NOP if the button's layout data is not
		 * an instance of <code>GridData</code>.
		 * 
		 * @param	the button for which to set the dimension hint
		 */
		public void setButtonDimensionHint(Button button) {
			Dialog.applyDialogFont(button);
			Object gd = button.getLayoutData();
			if (gd instanceof GridData) {
				((GridData) gd).widthHint = getButtonWidthHint(button);
			}
		}

		/**
		 * Returns a width hint for a button control.
		 */
		public int getButtonWidthHint(Button button) {
			if (button.getFont().equals(JFaceResources.getDefaultFont()))
				button.setFont(JFaceResources.getDialogFont());
			PixelConverter converter= new PixelConverter(button);
			int widthHint= converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			return Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		}

		public class PixelConverter {
			
			private org.eclipse.swt.graphics.FontMetrics fFontMetrics;
			
			public PixelConverter(Control control) {
				GC gc = new GC(control);
				gc.setFont(control.getFont());
				fFontMetrics= gc.getFontMetrics();
				gc.dispose();
			}
			
				
			/**
			 * @see DialogPage#convertHeightInCharsToPixels
			 */
			public int convertHeightInCharsToPixels(int chars) {
				return Dialog.convertHeightInCharsToPixels(fFontMetrics, chars);
			}

			/**
			 * @see DialogPage#convertHorizontalDLUsToPixels
			 */
			public int convertHorizontalDLUsToPixels(int dlus) {
				return Dialog.convertHorizontalDLUsToPixels(fFontMetrics, dlus);
			}

			/**
			 * @see DialogPage#convertVerticalDLUsToPixels
			 */
			public int convertVerticalDLUsToPixels(int dlus) {
				return Dialog.convertVerticalDLUsToPixels(fFontMetrics, dlus);
			}
			
			/**
			 * @see DialogPage#convertWidthInCharsToPixels
			 */
			public int convertWidthInCharsToPixels(int chars) {
				return Dialog.convertWidthInCharsToPixels(fFontMetrics, chars);
			}	
		}
		
		protected Composite createSelectedList(FormToolkit toolkit, Composite parent) {
			Composite container = toolkit.createComposite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			container.setLayout(layout);
			container.setLayoutData(new GridData(GridData.FILL_BOTH));
			
			toolkit.createLabel(container, CurrencyPagePlugin
                    .getResourceString("CurrencyPage.selectedList"), SWT.NONE); //$NON-NLS-1$ 

			Table table = toolkit.createTable(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
			GridData gd = new GridData(GridData.FILL_BOTH);
			gd.widthHint = 225;
			gd.heightHint = 200;
			table.setLayoutData(gd);

	        TableLayout tlayout = new TableLayout();
	        
	        new TableColumn(table, SWT.LEFT);
	        tlayout.addColumnData(new ColumnWeightData(0, 100));

	        new TableColumn(table, SWT.LEFT);
	        tlayout.addColumnData(new ColumnWeightData(0, 70));

	        table.setLayout(tlayout);

	        selectedListViewer = new TableViewer(table);
			selectedListViewer.setLabelProvider(new CurrencyLabelProvider());
			selectedListViewer.setContentProvider(new SelectedContentProvider());
			selectedListViewer.setInput(session);
			selectedListViewer.setSorter(new CurrencySorter());
			return container;
		}
		
		protected void pageChanged() {
			updateCount();
		}

		private void updateCount() {
			countLabel.setText(
				CurrencyPagePlugin.getFormattedMessage(
					"CurrencyPage.count", //$NON-NLS-1$
					new String[] {
						new Integer(selectedListViewer.getTable().getItemCount()).toString(),
						new Integer(allIsoCurrencies.size()).toString()}));
			countLabel.getParent().layout();
		}
		
		private void handleAdd() {
			IStructuredSelection ssel = (IStructuredSelection)availableListViewer.getSelection();
			if (ssel.size() > 0) {
				Table table = availableListViewer.getTable();
				int index = table.getSelectionIndices()[0];
				for (Iterator iter = ssel.iterator(); iter.hasNext(); ) {
					ISOCurrencyData currencyData = (ISOCurrencyData) iter.next();
					Currency newCurrency = (Currency)session.createCommodity(CurrencyInfo.getPropertySet());
					newCurrency.setName(currencyData.name);
					newCurrency.setCode(currencyData.code);
					newCurrency.setDecimals(currencyData.decimals);
					currencyData.currency = newCurrency;
				}

				selectedListViewer.add(ssel.toArray());
				availableListViewer.remove(ssel.toArray());

				table.setSelection(index < table.getItemCount() ? index : table.getItemCount() -1);
				pageChanged();
			}		
		}

		private void handleAddAll() {
			TableItem[] items = availableListViewer.getTable().getItems();

			ArrayList<ISOCurrencyData> data = new ArrayList<ISOCurrencyData>();
			for (TableItem item: items) {
				ISOCurrencyData currencyData = (ISOCurrencyData) item.getData();

				data.add(currencyData);

				Currency newCurrency = (Currency)session.createCommodity(CurrencyInfo.getPropertySet());
				newCurrency.setName(currencyData.name);
				newCurrency.setCode(currencyData.code);
				newCurrency.setDecimals(currencyData.decimals);
				currencyData.currency = newCurrency;
			}
			
			if (data.size() > 0) {
				selectedListViewer.add(data.toArray());
				availableListViewer.remove(data.toArray());
				pageChanged();
			}
		}
		
		private void handleRemove() {
			IStructuredSelection ssel = (IStructuredSelection)selectedListViewer.getSelection();
			if (ssel.size() > 0) {
				Table table = selectedListViewer.getTable();
				int index = table.getSelectionIndices()[0];
				
				Object [] isoCurrencies = ssel.toArray();
				ArrayList<ISOCurrencyData> data = new ArrayList<ISOCurrencyData>();
				for (int i = 0; i < ssel.size(); i++) {
					ISOCurrencyData currencyData = (ISOCurrencyData)isoCurrencies[i];
					if (!usedCurrencies.contains(currencyData.currency)
							&& !currencyData.currency.equals(session.getDefaultCurrency())) {
						data.add(currencyData);
						session.deleteCommodity(currencyData.currency);
						currencyData.currency = null;
					} else {
						MessageDialog dialog = new MessageDialog(
								this.getSite().getShell(),
								"Disallowed Action",
								null, // accept the default window icon
								"You cannot remove the default currency or any currencies that are in use.",
								MessageDialog.ERROR,
								new String[] { IDialogConstants.OK_LABEL }, 0);
						dialog.open();
					}
				}
				
				if (data.size() > 0) {
					availableListViewer.add(data.toArray());
					selectedListViewer.remove(data.toArray());
					table.setSelection(index < table.getItemCount() ? index : table.getItemCount() -1);
					pageChanged();
				}		
			}		
		}
		
		private void handleRemoveUnused() {
			TableItem[] items = selectedListViewer.getTable().getItems();
			
			ArrayList<ISOCurrencyData> data = new ArrayList<ISOCurrencyData>();
			for (int i = 0; i < items.length; i++) {
				ISOCurrencyData currencyData = (ISOCurrencyData)items[i].getData();
				if (!usedCurrencies.contains(currencyData.currency)
						&& !currencyData.currency.equals(session.getDefaultCurrency())) {
					data.add(currencyData);
					session.deleteCommodity(currencyData.currency);
					currencyData.currency = null;
				}
			}
			if (data.size() > 0) {
				availableListViewer.add(data.toArray());
				selectedListViewer.remove(data.toArray());
				pageChanged();
			}		
		}
		
		private void handleSetDefault() {
			// If more than one currency is selected, set the
			// first currency in the selection as the default.
			IStructuredSelection ssel = (IStructuredSelection)selectedListViewer.getSelection();
			if (ssel.size() > 0) {
				Table table = selectedListViewer.getTable();
				TableItem items [] = table.getSelection();
				Currency newDefaultCurrency = ((ISOCurrencyData)items[0].getData()).currency;
				session.setDefaultCurrency(newDefaultCurrency);
				
				pageChanged();
			}		
		}
		
		private void hookContextMenu(IWorkbenchPartSite site) {
			MenuManager menuMgr = new MenuManager("#PopupMenu");
			menuMgr.setRemoveAllWhenShown(true);
			menuMgr.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager manager) {
					CurrencyFormPage.this.fillContextMenu(manager);
				}
			});
			Menu menu = menuMgr.createContextMenu(selectedListViewer.getControl());
			selectedListViewer.getControl().setMenu(menu);
			
			site.registerContextMenu(menuMgr, selectedListViewer);
		}
		
		private void fillContextMenu(IMenuManager manager) {
			// Other plug-ins can contribute their actions here
			manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		}
	};
}
