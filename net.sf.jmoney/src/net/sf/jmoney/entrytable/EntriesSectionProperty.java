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

import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;

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
abstract public class EntriesSectionProperty implements IEntriesTableProperty {
	private ScalarPropertyAccessor<?> accessor;
	private String id;
	
	public EntriesSectionProperty(ScalarPropertyAccessor accessor, String source) {
		this.accessor = accessor;
		this.id = source + '.' + accessor.getName();
	}

	public String getText() {
		return accessor.getDisplayName();
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

	public abstract ExtendableObject getObjectContainingProperty(EntryData data);

	public ICellControl createCellControl(Composite parent, Session session) {
		final IPropertyControl propertyControl = accessor.createPropertyControl(parent, session);
		
		return new ICellControl() {

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

	public int compare(EntryData trans1, EntryData trans2) {
		ExtendableObject extendableObject1 = getObjectContainingProperty(trans1);
		ExtendableObject extendableObject2 = getObjectContainingProperty(trans2);
		if (extendableObject1 == null && extendableObject2 == null) return 0;
		if (extendableObject1 == null) return 1;
		if (extendableObject2 == null) return -1;
		return accessor.getComparator().compare(extendableObject1, extendableObject2);
	}

	public static IEntriesTableProperty createTransactionColumn(
			ScalarPropertyAccessor<?> propertyAccessor) {
		return new EntriesSectionProperty(propertyAccessor, "transaction") {
			@Override
			public ExtendableObject getObjectContainingProperty(EntryData data) {
				return data.getEntry().getTransaction();
			}
		};
	}

	public static IEntriesTableProperty createEntryColumn(
			ScalarPropertyAccessor<?> propertyAccessor) {
		return new EntriesSectionProperty(propertyAccessor, "entry") {
			@Override
			public ExtendableObject getObjectContainingProperty(EntryData data) {
				return data.getEntry();
			}
		};
	}

	public static IEntriesTableProperty createOtherEntryColumn(
			ScalarPropertyAccessor<?> propertyAccessor) {
		return new EntriesSectionCategoryProperty(propertyAccessor, "entry") {
			@Override
			public IPropertyControl createPropertyControl(Composite parent,
					Entry otherEntry) {
   				IPropertyControl control = accessor.createPropertyControl(parent, otherEntry.getSession());
   				control.load(otherEntry);
   				return control;
			}
		};
	}
}