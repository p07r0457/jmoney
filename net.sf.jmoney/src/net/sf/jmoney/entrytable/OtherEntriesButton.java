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

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Entry;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * This class represents a button that appears in each row and, when pressed,
 * drops down a shell showing details of the other entries.
 * 
 * This class is similar to OtherEntriesBlock but there are no columns in the
 * main table for the properties in the other entries, the shell has its own
 * columns with their own headers inside the shell.
 * 
 * @author Nigel Westbury
 */
public class OtherEntriesButton extends CellBlock<EntryData, EntryRowControl> {

	private final static int DROPDOWN_BUTTON_WIDTH = 15;
	
	static private Image downArrowImage = null;

	private Block<Entry, SplitEntryRowControl> otherEntriesRootBlock;
	
	public OtherEntriesButton(Block<Entry, SplitEntryRowControl> otherEntriesRootBlock) {
		super(DROPDOWN_BUTTON_WIDTH, 0);
		this.otherEntriesRootBlock = otherEntriesRootBlock;
	}

    @Override	
	public ICellControl<EntryData> createCellControl(final EntryRowControl parent) {
		
	    /*
	     * Use a single row tracker and cell focus tracker for this
	     * table.  This needs to be generalized for, say, the reconciliation
	     * editor if there is to be a single row selection for both tables.
	     */
		// TODO: This is not right - should not be created here.
	    RowSelectionTracker<SplitEntryRowControl> rowTracker = new RowSelectionTracker<SplitEntryRowControl>();
	    FocusCellTracker cellTracker = new FocusCellTracker();

		if (downArrowImage == null) {
			ImageDescriptor descriptor = JMoneyPlugin.createImageDescriptor("icons/comboArrow.gif");
			downArrowImage = descriptor.createImage();
		}

		return new ButtonCellControl(parent, downArrowImage, "Show the other entries in this transaction.") {
			@Override
			protected void run(EntryRowControl rowControl) {
				final OtherEntriesShell shell = new OtherEntriesShell(parent.getShell(), SWT.ON_TOP, rowControl.getUncommittedEntryData(), otherEntriesRootBlock, false);
    	        Display display = parent.getDisplay();
    	        Rectangle rect = display.map(parent, null, this.getControl().getBounds());
    	        shell.open(rect);
			}
		};
	}

	@Override
	public void createHeaderControls(Composite parent) {
		/*
		 * All CellBlock implementations must create a control because
		 * the header and rows must match. Maybe these objects could
		 * just point to the header controls, in which case this would
		 * not be necessary.
		 * 
		 * Note also we use Label, not an empty Composite, because we
		 * don't want a preferred height that is higher than the labels.
		 */
		new Label(parent, SWT.NONE);
	}
}
