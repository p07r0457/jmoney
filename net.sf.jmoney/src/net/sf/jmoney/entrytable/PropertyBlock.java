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

import java.util.Comparator;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Represents a property that can be displayed in the entries table,
 * edited by the user, or used in the filter.
 * <P>
 * Only properties where a single value exists in the cell are supported
 * by this class.
 * <P>
 * The credit, debit, and balance columns are hard coded at the end
 * of the table and are not represented by objects of this class.
 * 
 * @author Nigel Westbury
 */
abstract public class PropertyBlock<T extends EntryData, R extends Composite> extends IndividualBlock<T, R> {
	private ScalarPropertyAccessor<?> accessor;
	private String id;
	
	public PropertyBlock(ScalarPropertyAccessor accessor, String source) {
		super(
				accessor.getDisplayName(),
				accessor.getMinimumWidth(),
				accessor.getWeight()
		);

		this.accessor = accessor;
		this.id = source + '.' + accessor.getName();
	}

	public PropertyBlock(ScalarPropertyAccessor accessor, String source, String displayName) {
		super(
				displayName,
				accessor.getMinimumWidth(),
				accessor.getWeight()
		);

		this.accessor = accessor;
		this.id = source + '.' + accessor.getName();
	}

	public String getId() {
		return id;
	}

	/**
	 * Given the input data, returns the ExtendableObject that contains
	 * the property whose value is being shown in this column.
	 * 
	 * @param data
	 * @return the object containing the property being show, or null if
	 * 		this column is not applicable to the given input
	 */
	public abstract ExtendableObject getObjectContainingProperty(T data);

	/**
	 * This method is called whenever the user makes a change to this value.
	 * 
	 * This method is used by the RowControl objects to update other contents
	 * that may be affected by this control. The normal session change listener
	 * is not used because the RowControl wants to be notified only when the
	 * user makes changes from within this row control, not when a change has
	 * been made elsewhere.
	 * 
	 * As most controls do not affect other controls, an empty default implementation
	 * is provided.
	 */
	public void fireUserChange(R rowControl) {
		// Default implementation does nothing.
	}
	
    @Override	
	public ICellControl<T> createCellControl(Composite parent, final R rowControl) {
		final IPropertyControl propertyControl = accessor.createPropertyControl(parent);
		
		return new ICellControl<T>() {

			public Control getControl() {
				return propertyControl.getControl();
			}

			public void load(T data) {
				ExtendableObject entryContainingProperty = getObjectContainingProperty(data);
				propertyControl.load(entryContainingProperty);
			}

			public void save() {
				propertyControl.save();
				fireUserChange(rowControl);
			}

			public void setFocusListener(FocusListener controlFocusListener) {
				// Nothing to do
			}
		};
	}

    @Override	
	public Comparator<T> getComparator() {
		final Comparator<ExtendableObject> subComparator = accessor.getComparator();
		if (subComparator == null) {
			return null;
		} else {
			return new Comparator<T>() {
				public int compare(T entryData1, T entryData2) {
					ExtendableObject extendableObject1 = getObjectContainingProperty(entryData1);
					ExtendableObject extendableObject2 = getObjectContainingProperty(entryData2);
					if (extendableObject1 == null && extendableObject2 == null) return 0;
					if (extendableObject1 == null) return 1;
					if (extendableObject2 == null) return -1;
					return subComparator.compare(extendableObject1, extendableObject2);
				}
			};
		}
	}

	public static PropertyBlock<EntryData, Composite> createTransactionColumn(
			ScalarPropertyAccessor<?> propertyAccessor) {
		return new PropertyBlock<EntryData, Composite>(propertyAccessor, "transaction") {
			@Override
			public ExtendableObject getObjectContainingProperty(EntryData data) {
				return data.getEntry().getTransaction();
			}
		};
	}

	public static PropertyBlock<EntryData, Composite> createEntryColumn(
			ScalarPropertyAccessor<?> propertyAccessor) {
		return new PropertyBlock<EntryData, Composite>(propertyAccessor, "entry") {
			@Override
			public ExtendableObject getObjectContainingProperty(EntryData data) {
				return data.getEntry();
			}
		};
	}

	/**
	 * This version allows the caller to override the text used in the header.
	 * @param propertyAccessor
	 * @param displayName the text to use in the header
	 * @return
	 */
	public static PropertyBlock<EntryData, Composite> createEntryColumn(ScalarPropertyAccessor<?> propertyAccessor, String displayName) {
		return new PropertyBlock<EntryData, Composite>(propertyAccessor, "entry", displayName) {
			@Override
			public ExtendableObject getObjectContainingProperty(EntryData data) {
				return data.getEntry();
			}
		};
	}
}