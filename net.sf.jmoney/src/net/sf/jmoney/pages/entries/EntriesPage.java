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

import java.util.Vector;

import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.OtherEntriesPropertyBlock;
import net.sf.jmoney.entrytable.PropertyBlock;
import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.views.NodeEditor;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
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

	protected Vector<IndividualBlock> allEntryDataObjects = new Vector<IndividualBlock>();

    protected EntriesFilterSection fEntriesFilterSection;
    protected EntriesSection fEntriesSection;

	final EntriesFilter filter = new EntriesFilter(this);

	/**
	 * The account being shown in this page.
	 */
	private CurrencyAccount account;
	
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

    	// Set the account that this page is viewing and editing.
        account = originalAccount;
        
    	// Build an array of all possible properties that may be
    	// displayed in the table.
        
        // Add properties from the transaction.
        for (ScalarPropertyAccessor propertyAccessor: TransactionInfo.getPropertySet().getScalarProperties3()) {
        	allEntryDataObjects.add(new PropertyBlock(propertyAccessor, "transaction") {
        		public ExtendableObject getObjectContainingProperty(EntryData data) {
        			return data.getEntry().getTransaction();
        		}
        	});
        }

        // Add properties from this entry.
        // For time being, this is all the entry properties except the account and description
        // which come from the other entry, and the amount which is shown in the debit and
        // credit columns.
   		for (ScalarPropertyAccessor propertyAccessor: EntryInfo.getPropertySet().getScalarProperties3()) {
            if (propertyAccessor != EntryInfo.getAccountAccessor() 
           		&& propertyAccessor != EntryInfo.getDescriptionAccessor()
           		&& propertyAccessor != EntryInfo.getIncomeExpenseCurrencyAccessor()
        		&& propertyAccessor != EntryInfo.getAmountAccessor()) {
            	if (propertyAccessor.isScalar() && propertyAccessor.isEditable()) {
            		allEntryDataObjects.add(new PropertyBlock(propertyAccessor, "this") {
    					public ExtendableObject getObjectContainingProperty(EntryData data) {
    						return data.getEntry();
    					}
                	});
            	}
            }
        }

        /* Add properties that show values from the other entries.
         * These are the account, description, and amount properties.
         * 
         * I don't know what to do if there are other capital accounts
         * (a transfer or a purchase with money coming from more than one account).
         */
   		allEntryDataObjects.add(new OtherEntriesPropertyBlock(EntryInfo.getAccountAccessor()) {
   			public IPropertyControl createPropertyControl(Composite parent, Entry otherEntry) {
   				IPropertyControl control = EntryInfo.getAccountAccessor().createPropertyControl(parent);
   				control.load(otherEntry);
   				return control;
   			}
   		});
   		allEntryDataObjects.add(new OtherEntriesPropertyBlock(EntryInfo.getDescriptionAccessor()) {
   			public IPropertyControl createPropertyControl(Composite parent, Entry otherEntry) {
   				IPropertyControl control = EntryInfo.getDescriptionAccessor().createPropertyControl(parent);
   				control.load(otherEntry);
   				return control;
   			}
   		});
   		allEntryDataObjects.add(new OtherEntriesPropertyBlock(EntryInfo.getAmountAccessor()) {
   			public IPropertyControl createPropertyControl(Composite parent, Entry otherEntry) {
   				IPropertyControl control = EntryInfo.getAmountAccessor().createPropertyControl(parent);
   				control.load(otherEntry);
   				return control;
   			}
   		});

        /*
		 * Add the currency column. This is placed just before the amount, which
		 * is the logical place as it is the currency for the amount. The
		 * currency has special processing because the currency is not always
		 * applicable. The currency applies only if the account is an income and
		 * expense account with the multi-currency flag set or if the account is
		 * a capital account but not a currency account. In either case, the
		 * list of currencies that may exist in the account are fetched from the
		 * account object.
		 */
   		// TODO: This is not correct at all...
        allEntryDataObjects.add(new PropertyBlock(EntryInfo.getIncomeExpenseCurrencyAccessor(), "common2") {
        	public ExtendableObject getObjectContainingProperty(EntryData data) {
        		Entry entry = data.getEntry();
        		if (entry != null
        				&& entry.getAccount() instanceof IncomeExpenseAccount) {
        			IncomeExpenseAccount account = (IncomeExpenseAccount)entry.getAccount();
        			if (account.isMultiCurrency()) {
        				return entry;
        			}
        		}
        		
        		// Not a multi-currency account, so property not applicable.
        		return null;
        	}
        });

        /*
		 * Add the column which shows the amount of the foreign currency.
		 * This field is blank unless:
		 * - The transaction is a simple transaction (i.e. is displayed on
		 * a single line with no child rows).
		 * - The currency of the income and expense amount is different
		 * from the currency in this bank account.  This may be because
		 * the income and expense account is limited to entries in a single
		 * currency or it may be that the user chose a different currency
		 * for the income and expense amount.
		 * 
		 * This amount is always show without a sign (the sign can be
		 * determined from whether the other currency amount is in
		 * the credit or debit column).
		 */
        allEntryDataObjects.add(new PropertyBlock(EntryInfo.getAmountAccessor(), "other") {
        	public ExtendableObject getObjectContainingProperty(EntryData data) {
        			if (!data.hasSplitEntries()) {
        				Entry entry = data.getOtherEntry();
        				if (entry.getAccount() instanceof IncomeExpenseAccount
        				&& !JMoneyPlugin.areEqual(entry.getCommodity(), account.getCurrency())) {
        					return entry;
        				}
        			}
        		
        		// If we get here, the property is not applicable for this entry.
        		return null;
        	}
        });

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
    
    public CurrencyAccount getAccount () {
    	return account;
    }

	public void saveState(IMemento memento) {
		// Save view state (e.g. the sort order, the set of extension properties that are
		// displayed in the table).
	}
}