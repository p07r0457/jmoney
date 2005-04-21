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

package net.sf.jmoney.pages.entries;

import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.Transaction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * This class manages the transaction dialog that appears when
 * the 'details' button is pressed in the account entries list page.
 * <P>
 * The transaction dialog displays a single transaction.  The controls
 * for each entry are on alternating yellow and green areas, providing
 * a clear distinction between the properties for one entry and the
 * properties for another entry even when more than one line of controls
 * is needed for a single entry.  This dialog displays all the appropriate
 * properties for each entry.  Thus, if the entries list table does not
 * show a column for a property, the user can use this dialog to fill in
 * the complete set of properties.
 * 
 * @author Nigel Westbury
 */
public class TransactionDialog {

    protected static final Color yellow = new Color(Display.getCurrent(), 255, 255, 150);
    protected static final Color green  = new Color(Display.getCurrent(), 200, 255, 200);

    private Shell shell;
    private Display display;

    public void open() {
        shell.pack();
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
    }
    
    private Session session;

    private Currency defaultCurrency;
    
    private Composite transactionArea;
    
    private Composite entriesArea;
    
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
        }
        
        /**
         * The account for which the appropriate set of entry
         * properties exist.  If null then the account list 
         * property only exists.
         */
        private Account account;
        
        /** 
         * The entry object being displayed in the set of controls.
    	 */
    	private Entry entry;
    	
        /**
    	 * This object represents a row of controls that allow
    	 * editing of the properties for an Entry in the transaction.
    	 * Constructing this object also constructs the controls.
    	 */
    	EntryControls(final Entry entry, Color entryColor) {
    		this.entryColor = entryColor;
    		this.entry = entry;
    		
            composite1 = new Composite(entriesArea, 0);
            composite2 = new Composite(entriesArea, 0);
            composite3 = new Composite(entriesArea, 0);
            composite4 = new Composite(entriesArea, 0);
            composite5 = new Composite(entriesArea, 0);
             
            GridLayout layout1 = new GridLayout(8, false);
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

            // Create the debit and credit controls.  Note that the text for the
            // labels are not set until later because the text depends on whether
            // the account for the entry is a capital account or an income and
            // expense account.
            debitLabel = new Label(composite2, 0);
            debitText = new Text(composite3, 0);
            creditLabel = new Label(composite4, 0);
            creditText = new Text(composite5, 0);

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
    							commodityForFormatting = defaultCurrency;
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
    							commodityForFormatting = defaultCurrency;
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
            session.addSessionChangeListener(new SessionChangeAdapter() {
    			public void objectChanged(ExtendableObject extendableObject, PropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
    				if (propertyAccessor == EntryInfo.getAccountAccessor()
    						&& extendableObject.equals(entry)) {
    					updateSetOfEntryControls();
    				}
    			}
    		}, entriesArea);

			addFurtherControls(entry.getAccount());
				
			// Set the amount in the credit and debit controls.
			long amount = entry.getAmount();
			
			// Kludge: If a category account, use the currency
			// for set in this editor.
			
			Commodity commodity = (entry.getAccount() instanceof CapitalAccount) 
			? entry.getCommodity()
					: defaultCurrency;
			
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
    	 * @param propertyAccessor
    	 */
    	void addLabelAndEditControl(PropertyAccessor propertyAccessor) {
    		Label propertyLabel = new Label(composite1, 0);
    		propertyLabel.setText(propertyAccessor.getShortDescription() + ':');
    		propertyLabel.setBackground(entryColor);
    		IPropertyControl propertyControl = propertyAccessor.createPropertyControl(composite1);
    		propertyControl.getControl().addFocusListener(
    				new PropertyControlFocusListener(propertyAccessor, propertyControl) {
    					ExtendableObject getExtendableObject() {
    						return entry;
    					}
    				});
    		
    		LabelAndEditControlPair controlPair = new LabelAndEditControlPair(propertyLabel, propertyControl);
			propertyControl.load(entry);
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

    		// We must pack these or the labels will not show, though I
    		// do not entirely understand why.
    		debitLabel.pack();
    		creditLabel.pack();
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

			shell.pack();
    	}

		/**
		 * Set the backgroud color of the controls for this entry. 
		 * Although the color is passed to the constructor, the color may need to
		 * be flipped when an entry is deleted.
		 * 
		 * @param color
		 */
		public void setColor(Color entryColor) {
			this.entryColor = entryColor;
			
            composite1.setBackground(entryColor);
            composite2.setBackground(entryColor);
            composite3.setBackground(entryColor);
            composite4.setBackground(entryColor);
            composite5.setBackground(entryColor);
            debitLabel.setBackground(entryColor);
            creditLabel.setBackground(entryColor);
			for (int i = 0; i < entryPropertyControls.size(); i++) {
				LabelAndEditControlPair controlPair = (LabelAndEditControlPair)entryPropertyControls.get(i);
				controlPair.label.setBackground(entryColor);
			}
		}
    	
		void dispose() {
        	composite1.dispose();
        	composite2.dispose();
        	composite3.dispose();
        	composite4.dispose();
        	composite5.dispose();
		}
    }
    
    /** Element: EntryControls */
    Vector entryControlsList = new Vector();
    
    /** element: IPropertyControl */
    Vector transactionControls = new Vector();
    
    public TransactionDialog(Shell parent, Entry accountEntry, Session session, Currency defaultCurrency) {
    	this.session = session;
    	this.defaultCurrency = defaultCurrency;
    	
        this.display = parent.getDisplay();
        
   		final Transaction transaction = accountEntry.getTransaction();
   		
        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.CLOSE);
        shell.setText("Transaction Details");
    	
        GridLayout sectionLayout = new GridLayout();
        sectionLayout.numColumns = 1;
        sectionLayout.marginHeight = 0;
        sectionLayout.marginWidth = 0;
        shell.setLayout(sectionLayout);

        // Create the transaction property area
		transactionArea = new Composite(shell, 0);
		transactionArea.setLayoutData(new GridData(GridData.FILL_BOTH));

		GridLayout transactionAreaLayout = new GridLayout();
		transactionAreaLayout.numColumns = 10;
		transactionArea.setLayout(transactionAreaLayout);

        // Create the entries area
		entriesArea = new Composite(shell, 0);
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
        		Label propertyLabel = new Label(transactionArea, 0);
        		propertyLabel.setText(propertyAccessor.getShortDescription() + ':');
        		IPropertyControl propertyControl = propertyAccessor.createPropertyControl(transactionArea);
        		propertyControl.load(null);
        		transactionControls.add(propertyControl);
            }
        }

    	// Create the button area
		Composite buttonArea = new Composite(shell, 0);
		
		RowLayout layoutOfButtons = new RowLayout();
		layoutOfButtons.fill = false;
		layoutOfButtons.justify = true;
		buttonArea.setLayout(layoutOfButtons);
		
        // Create the 'add entry' button.
        Button addButton = new Button(buttonArea, SWT.PUSH);
        addButton.setText("Split off New Entry");
        addButton.addSelectionListener(new SelectionAdapter() {
           public void widgetSelected(SelectionEvent event) {
           		Entry newEntry = transaction.createEntry();
           		
           		// If all entries so far are in the same currency then set the
           		// amount of the new entry to be the amount that takes the balance
           		// to zero.  If we cannot determine the currency because the user
           		// has not yet entered the necessary data, assume that the currencies
           		// are all the same.
           		Commodity commodity = null;
           		boolean mismatchedCommodities = false;
           		long totalAmount = 0;
                for (Iterator iter = transaction.getEntryCollection().iterator(); iter.hasNext(); ) {
                	Entry entry = (Entry)iter.next();
                	if (commodity == null) {
                		// No commodity yet determined, so set to the commodity for
                		// this entry, if any.
                		commodity = entry.getCommodity(); 
                	} else {
                		if (!commodity.equals(entry.getCommodity())) {
                			mismatchedCommodities = true;
                			break;
                		}
                	}
                	totalAmount += entry.getAmount();
                }
                
                if (!mismatchedCommodities) {
                	newEntry.setAmount(-totalAmount);
                }
                
        		Color entryColor = (entryControlsList.size() % 2) == 0
        		? yellow
        		: green;

        		EntryControls newEntryControls = new EntryControls(newEntry, entryColor);
        		entryControlsList.add(newEntryControls);

                shell.pack(true);
           }
        });

        // Create the 'delete entry' button.
        Button deleteButton = new Button(buttonArea, SWT.PUSH);
        deleteButton.setText("Delete Entries with Zero or Blank Amounts");
        deleteButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
        		boolean alternate = false;
        		for (Iterator iter = entryControlsList.iterator(); iter.hasNext(); ) {
        			EntryControls entryControls = (EntryControls)iter.next();
        			if (entryControls.entry.getAmount() == 0) {
        				entryControls.dispose();
        				transaction.deleteEntry(entryControls.entry);
        				iter.remove();
        			} else {
        				entryControls.setColor(alternate ? green : yellow);
        				alternate = !alternate;
        			}
        		}
        		shell.pack();
        	}
        });

        // Create the 'close' button
        Button closeButton = new Button(buttonArea, SWT.PUSH);
        closeButton.setText("Close");
        closeButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
        		shell.close();
        	}
        });

    	// Update transaction property controls.
        for (Iterator iter = transactionControls.iterator(); iter.hasNext();) {
            IPropertyControl control = (IPropertyControl)iter.next();
           	control.load(transaction);
        }
        
		// Load the values from the given entry into the property controls.
		entryControlsList.add(new EntryControls(accountEntry, yellow));
		
        // Create and set the controls for the other entries in
        // the transaction.
        for (Iterator iter = transaction.getEntryCollection().iterator(); iter.hasNext(); ) {
        	Entry entry = (Entry)iter.next();
        	if (!entry.equals(accountEntry)) {

        		Color entryColor = (entryControlsList.size() % 2) == 0
        		? yellow 
        		: green;

        		entryControlsList.add(new EntryControls(entry, entryColor));
        	}
        }
        
        shell.pack();
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
			// Save the old value of this property for use in our 'undo' message.
			ExtendableObject object = getExtendableObject();
			oldValueText = propertyAccessor.formatValueForMessage(object);
		}
		
		abstract ExtendableObject getExtendableObject();
	};    
}
