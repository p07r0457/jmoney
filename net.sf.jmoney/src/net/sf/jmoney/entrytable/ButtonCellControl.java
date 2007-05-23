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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

abstract public class ButtonCellControl implements ICellControl {

	private Button button;
	private EntryData data;

	protected abstract void run(EntryData data); 

	public ButtonCellControl (Composite parent, Image image, String toolTipText) {
		/*
		 * Create a button, but override the preferred size to be 10 (by default
		 * it is 64).  This prevents the button from making the lines too high.
		 * Unfortunately will need to wrap the button in a composite in order to
		 * override the preferred size.  If anyone knows a better way, please change
		 * this code.
		 */
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		button = new Button(composite, SWT.PUSH);
		button.setLayoutData(new GridData(10, 10));
		button.setImage(image);
		button.setToolTipText(toolTipText);
			
		button.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				run(data);
			}
			public void widgetSelected(SelectionEvent e) {
				run(data);
			}
		});
	}

	public Control getControl() {
		return button;
	}

	public void load(EntryData data) {
		// We only need to save the data so we know the object
		// on which the action is to act.
		this.data = data;
	}

	public void save() {
		// Nothing to do
	}

	public void setFocusListener(FocusListener controlFocusListener) {
		// Nothing to do
	}
};
