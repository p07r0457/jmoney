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
package net.sf.jmoney.reconciliation.reconcilePage;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.pages.entries.EntrySection;
import net.sf.jmoney.pages.entries.IDisplayableItem;
import net.sf.jmoney.pages.entries.IEntriesTableProperty;
import net.sf.jmoney.pages.entries.EntriesTree.DisplayableTransaction;
import net.sf.jmoney.reconciliation.BankStatement;
import net.sf.jmoney.reconciliation.IBankStatementSource;
import net.sf.jmoney.reconciliation.ReconciliationEntryInfo;
import net.sf.jmoney.reconciliation.ReconciliationPlugin;
import net.sf.jmoney.views.NodeEditor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Implementation of the page that reconciles bank accounts.
 * 
 * @author Nigel Westbury
 */
public class ReconcilePage extends FormPage implements IBookkeepingPage {

	/**
	 * The id for this page.  This must match the value given
	 * by the ID attribute in plugin.xml.
	 */
    public static final String PAGE_ID = "reconcile";
    
	protected NodeEditor fEditor;

	/** Element: IEntriesTableProperty */
	protected Vector allEntryDataObjects = new Vector();

	IEntriesTableProperty debitColumnManager;
	IEntriesTableProperty creditColumnManager;
	IEntriesTableProperty balanceColumnManager;
	
	protected StatementsSection fStatementsSection;
    protected StatementSection fStatementSection;
    protected UnreconciledSection fUnreconciledSection;
	protected EntrySection fEntrySection;

	/**
	 * The transaction manager used for all changes made by
	 * this page.  It is created by the page is created and
	 * remains usable for the rest of the time that this page
	 * exists.
	 */
	TransactionManager transactionManager = null;

	/**
	 * The account being shown in this page.  This account
	 * object exists in the context of transactionManager.
	 */
	private CurrencyAccount account;
	
	/**
	 * The statement currently being shown in this page.
	 * Null indicates that no statement is currently showing.
	 */
	private BankStatement statement;
	
	/**
	 * the transaction currently being edited, or null
	 * if no transaction is being edited
	 */
	protected Transaction currentTransaction = null;

	/**
	 * This is a kludge.  I do not know how to transfer a reference to a Java object,
	 * so the entry is set here by the drag source and fetched from here by the drag
	 * target.
	 */
//	protected Entry entryBeingDragged = null;

