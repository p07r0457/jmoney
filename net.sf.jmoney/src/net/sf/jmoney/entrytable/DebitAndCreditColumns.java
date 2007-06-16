/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2007 Nigel Westbury <westbury@users.sf.net>
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

package net.sf.jmoney.entrytable;

import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.SessionChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * Represents a table column that is either the debit or the credit column.
 * Use two instances of this class instead of a single instance of the
 * above <code>EntriesSectionProperty</code> class if you want the amount to be
 * displayed in separate debit and credit columns.
 */
public class DebitAndCreditColumns extends IndividualBlock<EntryData> {

	private class DebitAndCreditCellControl implements ICellControl<EntryData> {
		private Text textControl;
		private Entry entry = null;
		
		private SessionChangeListener amountChangeListener = new SessionChangeAdapter() {
			public void objectChanged(ExtendableObject changedObject, ScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
				if (changedObject.equals(entry) && changedProperty == EntryInfo.getAmountAccessor()) {
					setControlContent();
				}
			}
		};

		public DebitAndCreditCellControl(Text textControl) {
			this.textControl = textControl;

			textControl.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
			    	if (entry != null) {
			    		entry.getObjectKey().getSessionManager().removeChangeListener(amountChangeListener);
			    	}
				}
			});
		}

		public Control getControl() {
			return textControl;
		}

		public void load(EntryData data) {
	    	if (entry != null) {
	    		entry.getObjectKey().getSessionManager().removeChangeListener(amountChangeListener);
	    	}
	    	
			entry = data.getEntry();

        	/*
        	 * We must listen to the model for changes in the value
        	 * of this property.
        	 */
			entry.getObjectKey().getSessionManager().addChangeListener(amountChangeListener);
			
			setControlContent();
		}

		private void setControlContent() {
			long amount = entry.getAmount();

			/*
			 * We need a currency so that we can format the amount. Get the
			 * currency from this entry if possible. However, the user may
			 * not have yet entered enough information to determine the
			 * currency for this entry, in which case use the default
			 * currency for this entry table.
			 */
			Commodity commodityForFormatting = entry.getCommodity();
			if (commodityForFormatting == null) {
				commodityForFormatting = commodity;
			}
			
			if (isDebit) {
				// Debit column
				textControl.setText(amount < 0 
						? commodityForFormatting.format(-amount) 
								: ""
				);
			} else {
				// Credit column
				textControl.setText(amount > 0 
						? commodityForFormatting.format(amount) 
								: ""
				);
			}
		}

		public void save() {
			/*
			 * We need a currency so that we can parse the amount. Get the
			 * currency from this entry if possible. However, the user may
			 * not have yet entered enough information to determine the
			 * currency for this entry, in which case use the default
			 * currency for this entry table.
			 */
			Commodity commodityForFormatting = entry.getCommodity();
			if (commodityForFormatting == null) {
				commodityForFormatting = commodity;
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

		public void setFocusListener(FocusListener controlFocusListener) {
			// Nothing to do
		}
	}

	private String id;
	private Commodity commodity;
	private boolean isDebit;

	public DebitAndCreditColumns(String id, String name, Commodity commodity, boolean isDebit) {
		super(name, 70, 2);
		this.id = id;
		this.commodity = commodity;
		this.isDebit = isDebit;
	}

	public String getId() {
		return id;
	}

	public ICellControl<EntryData> createCellControl(Composite parent) {
		final Text textControl = new Text(parent, SWT.TRAIL);
		textControl.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				switch (e.detail) {
				case SWT.TRAVERSE_ARROW_PREVIOUS:
					if (e.keyCode == SWT.ARROW_UP) {
						e.doit = false;
						e.detail = SWT.TRAVERSE_NONE;
					}
					break;
				case SWT.TRAVERSE_ARROW_NEXT:
					if (e.keyCode == SWT.ARROW_DOWN) {
						e.doit = true;
					}
					break;
				}
			}
		});
		
    	return new DebitAndCreditCellControl(textControl);
	}

	public int compare(EntryData entryData1, EntryData entryData2) {
		long amount1 = entryData1.getEntry().getAmount();
		long amount2 = entryData2.getEntry().getAmount();

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