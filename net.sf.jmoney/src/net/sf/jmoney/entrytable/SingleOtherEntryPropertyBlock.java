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
public class SingleOtherEntryPropertyBlock extends IndividualBlock<Entry, SplitEntryRowControl> {
	private ScalarPropertyAccessor<?> accessor;
	
	public SingleOtherEntryPropertyBlock(ScalarPropertyAccessor accessor) {
		super(
				accessor.getDisplayName(),
				accessor.getMinimumWidth(),
				accessor.getWeight()
		);

		this.accessor = accessor;
	}

	public SingleOtherEntryPropertyBlock(ScalarPropertyAccessor accessor, String displayName) {
		super(
				displayName,
				accessor.getMinimumWidth(),
				accessor.getWeight()
		);

		this.accessor = accessor;
	}

	public String getId() {
		return accessor.getName();
	}

    @Override	
	public ICellControl<Entry> createCellControl(Composite parent, SplitEntryRowControl rowControl) {
		final IPropertyControl propertyControl = accessor.createPropertyControl(parent);
		
		return new ICellControl<Entry>() {

			public Control getControl() {
				return propertyControl.getControl();
			}

			public void load(Entry entry) {
				propertyControl.load(entry);
			}

			public void save() {
				propertyControl.save();
			}

			public void setFocusListener(FocusListener controlFocusListener) {
				// Nothing to do
			}
		};
	}
}