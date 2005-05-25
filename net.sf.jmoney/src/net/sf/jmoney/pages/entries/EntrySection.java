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
 */

package net.sf.jmoney.pages.entries;

import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
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
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntrySection extends SectionPart {
	
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
		
		abstract class LabelAndEditControlPair {
			private IPropertyControl propertyControl;
			private PropertyAccessor propertyAccessor;
			private Composite pairComposite;
			
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
			 * @param propertyLabel
			 * @param propertyControl
			 */
			public LabelAndEditControlPair(PropertyAccessor propertyAccessor) {
				this.propertyAccessor = propertyAccessor;
				
				pairComposite = toolkit.createComposite(composite1);
				pairComposite.setBackground(entryColor);
				RowLayout layout = new RowLayout();
				layout.marginTop = 0;
				layout.marginBottom = 0;
				layout.marginLeft = 0;
				layout.marginRight = 0;
				pairComposite.setLayout(layout);
				
				Label label = toolkit.createLabel(pairComposite, propertyAccessor.getShortDescription() + ':');
				label.setBackground(entryColor);
				propertyControl = propertyAccessor.createPropertyControl(pairComposite);
				toolkit.adapt(propertyControl.getControl(), true, true);
				propertyControl.getControl().addFocusListener(
						new PropertyControlFocusListener(propertyAccessor, propertyControl) {
							ExtendableObject getExtendableObject() {
								return entry;
							}
						});
				
			}
			
			void dispose() {
				pairComposite.dispose();
			}
			
			/**
			 * @param entry
			 */
			public void load(Entry entry) {
				propertyControl.load(entry);
			}
			
			/**
			 * @param entry
			 * @param isEntryChanging true if this method is being called because
			 * 			a different Entry is being shown in the control, false if
			 * 			the entry has not changed but a property in the entry has
			 * 			changed and that property may affect whether this property
			 * 			is applicable
			 */
			public void refreshState(Entry entry, boolean isEntryChanging) {
				Account account = entry.getAccount();
				boolean isApplicable = isApplicable(account);
				
				// Controls with the visability set to false still
				// take up space in the grid.  We must dispose controls
				// if they do not apply.
				if (isApplicable) {
					boolean mustLoadEntry = isEntryChanging;
					
					if (pairComposite == null) {
						pairComposite = toolkit.createComposite(composite1);
						pairComposite.setBackground(entryColor);
						RowLayout layout = new RowLayout();
						layout.marginTop = 0;
						layout.marginBottom = 0;
						layout.marginLeft = 0;
						layout.marginRight = 0;
						pairComposite.setLayout(layout);
						
						Label label = toolkit.createLabel(pairComposite, propertyAccessor.getShortDescription() + ':');
						label.setBackground(entryColor);
						propertyControl = propertyAccessor.createPropertyControl(pairComposite);
						toolkit.adapt(propertyControl.getControl(), true, true);
						propertyControl.getControl().addFocusListener(
								new PropertyControlFocusListener(propertyAccessor, propertyControl) {
									ExtendableObject getExtendableObject() {
										return EntryControls.this.entry;
									}
								});
						
						mustLoadEntry = true;
					}
					
					if (mustLoadEntry) {
						load(entry);
					}
					
				} else {
					if (pairComposite != null) {
						pairComposite.dispose();
						pairComposite = null;
					}
				}
			}
			
			abstract boolean isApplicable(Account account);
		}
		
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
			
			composite1 = toolkit.createComposite(entriesArea);
			composite2 = toolkit.createComposite(entriesArea);
			composite3 = toolkit.createComposite(entriesArea);
			composite4 = toolkit.createComposite(entriesArea);
			composite5 = toolkit.createComposite(entriesArea);
			
			RowLayout layout1 = new RowLayout(SWT.HORIZONTAL);
			layout1.spacing = 5;
			composite1.setLayout(layout1);
			
			RowLayout layout2 = new RowLayout();
			layout2.marginHeight = 2;
			layout2.marginWidth = 2;
			composite2.setLayout(layout2);
			composite3.setLayout(layout2);
			composite4.setLayout(layout2);
			composite5.setLayout(layout2);
			
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.widthHint = 100;
			composite1.setLayoutData(gd);
			composite1.setBackground(entryColor);
			composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			composite2.setBackground(entryColor);
			composite3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			composite3.setBackground(entryColor);
			composite4.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			composite4.setBackground(entryColor);
			composite5.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
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
							// use the currency that was given to us as the default currency
							// for this page.
							Commodity commodityForFormatting = entry.getCommodity();
							if (commodityForFormatting == null) {
								commodityForFormatting = defaultCurrencyForPage;
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
							// use the currency that was given to us as the default currency
							// for this page.
							Commodity commodityForFormatting = entry.getCommodity();
							if (commodityForFormatting == null) {
								commodityForFormatting = defaultCurrencyForPage;
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
			
			// The account combo is always applicable, and must come
			// first, so add this first specifically.
			entryPropertyControls.add(
					new LabelAndEditControlPair(EntryInfo.getAccountAccessor()) {
						boolean isApplicable(Account account) {
							return true;
						}
					});
			
			// The other controls depend on the type of account.
			// This needs to be generalized in the metadata, but until
			// that work is done, the description applies to entries in
			// income/expense accounts and all other properties apply
			// to capital accounts.
			entryPropertyControls.add(
					new LabelAndEditControlPair(EntryInfo.getDescriptionAccessor()) {
						boolean isApplicable(Account account) {
							return account instanceof IncomeExpenseAccount;
						}
					});
			
			
			for (Iterator iter = EntryInfo.getPropertySet().getPropertyIterator3(); iter.hasNext();) {
				PropertyAccessor propertyAccessor = (PropertyAccessor) iter.next();
				if (propertyAccessor.isEditable()
						&& propertyAccessor.isScalar()
						&& propertyAccessor != EntryInfo.getAccountAccessor() 
						&& propertyAccessor != EntryInfo.getAmountAccessor()
						&& propertyAccessor != EntryInfo.getDescriptionAccessor()) {
					entryPropertyControls.add(
							new LabelAndEditControlPair(propertyAccessor) {
								boolean isApplicable(Account account) {
									return account instanceof CapitalAccount;
								}
							});
				}
			}
			
			// Listen for changes to the account selection.
			// This changes the set of properties to be shown
			// for this entry.
			session.addSessionChangeListener(new SessionChangeAdapter() {
				public void objectChanged(ExtendableObject changedObject, PropertyAccessor changedProperty, Object oldValue, Object newValue) {
					if (changedProperty == EntryInfo.getAccountAccessor()
							&& changedObject.equals(entry)) {
						updateSetOfEntryControls();
					}
					
					// If any other properties are changed, re-load the control.
					if (changedObject.equals(entry)) {
						for (int i = 0; i < entryPropertyControls.size(); i++) {
							LabelAndEditControlPair controlPair = (LabelAndEditControlPair)entryPropertyControls.get(i);
							if (controlPair.propertyAccessor == changedProperty) {
								if (controlPair.pairComposite != null) {
									controlPair.load(entry);
								}
							}
						}
					}
				}
			});
		}
		
		/**
		 * @param entry The entry to show.  Cannot be null - 
		 * 			if no entry is selected then call the
		 * 			showDisabledControls method.
		 */
		public void setEntry(final Entry entry) {
			this.entry = entry;
			
			for (int i = 0; i < entryPropertyControls.size(); i++) {
				LabelAndEditControlPair controlPair = (LabelAndEditControlPair)entryPropertyControls.get(i);
				controlPair.refreshState(entry, true);
			}
			
			// Set the amount in the credit and debit controls.
			long amount = entry.getAmount();
			
			// We need a currency so that we can format the amount.
			// Get the currency from this entry if possible.
			// However, the user may not have yet entered enough information
			// to determine the currency for this entry, in which case
			// use the currency that was given to us as the default currency
			// for this page.
			Commodity commodityForFormatting = entry.getCommodity();
			if (commodityForFormatting == null) {
				commodityForFormatting = defaultCurrencyForPage;
			}
			
			if (amount > 0) {
				creditText.setText(commodityForFormatting.format(amount));
			} else {
				creditText.setText("");
			}
			if (amount < 0) {
				debitText.setText(commodityForFormatting.format(-amount));
			} else {
				debitText.setText("");
			}
			
			layoutSection();
		}
		
		/**
		 * Some entry properties may be inapplicable, depending on the
		 * value of other properties.  This method is called when the value
		 * of a property changes and the change may make other properties
		 * applicable or inapplicable.
		 * 
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
		void updateSetOfEntryControls() {
			for (int i = 1; i < entryPropertyControls.size(); i++) {
				LabelAndEditControlPair controlPair = (LabelAndEditControlPair)entryPropertyControls.get(i);
				controlPair.refreshState(entry, false);
			}
			
			Account account = entry.getAccount();
			
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
			
			// Pack so that the new controls become visible
			entriesArea.pack();
		}
		
		void dispose() {
			composite1.dispose();
			composite2.dispose();
			composite3.dispose();
			composite4.dispose();
			composite5.dispose();
		}
		
		void setVisible(boolean visible) {
			composite1.setVisible(visible);
			composite2.setVisible(visible);
			composite3.setVisible(visible);
			composite4.setVisible(visible);
			composite5.setVisible(visible);
		}
	}
	
	private static final Color yellow = new Color(Display.getCurrent(), 255, 255, 200);
	private static final Color green  = new Color(Display.getCurrent(), 225, 255, 225);
	
	private FormToolkit toolkit;
	
	/**
	 * Generally the currency for the amount in an entry is known.
	 * However, if the user is entering a new entry and has not
	 * yet entered enough information to determine the currency
	 * then this default currency is used for formatting the amount.
	 */
	private Commodity defaultCurrencyForPage;
	
	private Entry currentEntry = null;
	
	private Composite transactionArea;
	
	private Composite entriesArea;
	
	/** Controls for the selected entry (the first entry listed
	 *  in the transaction).
	 */
	private EntryControls selectedEntryControls;
	
	/** Controls for the other entry if the row represents a
	 * simple transaction.  (Two entries in a transaction, both
	 * merged into a single row in the table).
	 */
	private EntryControls otherEntryControls;
	
	/** element: IPropertyControl */
	private Vector transactionControls = new Vector();
	
	private Composite container;
	private Composite filler = null;
	private Session session;
	
	public EntrySection(Composite parent, FormToolkit toolkit, Session session, Currency defaultCurrencyForPage) {
		super(parent, toolkit, 
				Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE 
				| (JMoneyPlugin.getDefault().getPreferenceStore().getBoolean("expandEditEntrySection")
						? Section.EXPANDED : 0));
		this.toolkit = toolkit;
		this.session = session;
		this.defaultCurrencyForPage = defaultCurrencyForPage;
		
		getSection().addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				JMoneyPlugin.getDefault().getPreferenceStore().setValue("expandEditEntrySection", e.getState());
			}
		});
		getSection().setText("Selected Entry");
		
		container = toolkit.createComposite(getSection());
		
		GridLayout sectionLayout = new GridLayout();
		sectionLayout.numColumns = 1;
		sectionLayout.marginHeight = 0;
		sectionLayout.marginWidth = 0;
		container.setLayout(sectionLayout);
		
		// Create the transaction property area
		// The area is initially set to a zero size so it is not visible.
		// It will be set to the appropriate size when an entry is selected.
		transactionArea = toolkit.createComposite(container);
		transactionArea.setLayoutData(new GridData(0, 0));
		
		RowLayout layout1 = new RowLayout(SWT.HORIZONTAL);
		transactionArea.setLayout(layout1);
		
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
		
		// Create the entries area
		entriesArea = toolkit.createComposite(container);
		entriesArea.setLayoutData(new GridData(0, 0));
		
		GridLayout entriesAreaLayout = new GridLayout();
		entriesAreaLayout.numColumns = 5;
		entriesAreaLayout.horizontalSpacing = 0;  // Ensures no uncolored gaps between items in same row
		entriesAreaLayout.verticalSpacing = 0;  // Ensures no uncolored gaps between items in same column
		entriesAreaLayout.marginWidth = 0;
		entriesArea.setLayout(entriesAreaLayout);
		
		// This must be on the container (if on the entriesArea, recursion occurs)
		container.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				layoutSection();
			}
		});
		
		// Create the row of controls for the current entry
		// (The entry from the transaction that was shown and
		// selected in the account entries list).
		selectedEntryControls = new EntryControls(yellow);
		otherEntryControls = new EntryControls(green);
		
		// The filler is used to set the entry section to a fixed size.
		// When we want to display some data in the section, the filler is destroyed
		// and the appropriate controls are set to the required non-zero sizes.
		// By keeping the sections a constant size, we avoid reflowing the sections
		// which does not look good.  
		
		// A size of 130 is sufficient on the Windows platform for one line of
		// transaction properties and two entries each with two rows of properties.
		// TODO: Find a better way of setting the fixed size for the section.
		filler = toolkit.createComposite(container);
		filler.setLayoutData(new GridData(SWT.DEFAULT, 130));
		getSection().setClient(container);
		toolkit.paintBordersFor(container);
		refresh();
	}
	
	/**
	 * Load the values from the given entry into the property controls.
	 *
	 * @param entry Entry whose editable properties are presented to the user
	 */
	public void update(Entry accountEntry, Entry selectedEntry) {
		currentEntry = accountEntry;
		
		if (filler != null) {
			filler.dispose();
			filler = null;
		}
		
		GridData entriesAreaLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
		entriesAreaLayoutData.widthHint = 200;
		entriesArea.setLayoutData(entriesAreaLayoutData);
		
		selectedEntryControls.setEntry(selectedEntry);
		
		// Update transaction property controls.
		if (selectedEntry.equals(accountEntry)) {
			getSection().setDescription("Edit the currently selected entry.");
			
			transactionArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			for (Iterator iter = transactionControls.iterator(); iter.hasNext();) {
				IPropertyControl control = (IPropertyControl)iter.next();
				control.load(accountEntry.getTransaction());
			}
		} else {
			getSection().setDescription("Edit the currently selected split.");
			transactionArea.setLayoutData(new GridData(0, 0));
		}
		
		// Create the groups for the remaining entries in the transaction.
		Transaction transaction = currentEntry.getTransaction();
		
		if (selectedEntry.equals(accountEntry)
				&& transaction.hasTwoEntries()) {
			Entry otherEntry = transaction.getOther(currentEntry);
			otherEntryControls.setEntry(otherEntry);
			otherEntryControls.setVisible(true);
		} else {
			otherEntryControls.setVisible(false);
		}
		
		layoutSection();
	}
	
	
	/**
	 * Layout the entry section.  This is not done correctly by the grid and
	 * row layouts.  The problem is as follows:  The entriesArea grid gets
	 * the preferred sizes from each of the child controls.  The composite1
	 * controls (each of row layout) return the fixed preferred width and then calculates the height
	 * required to contain the child controls.  The grid layout that allocates
	 * any excess width to the column containing the composite1 controls.  The
	 * controls in the composite1 composites are then re-flowed by the row layout.
	 * However, the height is not reduced, resulting in the controls being too high
	 * and a lot of empty space at the bottom of each row.
	 * <P>
	 * The solution is to first layout the grid with a small size set for the
	 * preferred width of the composite1 controls.  Then the actual width allocated
	 * by the grid layout is set as the preferred width, then the grid is layed out
	 * again.  This results in the correct heights.
	 */
	private void layoutSection() {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd1.widthHint = 100;
		selectedEntryControls.composite1.setLayoutData(gd1);
		
		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd2.widthHint = 100;
		otherEntryControls.composite1.setLayoutData(gd2);
		
		container.layout();
		
		otherEntryControls.composite1.setLayoutData(new GridData(otherEntryControls.composite1.getSize().x, SWT.DEFAULT));
		selectedEntryControls.composite1.setLayoutData(new GridData(selectedEntryControls.composite1.getSize().x, SWT.DEFAULT));
		
		entriesArea.pack(true);
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