    /**
     * Create a new page to edit entries.
     * 
     * @param editor Parent editor
     */
    public ReconcilePage(NodeEditor editor) {
        super(editor, PAGE_ID, "Reconcile Bank Statement");
        fEditor = editor;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.forms.editor.FormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
     */
    protected void createFormContent(IManagedForm managedForm) {
        CurrencyAccount originalAccount = (CurrencyAccount) fEditor.getSelectedObject();

        // Create our own transaction manager.
        // This ensures that uncommitted changes
    	// made by this page are isolated from datastore usage outside
    	// of this page.
        transactionManager = new TransactionManager(originalAccount.getSession());
    	
    	// Set the account that this page is viewing and editing.
    	// We set an account object that is managed by our own
    	// transaction manager.
        account = (CurrencyAccount)transactionManager.getCopyInTransaction(originalAccount);
  
        // Set the statement to show initially.
        // If there are any entries in statements after the last
        // reconciled statement, set the first such unreconciled
        // statement in this view.  Otherwise set the statement to
        // null to indicate no statement is to be shown.
        // TODO: implement this
        statement = null;
        
    	// Build an array of all possible properties that may be
    	// displayed in the table.
        
        // Add properties from the transaction.
        for (Iterator iter = TransactionInfo.getPropertySet().getPropertyIterator3(); iter.hasNext();) {
            final PropertyAccessor propertyAccessor = (PropertyAccessor) iter.next();
            if (propertyAccessor.isScalar()) {
            	allEntryDataObjects.add(new EntriesSectionProperty(propertyAccessor, "transaction") {
					public ExtendableObject getObjectContainingProperty(IDisplayableItem data) {
						return data.getTransactionForTransactionFields();
					}
            	});
            }
        }

        // Add properties from this entry.
        // For time being, this is all the properties except the account and description
        // which come from the other entry, and the amount which is shown in the debit and
        // credit columns.
        for (Iterator iter = EntryInfo.getPropertySet().getPropertyIterator3(); iter.hasNext();) {
            PropertyAccessor propertyAccessor = (PropertyAccessor) iter.next();
            if (propertyAccessor != EntryInfo.getAccountAccessor() 
           		&& propertyAccessor != EntryInfo.getDescriptionAccessor()
        		&& propertyAccessor != EntryInfo.getAmountAccessor()) {
            	if (propertyAccessor.isScalar() && propertyAccessor.isEditable()) {
            		allEntryDataObjects.add(new EntriesSectionProperty(propertyAccessor, "this") {
    					public ExtendableObject getObjectContainingProperty(IDisplayableItem data) {
    						return data.getEntryForAccountFields();
    					}
                	});
            	}
            }
        }

        // Add properties from the other entry.
        // For time being, this is just the account and description.
        PropertySet extendablePropertySet = EntryInfo.getPropertySet();
        for (Iterator iter = extendablePropertySet.getPropertyIterator3(); iter.hasNext();) {
            PropertyAccessor propertyAccessor = (PropertyAccessor) iter.next();
            if (propertyAccessor == EntryInfo.getAccountAccessor() || propertyAccessor == EntryInfo.getDescriptionAccessor()) {
            	allEntryDataObjects.add(new EntriesSectionProperty(propertyAccessor, "other") {
					public ExtendableObject getObjectContainingProperty(IDisplayableItem data) {
						Entry entry = data.getEntryForAccountFields();
						if (entry == null) {
							// May be the new entry.
							return null;
						}
						Account account = entry.getAccount();
						if (account instanceof IncomeExpenseAccount) {
							return data.getEntryForAccountFields();
						}
						return data.getEntryForOtherFields();
					}
            	});
            }
        }

		debitColumnManager = new DebitAndCreditColumns("Debit", "debit", true);     //$NON-NLS-2$
		creditColumnManager = new DebitAndCreditColumns("Credit", "credit", false); //$NON-NLS-2$
		balanceColumnManager = new BalanceColumn();

    	
    	ScrolledForm form = managedForm.getForm();
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        form.getBody().setLayout(layout);
        
        fStatementsSection = new StatementsSection(form.getBody(), managedForm.getToolkit(), account);
        GridData data = new GridData(GridData.FILL_VERTICAL);
        data.verticalSpan = 2;
        fStatementsSection.getSection().setLayoutData(data);
        managedForm.addPart(fStatementsSection);
        fStatementsSection.initialize(managedForm);

		// Listen for double clicks.
		// Double clicking on a statement from the list will show
		// that statement in the statement table.
        fStatementsSection.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				StatementDetails statementDetails = (StatementDetails)e.item.getData();
		    	statement = statementDetails.statement;
		    	
		    	// Refresh the statement section
		    	fStatementSection.setStatement(statementDetails.statement, statementDetails.openingBalance);
			}
		});
		
		Composite actionbarContainer = new Composite(form.getBody(), 0);
		
		GridLayout actionbarLayout = new GridLayout();
		actionbarLayout.numColumns = 4;
		actionbarContainer.setLayout(actionbarLayout);
		
		final Combo fStatementsViewCombo = new Combo(actionbarContainer, SWT.DROP_DOWN);
		fStatementsViewCombo.setItems(new String [] {
				ReconciliationPlugin.getResourceString("ToolbarSection.hideStatements"),
				ReconciliationPlugin.getResourceString("ToolbarSection.showStatementsWithoutBalances"),
				ReconciliationPlugin.getResourceString("ToolbarSection.showStatementsWithBalances"),
		});
		fStatementsViewCombo.select(2);
		fStatementsViewCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				GridData gd = (GridData)fStatementsSection.getSection().getLayoutData();
				switch (fStatementsViewCombo.getSelectionIndex()) {
				case 0:
					gd.heightHint = 0;
					gd.widthHint = 0;
					break;
				case 1:
					gd.heightHint = SWT.DEFAULT;
					gd.widthHint = SWT.DEFAULT;
					fStatementsSection.showBalance(false);
					break;
				case 2:
					gd.heightHint = SWT.DEFAULT;
					gd.widthHint = SWT.DEFAULT;
					fStatementsSection.showBalance(true);
					break;
				}
				
				getManagedForm().getForm().getBody().layout(true);
			}
		});
		
		Button newStatementButton = new Button(actionbarContainer, SWT.PUSH);
		newStatementButton.setText("New Statement...");
		newStatementButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StatementDetails lastStatement = fStatementsSection.getLastStatement();
				NewStatementDialog messageBox = 
					new NewStatementDialog(getSite().getShell(), lastStatement==null ? null : lastStatement.statement);
				if (messageBox.open() == Dialog.OK) {
					statement = messageBox.getValue();
					long openingBalanceOfNewStatement = 
						lastStatement == null 
						? account.getStartBalance()
						: lastStatement.getClosingBalance();
					fStatementSection.setStatement(statement, openingBalanceOfNewStatement);
				}				
			}
		});

		final ToolBar toolBar =
			new ToolBar(actionbarContainer, SWT.FLAT);
		final ToolItem importButton =
			new ToolItem(toolBar, SWT.DROP_DOWN);
		importButton.setText("Import");
		final Menu menu = new Menu(getSite().getShell(), SWT.POP_UP);

		
		// The list of sources are taken from the net.sf.jmoney.reconciliation.bankstatements
		// extension point.
		
		// Load the extensions
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint("net.sf.jmoney.reconciliation.bankstatements");
		IExtension[] extensions = extensionPoint.getExtensions();
		
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals("statement-source")) {
					String description = elements[j].getAttribute("description");
					
					MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
					menuItem.setText(description);
					
					final IConfigurationElement thisElement = elements[j];
					
					menuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent event) {
							try {
								// Load the extension point listener for the selected source
								IBankStatementSource statementSource = (IBankStatementSource)thisElement.createExecutableExtension("class");
								Collection importedEntries = statementSource.importEntries(getSite().getShell(), getAccount());
								if (importedEntries != null) {
									for (Iterator iter = importedEntries.iterator(); iter.hasNext(); ) {
										IBankStatementSource.EntryData entryData = (IBankStatementSource.EntryData)iter.next();
						           		Session session = getAccount().getSession();
						           		
						           		// Commit any previous transaction
						           		//fPage.transactionManager.commit();
						           		
						           		Transaction transaction = session.createTransaction();
						           		Entry entry1 = transaction.createEntry();
						           		Entry entry2 = transaction.createEntry();
						           		entry1.setAccount(getAccount());
						           		entry1.setPropertyValue(ReconciliationEntryInfo.getStatementAccessor(), getStatement());
						           		entryData.assignPropertyValues(transaction, entry1, entry2);
									}
									// refresh the top table.
									// NO- updates are done through the usual event framework
									//fStatementSection.fReconciledEntriesControl.refresh();
								}
							} catch (CoreException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								throw new RuntimeException("class attribute not found");
							}
						}
					});
				}
			}
		}		  

		importButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (event.detail == SWT.ARROW) {
					Rectangle rect = importButton.getBounds();
					Point pt = new Point(rect.x, rect.y + rect.height);
					menu.setLocation(toolBar.toDisplay(pt));
					menu.setVisible(true);
				}
			}
		});

		
        Composite containerOfSash = new Composite(form.getBody(), 0);
        containerOfSash.setLayout(new FormLayout());

        // Create the sash first, so the other controls
        // can be attached to it.
        final Sash sash = new Sash(containerOfSash, SWT.BORDER | SWT.HORIZONTAL);
        FormData formData = new FormData();
        formData.left = new FormAttachment(0, 0); // Attach to left
        formData.right = new FormAttachment(100, 0); // Attach to right
        formData.top = new FormAttachment(50, 0); // Attach halfway down
        sash.setLayoutData(formData);

        sash.addSelectionListener(new SelectionAdapter() {
          public void widgetSelected(SelectionEvent event) {
          	final int mimimumHeight = 61;  // In Windows, allows 3 lines minimum.  TODO: Calculate this for other OS's
          	int y = event.y;
          	if (y < mimimumHeight) {
          		y = mimimumHeight;
          	}
          	if (y + sash.getSize().y > sash.getParent().getSize().y - mimimumHeight) {
          		y = sash.getParent().getSize().y - mimimumHeight - sash.getSize().y;
          	}
          	
            // We reattach to the top edge, and we use the y value of the event to
            // determine the offset from the top
            ((FormData) sash.getLayoutData()).top = new FormAttachment(0, y);

            // Until the parent window does a layout, the sash will not be redrawn in
            // its new location.
            sash.getParent().layout();
          }
        });

        GridData gridData1 = new GridData(GridData.FILL_BOTH);
        gridData1.heightHint = 200;   // TODO: tidy up???
        gridData1.widthHint = 200;   // TODO: tidy up???
		gridData1.grabExcessHorizontalSpace = true;
		gridData1.grabExcessVerticalSpace = true;
        containerOfSash.setLayoutData(gridData1);
        
        
        fStatementSection = new StatementSection(this, containerOfSash);
        managedForm.addPart(fStatementSection);
        fStatementSection.initialize(managedForm);

        formData = new FormData();
        formData.top = new FormAttachment(0, 0);
        formData.bottom = new FormAttachment(sash, 0);
        formData.left = new FormAttachment(0, 0);
        formData.right = new FormAttachment(100, 0);
        fStatementSection.getSection().setLayoutData(formData);

        
        
        fUnreconciledSection = new UnreconciledSection(this, containerOfSash);
        managedForm.addPart(fUnreconciledSection);
        fUnreconciledSection.initialize(managedForm);

        formData = new FormData();
        formData.top = new FormAttachment(sash, 0);
        formData.bottom = new FormAttachment(100, 0);
        formData.left = new FormAttachment(0, 0);
        formData.right = new FormAttachment(100, 0);
        fUnreconciledSection.getSection().setLayoutData(formData);

        fEntrySection = new EntrySection(form.getBody(), managedForm.getToolkit(), account.getSession(), account.getCurrency());
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        fEntrySection.getSection().setLayoutData(data);
        managedForm.addPart(fStatementSection);
        fEntrySection.initialize(managedForm);

        form.setText("Reconcile Entries against Bank Statement/Bank's Records");
