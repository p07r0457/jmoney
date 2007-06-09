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
abstract public class PropertyBlock extends IndividualBlock<EntryData> {
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

	public String getId() {
		return id;
	}

	public abstract ExtendableObject getObjectContainingProperty(EntryData data);

	public ICellControl<EntryData> createCellControl(Composite parent) {
		final IPropertyControl propertyControl = accessor.createPropertyControl(parent);
		
		return new ICellControl<EntryData>() {

			public Control getControl() {
				return propertyControl.getControl();
			}

			public void load(EntryData data) {
				propertyControl.load(getObjectContainingProperty(data));
			}

			public void save() {
				propertyControl.save();
			}

			public void setFocusListener(FocusListener controlFocusListener) {
				// Nothing to do
			}
		};
	}

	public Comparator<EntryData> getComparator() {
		final Comparator<ExtendableObject> subComparator = accessor.getComparator();
		if (subComparator == null) {
			return null;
		} else {
			return new Comparator<EntryData>() {
				public int compare(EntryData entryData1, EntryData entryData2) {
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

	public static PropertyBlock createTransactionColumn(
			ScalarPropertyAccessor<?> propertyAccessor) {
		return new PropertyBlock(propertyAccessor, "transaction") {
			@Override
			public ExtendableObject getObjectContainingProperty(EntryData data) {
				return data.getEntry().getTransaction();
			}
		};
	}

	public static PropertyBlock createEntryColumn(
			ScalarPropertyAccessor<?> propertyAccessor) {
		return new PropertyBlock(propertyAccessor, "entry") {
			@Override
			public ExtendableObject getObjectContainingProperty(EntryData data) {
				return data.getEntry();
			}
		};
	}
}