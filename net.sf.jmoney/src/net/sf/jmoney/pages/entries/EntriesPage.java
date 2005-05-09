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
package net.sf.jmoney.pages.entries;

import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.JMoneyPlugin;
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
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.views.NodeEditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
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
    
    /**
     * Display (true) or hide (false) the details of selected transaction
     * at the botom of the window. I don't like this display and want a 
     * possibility to hide it. 
     * This parameter will surely be "true" for common usage and "false" for
     * me. 
     * @author Faucheux
     */
    public static final boolean IS_ENTRY_SECTION_TO_DISPLAY = true;
    

	protected NodeEditor fEditor;

	/** Element: IEntriesTableProperty */
	protected Vector allEntryDataObjects = new Vector();

	IEntriesTableProperty debitColumnManager;
	IEntriesTableProperty creditColumnManager;
	IEntriesTableProperty balanceColumnManager;
	
    protected EntriesFilterSection fEntriesFilterSection;
    protected EntriesSection fEntriesSection;
	protected EntrySection fEntrySection;

	final EntriesFilter filter = new EntriesFilter(this);

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
	 * the transaction currently being edited, or null
	 * if no transaction is being edited
	 */
	protected Transaction currentTransaction = null;

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
        form.getBody().setLayout(layout);
        
        fEntriesFilterSection = new EntriesFilterSection(this, form.getBody());
        fEntriesFilterSection.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        managedForm.addPart(fEntriesFilterSection);
        fEntriesFilterSection.initialize(managedForm);
        
        fEntriesSection = new EntriesSection(this, form.getBody());
        fEntriesSection.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
        managedForm.addPart(fEntriesSection);
        fEntriesSection.initialize(managedForm);

        if (IS_ENTRY_SECTION_TO_DISPLAY) {
            fEntrySection = new EntrySection(form.getBody(), managedForm.getToolkit(), account.getSession(), account.getCurrency());
            fEntrySection.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            managedForm.addPart(fEntriesSection);
            fEntrySection.initialize(managedForm);
        }

        form.setText("Accounting Entries");
        
        IToolBarManager toolBarManager = form.getToolBarManager();

        toolBarManager.add(
        		new Action("tree", JMoneyPlugin.createImageDescriptor("icons/TreeView.gif")) {
        			public void run() {
       					fEntriesSection.setTreeView();
        			}
        		}
        );

        toolBarManager.add(
        		new Action("table", JMoneyPlugin.createImageDescriptor("icons/TableView.gif")) {
        			public void run() {
       					fEntriesSection.setTableView();
        			}
        		}
        );

        toolBarManager.update(false);
    }
    
    public CurrencyAccount getAccount () {
    	return account;
    }

	public void saveState(IMemento memento) {
		// Save view state (e.g. the sort order, the set of extension properties that are
		// displayed in the table).
	}

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
			Commodity commodity = EntriesPage.this.getAccount().getCurrency();

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

			Commodity commodity = EntriesPage.this.getAccount().getCurrency();
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
				Commodity commodity = EntriesPage.this.getAccount().getCurrency();
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
    }
	
	/**
	 * Commit the transaction
	 */
	public void commitTransaction() {
		// TODO make a sound
		
		transactionManager.commit();
	}
}