/* probably no longer needed
        IToolBarManager toolBarManager = form.getToolBarManager();
        toolBarManager.add(
        		new Action("tree", JMoneyPlugin.createImageDescriptor("icons/TreeView.gif")) {
        			public void run() {
       					fStatementSection.setTreeView();
        			}
        		}
        );

        toolBarManager.add(
        		new Action("table", JMoneyPlugin.createImageDescriptor("icons/TableView.gif")) {
        			public void run() {
       					fStatementSection.setTableView();
        			}
        		}
        );

        toolBarManager.update(false);
*/        
    }
    
    public CurrencyAccount getAccount() {
    	return account;
    }

    public BankStatement getStatement() {
    	return statement;
    }

	public void saveState(IMemento memento) {
		// Save view state (e.g. the sort order, the set of extension properties that are
		// displayed in the table).
	}

	// TODO: These IEntriesTableProperty implementations are duplicated.
	// Remove the duplication.  We also should probably persist column
	// selection in a memento, and also save so that when a new account
	// is opened, the previously made selection is used.
	
	
	/**
	 * Represents a property that can be displayed in the entries table,
	 * edited by the user, or used in the filter.
	 * <P>
	 * The credit, debit, and balance columns are hard coded at the end
	 * of the table and are not represented by objects of this class.
	 * 
	 * @author Nigel Westbury
	 */
	abstract class EntriesSectionProperty implements IEntriesTableProperty {
		private PropertyAccessor accessor;
		private String id;
		
		EntriesSectionProperty(PropertyAccessor accessor, String source) {
			this.accessor = accessor;
			this.id = source + '.' + accessor.getName();
		}

		public String getText() {
			return accessor.getShortDescription();
		}

		public String getId() {
			return id;
		}

		public int getWeight() {
			return accessor.getWeight();
		}

		public int getMinimumWidth() {
			return accessor.getMinimumWidth();
		}

		/**
		 * @param entry
		 * @return
		 */
		public String getValueFormattedForTable(IDisplayableItem data) {
			ExtendableObject extendableObject = getObjectContainingProperty(data);
			if (extendableObject == null) {
				return "";
			} else {
				return accessor.formatValueForTable(extendableObject);
			}
		}

		public abstract ExtendableObject getObjectContainingProperty(IDisplayableItem data);

		/**
		 * @param table
		 * @return
		 */
		public IPropertyControl createAndLoadPropertyControl(Composite parent, IDisplayableItem data) {
			IPropertyControl propertyControl = accessor.createPropertyControl(parent); 
				
			ExtendableObject extendableObject = getObjectContainingProperty(data);

			// If the returned object is null, that means this column contains a property
			// that does not apply to this row.  Perhaps the property is the transaction date
			// and this is a split entry, or perhaps the property is
			// a property for an income and expense category but this row 
			// is a transfer transaction.
			// We return null to indicate that the cell is not editable.
			if (extendableObject == null) {
					return null;
			}
				
			propertyControl.load(extendableObject);
			
			return propertyControl;
		}

		/**
		 * @return
		 */
		public boolean isTransactionProperty() {
			return accessor.getExtendablePropertySet() == TransactionInfo.getPropertySet();
		}

		public int compare(DisplayableTransaction trans1, DisplayableTransaction trans2) {
			ExtendableObject extendableObject1 = getObjectContainingProperty(trans1);
			ExtendableObject extendableObject2 = getObjectContainingProperty(trans2);

			int result;
			
			// First deal with null cases.  If no object contains this
			// property on this row then the cell is blank.

			// Null values are sorted first.  It is necessary to put
			// null values first because empty strings are sorted first,
			// and users may not be aware of the difference.
			if (extendableObject1 == null && extendableObject2 == null) {
				return 0;
			} else if (extendableObject1 == null) {
				return -1;
			} else if (extendableObject2 == null) {
				return 1;
			}
				
			Object value1 = extendableObject1.getPropertyValue(accessor);
			Object value2 = extendableObject2.getPropertyValue(accessor);
			
			if (accessor.getCustomComparator() != null) { 
				result = accessor.getCustomComparator().compare(value1, value2);
			} else {
				if (accessor.getValueClass() == Date.class) {
					result = ((Date)value1).compareTo((Date)value2);
				} else if (accessor.getValueClass() == Integer.class) {
					result = ((Integer)value1).compareTo((Integer)value2);
				} else if (accessor.getValueClass() == Long.class) {
					result = ((Long)value1).compareTo((Long)value2);
				} else {
					// No custom comparator and not a known type, so sort according to the
					// text value that is displayed when the property is shown
					// in a table (ignoring case).
					String text1 = accessor.formatValueForTable(extendableObject1);
					String text2 = accessor.formatValueForTable(extendableObject2);
					result = text1.compareToIgnoreCase(text2);
				}
			}
			
			return result;
		}
	}

	
	/**
	 * Represents a table column that is either the debit or the credit column.
	 * Use two instances of this class instead of a single instance of the
	 * above <code>EntriesSectionProperty</code> class if you want the amount to be
	 * displayed in seperate debit and credit columns.
	 */
	class DebitAndCreditColumns implements IEntriesTableProperty {
		private String id;
		private String name;
		private boolean isDebit;
		
		DebitAndCreditColumns(String id, String name, boolean isDebit) {
			this.id = id;
			this.name = name;
			this.isDebit = isDebit;
		}
		
		public String getText() {
			return name;
		}

		public String getId() {
			return id;
		}

		public int getWeight() {
			return 2;
		}

		public int getMinimumWidth() {
			return 70;
		}

		public String getValueFormattedForTable(IDisplayableItem data) {
			Entry entry = data.getEntryForAccountFields();
			if (entry == null) {
				return "";
			}
			
			long amount = entry.getAmount();
			Commodity commodity = ReconcilePage.this.getAccount().getCurrency();

			if (isDebit) {
				return amount < 0 ? commodity.format(-amount) : "";
			} else {
				return amount > 0 ? commodity.format(amount) : "";
			}
		}

		public IPropertyControl createAndLoadPropertyControl(Composite parent, IDisplayableItem data) {
			// This is the entry whose amount is being edited by
			// this control.
			final Entry entry = data.getEntryForAccountFields();
			if (entry == null) {
				return null;
			}
			
			long amount = entry.getAmount();

			final Text textControl = new Text(parent, SWT.NONE);

			Commodity commodity = ReconcilePage.this.getAccount().getCurrency();
			if (isDebit) {
				// Debit column
				textControl.setText(amount < 0 
						? commodity.format(-amount) 
								: ""
				);
			} else {
				// Credit column
				textControl.setText(amount > 0 
						? commodity.format(amount) 
								: ""
				);
			}

			IPropertyControl propertyControl = new IPropertyControl() {
				public Control getControl() {
					return textControl;
				}
				public void load(ExtendableObject object) {
					throw new RuntimeException();
				}
				public void save() {
					// We need a currency so that we can format the amount.
					// Get the currency from this entry if possible.
					// However, the user may not have yet entered enough information
					// to determine the currency for this entry, in which case
					// use the currency for the account being listed in this editor.
					// FIXME change this when we can get the currency for income/expense
					// accounts.
					Commodity commodityForFormatting = null;
					if (entry.getAccount() != null
							&& entry.getAccount() instanceof CapitalAccount) {
						commodityForFormatting = entry.getCommodity();
					}
					if (commodityForFormatting == null) {
						commodityForFormatting = getAccount().getCurrency();
					}
					
					String amountString = textControl.getText();
					long amount = commodityForFormatting.parse(amountString);
					
					long previousEntryAmount = entry.getAmount();
					long newEntryAmount;
					
					if (isDebit) {
						if (amount != 0) {
							newEntryAmount = -amount;
						} else {
							if (previousEntryAmount < 0) { 
								newEntryAmount  = 0;
							} else {
								newEntryAmount = previousEntryAmount;
							}
						}
					} else {
						if (amount != 0) {
							newEntryAmount = amount;
						} else {
							if (previousEntryAmount > 0) { 
								newEntryAmount  = 0;
							} else {
								newEntryAmount = previousEntryAmount;
							}
						}
					}

					entry.setAmount(newEntryAmount);

					// If there are two entries in the transaction and
					// if both entries have accounts in the same currency or
					// one or other account is not known or one or other account
					// is a multi-currency account then we set the amount in
					// the other entry to be the same but opposite signed amount.
					
					if (entry.getTransaction().hasTwoEntries()) {
						Entry otherEntry = entry.getTransaction().getOther(entry);
						Commodity commodity1 = entry.getCommodity();
						Commodity commodity2 = otherEntry.getCommodity();
						if (commodity1 == null || commodity2 == null || commodity1.equals(commodity2)) {
							otherEntry.setAmount(-newEntryAmount);
						}
					}
				}
			};
			
			return propertyControl;
		}

		public boolean isTransactionProperty() {
			return false;
		}

		public int compare(DisplayableTransaction trans1, DisplayableTransaction trans2) {
			long amount1 = trans1.getEntryForAccountFields().getAmount();
			long amount2 = trans2.getEntryForAccountFields().getAmount();
			
			int result;
			if (amount1 < amount2) {
				result = -1;
			} else if (amount1 > amount2) {
				result = 1;
			} else {
				result = 0;
			}

			// If debit column then reverse.  Ascending sort should
			// result in the user seeing ascending numbers in the
			// sorted column.
			if (isDebit) {
				result = -result;
			}
			
			return result;
		}
    }
	
	/**
	 * Represents a table column that is the account balance.
	 */
	class BalanceColumn implements IEntriesTableProperty {
		public String getText() {
			return "Balance";
		}

		public String getId() {
			return "balance"; //$NON-NLS-1$
		}

		public int getWeight() {
			return 2;
		}

		public int getMinimumWidth() {
			return 70;
		}

		public String getValueFormattedForTable(IDisplayableItem data) {
		    if (data.isBalanceAffected()) {
				Commodity commodity = ReconcilePage.this.getAccount().getCurrency();
		        return commodity.format(data.getBalance());
		    } else { 
				// Display an empty cell in this column for the entry rows
		        return "";
		    }
		}

		public IPropertyControl createAndLoadPropertyControl(Composite parent, IDisplayableItem data) {
			// This column is not editable so return null
			return null;
		}

		public boolean isTransactionProperty() {
			// This is displayed on transaction lines only,
			// 
			return false;
		}

		public int compare(DisplayableTransaction trans1, DisplayableTransaction trans2) {
			// Entries lists cannot be sorted based on the balance.
			// The caller should not do this.
			throw new RuntimeException("internal error - attempt to sort on balance");
		}
    }
	
	/**
	 * Commit the transaction
	 */
	public void commitTransaction() {
		// TODO make a sound
		
		transactionManager.commit();
	}
}