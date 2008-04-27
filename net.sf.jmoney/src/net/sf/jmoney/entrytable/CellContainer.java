/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2008 Nigel Westbury <westbury@users.sf.net>
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class CellContainer<T,R extends RowControl> extends Composite {

	/**
	 * the current input, being always a non-null value if this row
	 * is active and undefined if this row is inactive 
	 */
	protected T input;
	
	public RowSelectionTracker<R> selectionTracker;
	public FocusCellTracker focusCellTracker;
	
	// Although currently the keys of this map are never used
	// (and it may as well be a list of the values only), a map
	// allows us to do stuff like move the focus to the control
	// in error during transaction validation.
	protected Map<CellBlock, ICellControl<? super T>> controls = new HashMap<CellBlock, ICellControl<? super T>>();

	public CellContainer(Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * This method creates the controls.
	 * 
	 * This method must always be called by the constructor of the final derived
	 * classes of this class.  Why do we not just call it from the constructor
	 * of this class?  The reason is because the controls that are created by this method have a back reference to
	 * this object.  These back references are typed (using generics) to the
	 * final derived type.  These controls will expect field initializers and
	 * possibly constructor initialization to have been done on the final derived type.
	 * However, at the time the base constructor is called, neither will have
	 * been initialized.
	 */
	protected void init(R rowControl, Block<? super T, ? super R> rootBlock, RowSelectionTracker<R> selectionTracker, FocusCellTracker focusCellTracker) {
		this.selectionTracker = selectionTracker;
		this.focusCellTracker = focusCellTracker;
		
		for (CellBlock<? super T, ? super R> cellBlock: rootBlock.buildCellList()) {
			// Create the control with no content set.
			final ICellControl<? super T> cellControl = cellBlock.createCellControl(this, rowControl);
			controls.put(cellBlock, cellControl);

			if (input != null) {
				cellControl.load(input);
			}
			
			FocusListener controlFocusListener = new CellFocusListener<R>(rowControl, cellControl, selectionTracker, focusCellTracker);
			
			Control control = cellControl.getControl();
//				control.addKeyListener(keyListener);
			addFocusListenerRecursively(control, controlFocusListener);
//				control.addTraverseListener(traverseListener);
			
			// This is needed in case more child controls are created at a
			// later time.  This is not the cleanest code, but the UI for  these
			// split entries may be changed at a later time anyway.
			cellControl.setFocusListener(controlFocusListener);
		}
	}

	/**
	 * Add listeners to each control.
	 * 
	 * @param control The control to listen to.
	 */
	protected void addFocusListenerRecursively(Control control, FocusListener listener) {
		control.addFocusListener(listener);
		
		if (control instanceof Composite) {
			Composite composite = (Composite) control;
			for (int i = 0; i < composite.getChildren().length; i++) {
				Control childControl = composite.getChildren()[i];
				addFocusListenerRecursively(childControl, listener);
			}
		}
	}
	
	public void setInput(T input) {
		this.input = input;

		for (final ICellControl<? super T> control: controls.values()) {
			control.load(input);
		}
	}

	public T getInput() {
		return input;
	}
}
