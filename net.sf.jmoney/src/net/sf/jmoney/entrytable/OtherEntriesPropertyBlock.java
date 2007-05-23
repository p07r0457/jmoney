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

import java.util.Vector;

import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class OtherEntriesPropertyBlock extends CellBlock {
	protected ScalarPropertyAccessor<?> accessor;
	private String id;
	
	public OtherEntriesPropertyBlock(ScalarPropertyAccessor accessor) {
		super(
				accessor.getDisplayName(),
				accessor.getWeight(),
				accessor.getMinimumWidth()
		);

		this.accessor = accessor;
		this.id = "other." + accessor.getName();
	}

	public String getId() {
		return id;
	}

	public ICellControl createCellControl(Composite parent, Session session) {
		// Because this may be multi-valued, setup the container only.
		final Composite composite = new Composite(parent, SWT.NONE);
		
		composite.setBackgroundMode(SWT.INHERIT_FORCE);
		
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 1;
		layout.verticalSpacing = 1;
		composite.setLayout(layout);
		
		return new ICellControl() {

			private Vector<IPropertyControl> propertyControls = new Vector<IPropertyControl>();
			private FocusListener controlFocusListener;
			
			public Control getControl() {
				return composite;
			}

			public void load(EntryData data) {
				for (Control child: composite.getChildren()) {
					child.dispose();
				}
				propertyControls.clear();
				
				for (Entry entry: data.getSplitEntries()) {
					IPropertyControl propertyControl = accessor.createPropertyControl(composite, data.getEntry().getSession()); 
					propertyControl.load(entry);
					propertyControls.add(propertyControl);

					propertyControl.getControl().setLayoutData(new GridData(GridData.FILL,GridData.FILL, true, true));					

					addFocusListenerRecursively(propertyControl.getControl(), controlFocusListener);
				}
			}

			public void save() {
				for (IPropertyControl propertyControl: propertyControls) {
					propertyControl.save();
				}
			}

			public void setFocusListener(FocusListener controlFocusListener) {
				this.controlFocusListener = controlFocusListener;
			}

			private void addFocusListenerRecursively(Control control, FocusListener listener) {
				control.addFocusListener(listener);
				if (control instanceof Composite) {
					for (Control child: ((Composite)control).getChildren()) {
						addFocusListenerRecursively(child, listener);
					}
				}
			}
		};
	}

	public int compare(EntryData trans1, EntryData trans2) {
		if (!trans1.isSimpleEntry()) {
			if (!trans2.isSimpleEntry()) {
				return 0;
			} else {
				return 1;
			}
		} else {
			if (!trans2.isSimpleEntry()) {
				return -1;
			} else {
				ExtendableObject extendableObject1 = trans1.getOtherEntry();
				ExtendableObject extendableObject2 = trans2.getOtherEntry();
				return accessor.getComparator().compare(extendableObject1, extendableObject2);
			}
		}
	}
}