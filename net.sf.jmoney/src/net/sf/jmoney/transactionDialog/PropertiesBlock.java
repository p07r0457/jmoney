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

package net.sf.jmoney.transactionDialog;

import java.util.ArrayList;
import java.util.List;

import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.CellFocusListener;
import net.sf.jmoney.entrytable.ICellControl;
import net.sf.jmoney.entrytable.ICellControl2;
import net.sf.jmoney.entrytable.RowControl;
import net.sf.jmoney.entrytable.SplitEntryRowControl;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.SessionChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * This class represents a block where alternative layouts are shown,
 * depending on the type of the transaction.
 * 
 * This class contains an abstract method, getTopBlock.  This method is
 * passed the row input.  An implementation must be provided that determines
 * which of the child blocks is to be shown.
 * 
 * Note that because a different block may be shown in each row, it is not possible
 * for the header to show all the column headers for all rows.  The header will
 * show the correct column headers only for the selected row, or will be blank if
 * no row is selected.
 * 
 * getTopBlock may return null for a particular row input.  In that case the block
 * will be blank for that row.
 */
class PropertiesBlock extends CellBlock<Entry, SplitEntryRowControl> {
	
	public PropertiesBlock() {
		super(400, 20);
	}
	
	// TODO: remove entry parameter from this method.
	@Override
	public void createHeaderControls(Composite parent, Entry entry) {
		Label label = new Label(parent, SWT.NULL);
		label.setText("Properties");
		label.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
	}

	@Override
	public ICellControl<Entry> createCellControl(Composite parent,
			RowControl rowControl, SplitEntryRowControl coordinator) {
    	
    	return new PropertiesCellControl(parent, rowControl);
	}

	private class PropertiesCellControl implements ICellControl<Entry> {
		private Composite propertiesControl;
		private Entry entry = null;
		
		private SessionChangeListener amountChangeListener = new SessionChangeAdapter() {
			@Override
			public void objectChanged(ExtendableObject changedObject, ScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
				if (changedObject.equals(entry) && changedProperty == EntryInfo.getAmountAccessor()) {
//					setControlContent();
				}
			}
		};
		
		final private Color labelColor;
		final private Color controlColor;
		private List<IPropertyControl> properties;
		private RowControl rowControl;

		public PropertiesCellControl(Composite parent, RowControl rowControl) {
	    	this.rowControl = rowControl;
	    	
			labelColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
			controlColor = Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);
			
			propertiesControl = new Composite(parent, SWT.NONE);
			propertiesControl.setLayout(new RowLayout(SWT.HORIZONTAL));
		}

		public Control getControl() {
			return propertiesControl;
		}

		public void load(final Entry entry) {
			this.entry = entry;

			createPropertyControls();

			/*
			 * Note that this dialog is modal so changes cannot be made from outside the dialog.
			 * Refreshing of the view after inserting and removing splits is handled by the commands
			 * directly, so we are only interested in property changes. 
			 */
			entry.getDataManager().addChangeListener(new SessionChangeAdapter() {
				@Override
				public void objectChanged(ExtendableObject changedObject, ScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
					if (changedObject == entry) {
						if (changedProperty == EntryInfo.getAccountAccessor()) {
							createPropertyControls();
							propertiesControl.layout(true);
							Composite c = propertiesControl;
							do {
								c.layout(true);
								c = c.getParent();
							} while (c != null);
//			    	        shell.pack();
						}
					}
				}
			}, propertiesControl);

			
		}

		private void createPropertyControls() {
			for (Control control : propertiesControl.getChildren()) {
				control.dispose();
			}

			properties = new ArrayList<IPropertyControl>();

			if (entry.getAccount() != null) {
				for (ScalarPropertyAccessor accessor : EntryInfo.getPropertySet().getScalarProperties3()) {
					if (accessor != EntryInfo.getAccountAccessor()
							&& accessor != EntryInfo.getMemoAccessor()
							&& accessor != EntryInfo.getAmountAccessor()
							&& isEntryPropertyApplicable(accessor, entry.getAccount())) {
						createPropertyControl(propertiesControl, accessor);
					}
				}
			}
		}

		private boolean isEntryPropertyApplicable(
				ScalarPropertyAccessor accessor, Account account) {
			// TODO Push this into the metadata.
			if (account instanceof CapitalAccount) {
				return true;
			} else {
				return false;
			}
		}

		private void createPropertyControl(Composite parent, ScalarPropertyAccessor accessor) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(2, false));
			Label label = new Label(composite, SWT.LEFT);
			label.setText(accessor.getDisplayName() + ":");
			label.setForeground(labelColor);
			final IPropertyControl propertyControl = accessor.createPropertyControl(composite);
			propertyControl.getControl().setLayoutData(new GridData(accessor.getMinimumWidth(), SWT.DEFAULT));
			propertyControl.getControl().setBackground(controlColor);

			// TODO: This will not add listener to child controls - fix this.

			// TODO: This is a really big kludge.
			// We need this interface just for two methods,
			// select and unselect.  Should those be Should we move
			//
			FocusListener controlFocusListener = new CellFocusListener<RowControl>(rowControl, new ICellControl2<Entry>() {
				public Control getControl() {
					return propertyControl.getControl();
				}

				public void load(Entry data) {
					propertyControl.load(data);
				}

				public void save() {
					propertyControl.save();
//					fireUserChange(coordinator);
				}

				public void setFocusListener(FocusListener controlFocusListener) {
					// Nothing to do???
				}

				@Override
				public void setSelected() {
					propertyControl.getControl().setBackground(RowControl.selectedCellColor);
				}

				@Override
				public void setUnselected() {
					propertyControl.getControl().setBackground(null);
				}
			});

			// This is a little bit of a kludge.  Might be a little safer to implement a method
			// in IPropertyControl to add the focus listener?
//			addFocusListenerRecursively(propertyControl.getControl(), controlFocusListener);
			propertyControl.getControl().addFocusListener(controlFocusListener);

			propertyControl.load(entry);
			properties.add(propertyControl);
		}

		public void save() {
			for (IPropertyControl control : properties) {
				control.save();
			}
		}

		public void setFocusListener(FocusListener controlFocusListener) {
			// TODO: Should this method be removed???
		}

		public void setSelected() {
			propertiesControl.setBackground(RowControl.selectedCellColor);
		}

		public void setUnselected() {
			propertiesControl.setBackground(null);
		}
	}

}
