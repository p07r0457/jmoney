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
package net.sf.jmoney.ui.internal.pages.account.capital;

import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.ui.internal.JMoneyUIPlugin;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntrySection extends SectionPart {

    protected EntriesPage fPage;
    protected Text fDescription;
    
    protected Entry currentEntry = null;
    
    Composite transactionArea;
    
    Composite entriesArea;
    
    /**
     * Represents the set of controls for a single entry in the
     * transaction.  These controls form one (or maybe more) rows
     * of controls in a colored row across the grid.
     * 
     * @author Nigel Westbury
     */
    class EntryControls {
    	private Color entryColor;
    	
    	/** The controls that fill each grid item in the entries grid. */
    	private Composite composite1;
    	private Composite composite2;
    	private Composite composite3;
    	private Composite composite4;
    	private Composite composite5;
    	
    	private Label debitLabel;
    	private Text debitText;
    	private Label creditLabel;
    	private Text creditText;

    	/**
    	 * Set of all controls in the composite1 area.
    	 * element: Control (labels and property edit controls)
    	 */
//    	private Vector propertyControls = new Vector();
    	
        /** element: IPropertyControl */
//      Vector entryControls = new Vector();
    	
        /** element: LabelAndEditControlPair */
    	Vector entryPropertyControls = new Vector();

        class LabelAndEditControlPair {
        	private Label label;
        	private IPropertyControl propertyControl;
        	
        	/**
			 * @param propertyLabel
			 * @param propertyControl
			 */
			public LabelAndEditControlPair(Label label, IPropertyControl propertyControl) {
				this.label = label;
				this.propertyControl = propertyControl;
			}

			void dispose() {
        		label.dispose();
        		propertyControl.getControl().dispose();
        	}

			/**
			 * @param entry
			 */
			public void load(Entry entry) {
				propertyControl.load(entry);
			}
        }
        
        /**
         * The account for which the appropriate set of entry
         * properties exist.  If null then the account list 
         * property only exists.
         */
        private Account account;
        
        /** The object actually being edited.
    	 * This will be null if no entry is selected in the table.
    	 * Any controls will be disabled.
    	 * The entry can only be null if this is the first entry
    	 * because that is the only entry row that is displayed when
    	 * no entry is selected.
    	 */
    	private Entry entry;
    	
        /**
    	 * This object represents a row of controls that allow
    	 * editing of the properties for an Entry in the transaction.
    	 * Constructing this object also constructs the controls.
    	 */
    	EntryControls(Color entryColor) {
    		this.entryColor = entryColor;
    		this.entry = null;
    		
    		FormToolkit toolkit = fPage.getManagedForm().getToolkit();

            composite1 = toolkit.createComposite(entriesArea);
            composite2 = toolkit.createComposite(entriesArea);
            composite3 = toolkit.createComposite(entriesArea);
            composite4 = toolkit.createComposite(entriesArea);
            composite5 = toolkit.createComposite(entriesArea);
             
            GridLayout layout1 = new GridLayout(10, false);
            composite1.setLayout(layout1);

            RowLayout layout2 = new RowLayout();
            layout2.marginHeight = 2;
            layout2.marginWidth = 2;
            composite2.setLayout(layout2);
            composite3.setLayout(layout2);
            composite4.setLayout(layout2);
            composite5.setLayout(layout2);

            composite1.setLayoutData(new GridData(GridData.FILL_BOTH));
            composite1.setBackground(entryColor);
    		composite2.setLayoutData(new GridData(GridData.FILL_BOTH));
            composite2.setBackground(entryColor);
    		composite3.setLayoutData(new GridData(GridData.FILL_BOTH));
            composite3.setBackground(entryColor);
    		composite4.setLayoutData(new GridData(GridData.FILL_BOTH));
            composite4.setBackground(entryColor);
    		composite5.setLayoutData(new GridData(GridData.FILL_BOTH));
            composite5.setBackground(entryColor);

            // If no account is set yet in the entry then use the "Income"
            // and "Expense" labels, because it is more likely that the account
            // will be an income/expense account than a capital account.
            debitLabel = toolkit.createLabel(composite2, "Income:");
            debitText = toolkit.createText(composite3, "");
            creditLabel = toolkit.createLabel(composite4, "Expense:");
            creditText = toolkit.createText(composite5, "");

            debitLabel.setBackground(entryColor);
            creditLabel.setBackground(entryColor);
            
            debitText.addFocusListener(
    				new FocusAdapter() {
    					public void focusLost(FocusEvent e) {
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
    							commodityForFormatting = fPage.getAccount().getCurrency();
    						}
    						
    						String amountString = debitText.getText();
    						long amount = commodityForFormatting.parse(amountString);
    						if (amount != 0) {
    							entry.setAmount(-amount);
    							debitText.setText(commodityForFormatting.format(amount));
    							// When a debit is entered, clear out any credit.
    							creditText.setText("");
    						} else {
    							if (creditText.getText().equals("")) { 
    								entry.setAmount(0);
    							}
								debitText.setText("");
    						}
    					}
    				});
    		
            creditText.addFocusListener(
    				new FocusAdapter() {
    					public void focusLost(FocusEvent e) {
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
    							commodityForFormatting = fPage.getAccount().getCurrency();
    						}
    						
    						String amountString = creditText.getText();
    						long amount = commodityForFormatting.parse(amountString);
    						if (amount != 0) {
    							entry.setAmount(amount);
    							creditText.setText(commodityForFormatting.format(amount));
    							// When a debit is entered, clear out any credit.
    							debitText.setText("");
    						} else {
    							if (debitText.getText().equals("")) { 
    								entry.setAmount(0);
    							}
								creditText.setText("");
    						}
    					}
    				});
    		
    		// The account combo always exists and always comes first,
            // so we add that ourselves.
    		addLabelAndEditControl(EntryInfo.getAccountAccessor());
    		
    		// Listen for changes to the account selection.
    		// This changes the set of properties to be shown
    		// for this entry.
            Session session = fPage.getAccount().getSession();
            session.addSessionChangeListener(new SessionChangeAdapter() {
    			public void objectChanged(ExtendableObject extendableObject, PropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
    				if (propertyAccessor == EntryInfo.getAccountAccessor()
    						&& extendableObject.equals(entry)) {
    					updateSetOfEntryControls();
    				}
    			}
    		});
    	}

    	/**
    	 * Create the set of controls that are appropriate for
    	 * entries in the given account.  This method is to be
    	 * called when controls are to be shown, but no entry is
    	 * selected and the controls are to be disabled.
    	 * <P>
    	 * This method is used to create a view of the entry
    	 * edit area when no entry is selected. 
    	 * 
		 * @param account The set of disabled controls are as
		 * 			appropriate for entries in this account.
		 * 			May be null in which case just the account
		 * 			selection control is shown. 
		 */
		public void showDisabledControls(Account account) {
			Account oldAccount = this.account;
			this.account = account;
			this.entry = null;

			boolean isAccountSet = true;
			
			if ((account == null && oldAccount == null)
					|| (account != null && account.equals(oldAccount))) {
				// TODO: Clean this out.
			} else {
				((LabelAndEditControlPair)entryPropertyControls.get(0)).load(null);
				
				// Remove all the old controls after the account.
				for (int i=1; i < entryPropertyControls.size(); i++) {
					LabelAndEditControlPair controlPair = (LabelAndEditControlPair)entryPropertyControls.get(i);
					controlPair.dispose();
				}
				entryPropertyControls.setSize(1);
				
				addFurtherControls(account);
				
				creditLabel.setVisible(isAccountSet);
				creditText.setVisible(isAccountSet);
				debitLabel.setVisible(isAccountSet);
				debitText.setVisible(isAccountSet);

				debitText.setEnabled(false);
	            creditText.setEnabled(false);
			}
			
			// Clear and disable the controls.
			for (Iterator iter = entryPropertyControls.iterator(); iter.hasNext();) {
				LabelAndEditControlPair controlPair = (LabelAndEditControlPair)iter.next();
				controlPair.load(null);
			}
		}
		
    	/**
		 * @param entry The entry to show.  Cannot be null - 
		 * 			if no entry is selected then call the
		 * 			showDisabledControls method.
		 */
		public void setEntry(final Entry entry) {
			Entry oldEntry = this.entry;
			this.entry = entry;

			boolean isAccountSet = (entry.getAccount() != null);

			// If the account is the same, we simply re-load.
			if ((account == null && entry.getAccount() == null)
					|| (account != null && account.equals(entry.getAccount()))) {
				// TODO tidy up
			} else {
				// Remove all the old controls after the account.
				for (int i = 1; i < entryPropertyControls.size(); i++) {
					LabelAndEditControlPair controlPair = (LabelAndEditControlPair)entryPropertyControls.get(i);
					controlPair.dispose();
				}
				entryPropertyControls.setSize(1);
				
				addFurtherControls(entry.getAccount());
				
				creditLabel.setVisible(isAccountSet);
				creditText.setVisible(isAccountSet);
				debitLabel.setVisible(isAccountSet);
				debitText.setVisible(isAccountSet);
			}

			// Update the contents of the controls to the values from the entry
			// and ensure the controls are enabled.
			for (Iterator iter = entryPropertyControls.iterator(); iter.hasNext();) {
				LabelAndEditControlPair controlPair = (LabelAndEditControlPair)iter.next();
				controlPair.load(entry);
			}
			
			debitText.setEnabled(true);
            creditText.setEnabled(true);
			
			// Set the amount in the credit and debit controls.
			long amount = entry.getAmount();
			
			// Kludge: If a category account, use the currency
			// for set in this editor.
			
			Commodity commodity = (entry.getAccount() instanceof CapitalAccount) 
			? entry.getCommodity()
					: fPage.getAccount().getCurrency();
			
			if (commodity != null) {
				if (amount > 0) {
					creditText.setText(commodity.format(amount));
				} else {
					creditText.setText("");
				}
				if (amount < 0) {
					debitText.setText(commodity.format(-amount));
				} else {
					debitText.setText("");
				}
			}
		}

    	/**
    	 * Creates both a label and an edit control.
    	 * 
    	 * Adds both to the propertyControls array so that they can
    	 * later be disposed (they will be disposed if the account or
    	 * some other property is changed so that this property is
    	 * no longer applicable).
    	 * <P>
    	 * The caller must call the <code>IPropertyControl.load()</code>
    	 * method to either set the entry object or to disable the
    	 * control.
    	 * 
    	 * @param composite1
    	 * @param propertyAccessor
    	 * @param entry
    	 */
    	void addLabelAndEditControl(PropertyAccessor propertyAccessor) {
    		FormToolkit toolkit = fPage.getManagedForm().getToolkit();
    		Label propertyLabel = toolkit.createLabel(composite1, propertyAccessor.getShortDescription() + ':');
    		propertyLabel.setBackground(entryColor);
    		IPropertyControl propertyControl = propertyAccessor.createPropertyControl(composite1);
    		toolkit.adapt(propertyControl.getControl(), true, true);
    		propertyControl.getControl().addFocusListener(
    				new PropertyControlFocusListener(propertyAccessor, propertyControl) {
    					ExtendableObject getExtendableObject() {
    						return entry;
    					}
    				});
    		
    		LabelAndEditControlPair controlPair = new LabelAndEditControlPair(propertyLabel, propertyControl);
    		entryPropertyControls.add(controlPair);
    	}


    	/**
    	 * Adds the controls that are dependent on the selected account.
    	 * This method is called when the row of controls for an entry are
    	 * first constructed, and also called when the user changes the
    	 * account selected in the entry.
    	 * 
    	 * The set of applicable entry properties depends on the account
    	 * set in the entry.
    	 * 
    	 * @param account If null then no controls are added beyond the
    	 * 			account list control.
    	 */
    	void addFurtherControls(Account account) {
    		this.account = account;
    		
    		// The other controls depend on the type of account.
    		// This needs to be generalized in the metadata, but until
    		// that work is done, the description applies to entries in
    		// income/expense accounts and all other properties apply
    		// to capital accounts.
    		if (account != null) {
    			if (account instanceof IncomeExpenseAccount) {
    				addLabelAndEditControl(EntryInfo.getDescriptionAccessor());
    			} else {
    				for (Iterator iter = EntryInfo.getPropertySet().getPropertyIterator3(); iter.hasNext();) {
    					PropertyAccessor propertyAccessor = (PropertyAccessor) iter.next();
    					if (propertyAccessor.isEditable()
    							&& propertyAccessor.isScalar()
    							&& propertyAccessor != EntryInfo.getAccountAccessor() 
    							&& propertyAccessor != EntryInfo.getAmountAccessor()
    							&& propertyAccessor != EntryInfo.getDescriptionAccessor()) {
    						addLabelAndEditControl(propertyAccessor);
    					}
    				}
    			}
    		}
    		
    		// The choice of labels for the two amount controls also depend
    		// on the type of account that is selected.
    		String debitText = "";
    		String creditText = "";
    		if (account != null) {
    			if (account instanceof IncomeExpenseAccount) {
    				debitText = "Income:";
    				creditText = "Expense:";
    			} else {
    				debitText = "Debit:";
    				creditText = "Credit:";
    			}
    		}
    		debitLabel.setText(debitText);
    		creditLabel.setText(creditText);
    	}

    	void updateSetOfEntryControls() {
			// Remove all the old controls after the account.
			for (int i = 1; i < entryPropertyControls.size(); i++) {
				LabelAndEditControlPair controlPair = (LabelAndEditControlPair)entryPropertyControls.get(i);
				controlPair.dispose();
			}
			entryPropertyControls.setSize(1);
			
			// Add the new controls
			addFurtherControls(entry.getAccount());

			// Set the further controls so they are editing
			// this entry.
			for (int i=1; i < entryPropertyControls.size(); i++) {
				LabelAndEditControlPair controlPair = (LabelAndEditControlPair)entryPropertyControls.get(i);
				controlPair.load(entry);
			}
			
	        fPage.getManagedForm().reflow(true);
    	}
    	
		void dispose() {
        	composite1.dispose();
        	composite2.dispose();
        	composite3.dispose();
        	composite4.dispose();
        	composite5.dispose();
		}
    }
    
    /** Controls for the selected entry (the first entry listed
     *  in the transaction).
     */
    EntryControls selectedEntryControls;
    
    /** Element: EntryControls */
    Vector entryControlsList = new Vector();
    
    /** element: IPropertyControl */
    Vector transactionControls = new Vector();
    
    public EntrySection(EntriesPage page, Composite parent) {
        super(parent, page.getManagedForm().getToolkit(), Section.DESCRIPTION | Section.TITLE_BAR);
        fPage = page;
        getSection().setText("Selected Entry");
        getSection().setDescription("Edit the currently selected entry.");
        createClient(page.getManagedForm().getToolkit());
    }

    protected void createClient(FormToolkit toolkit) {
        Composite container = toolkit.createComposite(getSection());

        GridLayout sectionLayout = new GridLayout();
        sectionLayout.numColumns = 1;
        sectionLayout.marginHeight = 0;
        sectionLayout.marginWidth = 0;
        container.setLayout(sectionLayout);

        // Create the transaction property area
		transactionArea = toolkit.createComposite(container);
		transactionArea.setLayoutData(new GridData(GridData.FILL_BOTH));

		GridLayout transactionAreaLayout = new GridLayout();
		transactionAreaLayout.numColumns = 7;
		transactionArea.setLayout(transactionAreaLayout);

        // Create the entries area
		entriesArea = toolkit.createComposite(container);
		entriesArea.setLayoutData(new GridData(GridData.FILL_BOTH));

		GridLayout entriesAreaLayout = new GridLayout();
        entriesAreaLayout.numColumns = 5;
        entriesAreaLayout.horizontalSpacing = 0;  // Ensures no uncolored gaps between items in same row
        entriesAreaLayout.verticalSpacing = 0;  // Ensures no uncolored gaps between items in same column
        entriesAreaLayout.marginWidth = 0;
        entriesArea.setLayout(entriesAreaLayout);

        // Add properties from the transaction.
        for (Iterator iter = TransactionInfo.getPropertySet().getPropertyIterator3(); iter.hasNext();) {
            final PropertyAccessor propertyAccessor = (PropertyAccessor) iter.next();
            if (propertyAccessor.isScalar()) {
        		Label propertyLabel = toolkit.createLabel(transactionArea, propertyAccessor.getShortDescription() + ':');
        		IPropertyControl propertyControl = propertyAccessor.createPropertyControl(transactionArea);
        		propertyControl.load(null);
        		toolkit.adapt(propertyControl.getControl(), true, true);
        		transactionControls.add(propertyControl);
            }
        }

        // Create the row of controls for the current entry
        // (The entry from the transaction that was shown and
        // selected in the account entries list).
        // This row is initially shown with controls that are
        // empty and disabled.
		selectedEntryControls = new EntryControls(JMoneyUIPlugin.getDefault().getYellowColor());
		selectedEntryControls.showDisabledControls(fPage.getAccount());
		
    	// Create the button area
		Composite buttonArea = toolkit.createComposite(container);
		
		RowLayout layoutOfButtons = new RowLayout();
		layoutOfButtons.fill = false;
		layoutOfButtons.justify = true;
		buttonArea.setLayout(layoutOfButtons);
		
        // Create the 'add entry' and 'delete entry' buttons.
        Button addButton = toolkit.createButton(buttonArea, "Split off New Entry", SWT.PUSH);
        addButton.addSelectionListener(new SelectionAdapter() {
           public void widgetSelected(SelectionEvent event) {
           		Session session = fPage.getAccount().getSession();
           		Transaction transaction = currentEntry.getTransaction();
           		
           		Entry newEntry = transaction.createEntry();
           		
           		// If all entries so far are in the same currency then set the
           		// amount of the new entry to be the amount that takes the balance
           		// to zero.
           		// FIXME Some lines are commented out because I don't know how to
           		// determine the currency used in an income/expense category.
           		//Commodity commodity = currentEntry.getCommodity();
           		boolean mismatchedCommodities = false;
           		long totalAmount = 0;
                for (Iterator iter = transaction.getEntryIterator(); iter.hasNext(); ) {
                	Entry entry = (Entry)iter.next();
                	/* We need to sort this out.  Can income and expense accounts
                	 * contain entries of differing currencies?
                	 * The following call fails on the income/expense entries.
                	 
                	if (!currentEntry.getCommodity().equals(entry.getCommodity())) {
                		mismatchedCommodities = true;
                		break;
                	}
                	*/
                	totalAmount += entry.getAmount();
                }
                
                if (!mismatchedCommodities) {
                	newEntry.setAmount(-totalAmount);
                }
                
                createGroupForEntry(newEntry);

                entriesArea.pack(true);
                fPage.getManagedForm().reflow(true);
           }
        });

        Button deleteButton = toolkit.createButton(buttonArea, "Delete Entries with Zero or Blank Amounts", SWT.PUSH);
        deleteButton.addSelectionListener(new SelectionAdapter() {
           public void widgetSelected(SelectionEvent event) {
  /*         		
            if (fViewer.getSelection() instanceof IStructuredSelection) {
                IStructuredSelection s = (IStructuredSelection) fViewer.getSelection();
                Object selectedObject = s.getFirstElement();
                // The selected object cannot be null because the 'delete tranaction'
                // button would be disabled if no entry were selected.
               	if (selectedObject instanceof DisplayableEntry) {
               		DisplayableEntry de = (DisplayableEntry) selectedObject;
               		Transaction transaction = de.entry.getTransaction();
               		transaction.getSession().deleteTransaction(transaction);
               		transaction.getSession().registerUndoableChange("Delete Transaction");
                }
            }
*/            
           }
        });
        
        getSection().setClient(container);
        toolkit.paintBordersFor(container);
        refresh();
    }

    /**
     * Load the values from the given entry into the property controls.
     *
     * @param entry Entry whose editable properties are presented to the user
     */
    public void update(Entry newCurrentEntry) {
    	currentEntry = newCurrentEntry;

    	selectedEntryControls.setEntry(newCurrentEntry);

    	// Update transaction property controls.
        for (Iterator iter = transactionControls.iterator(); iter.hasNext();) {
            IPropertyControl control = (IPropertyControl)iter.next();
           	control.load(newCurrentEntry.getTransaction());
        }
        
        // Create the groups for the remaining entries in the transaction.
		FormToolkit toolkit = fPage.getManagedForm().getToolkit();
        Transaction transaction = currentEntry.getTransaction();
        
        // Dispose all old groups.
        for (Iterator iter = entryControlsList.iterator(); iter.hasNext(); ) {
        	EntryControls controls = (EntryControls)iter.next();
        	controls.dispose();
        }
        entryControlsList.clear();
        
        
        // Create and set the controls for the other entries in
        // the transaction.
        for (Iterator iter = transaction.getEntryIterator(); iter.hasNext(); ) {
        	Entry entry = (Entry)iter.next();
        	if (!entry.equals(currentEntry)) {
        		createGroupForEntry(entry);
        	}
        }
        
        entriesArea.pack(true);
        fPage.getManagedForm().reflow(true);
	}
	
	
	/**
	 * @param entry
	 */
	private void createGroupForEntry(final Entry entry) {
		FormToolkit toolkit = fPage.getManagedForm().getToolkit();

		final Color entryColor = (entryControlsList.size() % 2) == 0
		? JMoneyUIPlugin.getDefault().getGreenColor() 
		: JMoneyUIPlugin.getDefault().getYellowColor();

		final EntryControls entryControls = new EntryControls(entryColor);
		entryControls.setEntry(entry);
		
		entryControlsList.add(entryControls);
	}

	abstract private class PropertyControlFocusListener extends FocusAdapter {

    	private PropertyAccessor propertyAccessor;
    	private IPropertyControl propertyControl;
    	
		// When a control gets the focus, save the old value here.
		// This value is used in the change message.
		private String oldValueText;
		
		
		PropertyControlFocusListener(PropertyAccessor propertyAccessor, IPropertyControl propertyControl) {
			this.propertyAccessor = propertyAccessor;
			this.propertyControl = propertyControl;
		}
		
		public void focusLost(FocusEvent e) {
			System.out.println("Focus lost: " + propertyAccessor.getLocalName());
	
			ExtendableObject object = getExtendableObject();
			
			if (object.getSession().isSessionFiring()) {
				return;
			}
			
			propertyControl.save();
			String newValueText = propertyAccessor.formatValueForMessage(object);
			
			String description = 
					"change " + propertyAccessor.getShortDescription() + " property"
					+ " from " + oldValueText
					+ " to " + newValueText;
			
			object.getSession().registerUndoableChange(description);
		}
		public void focusGained(FocusEvent e) {
			System.out.println("Focus gained: " + propertyAccessor.getLocalName());
			// Save the old value of this property for use in our 'undo' message.
			ExtendableObject object = getExtendableObject();
			oldValueText = propertyAccessor.formatValueForMessage(object);
		}
		
		abstract ExtendableObject getExtendableObject();
	};    
}