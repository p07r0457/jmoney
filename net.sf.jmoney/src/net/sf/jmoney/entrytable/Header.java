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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class Header<T> extends Composite {

	private Block<? super T,?> rootBlock;
	
	public Header(Composite parent, int style, Block<? super T,?> rootBlock) {
		super(parent, style);
		this.rootBlock = rootBlock;
		
		BlockLayout layout = new BlockLayout<T>(rootBlock, false);
		setLayout(layout);

		setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		
		// TODO: This is not needed anymore.  It was called to set indexes but
		// those are not done by this method.
//		rootBlock.buildCellList();
		
		rootBlock.createHeaderControls(this, null);
	}

	/**
	 * The header rows may depend on the row selection.
	 * This method adjusts the headers to match a row with the given input.
	 * 
	 * @param input
	 */
	public void setInput(T input) {
		rootBlock.setInput(input);
	}
	
	protected boolean sortOnColumn(IndividualBlock<EntryData, EntryRowControl> sortProperty, int sortDirection) {
		// TODO: Get this working.  This method is not even called
		// currently.
		
//		entriesTable.sort(sortProperty, sortDirection == SWT.UP);
//
//		// TODO: Is there a better way of getting the table?
//		entriesTable.table.refreshContent();
        
        return true;
	}

	// TODO: This class is duplicated in EntriesTable.
	// Need to get sorting working.
	private class RowComparator implements Comparator<EntryData> {
		private Comparator<EntryData> cellComparator;
		private boolean ascending;
		
		RowComparator(IndividualBlock<EntryData, EntryRowControl> sortProperty, boolean ascending) {
			this.cellComparator = sortProperty.getComparator();
			this.ascending = ascending;
		}
		
		public int compare(EntryData entryData1, EntryData entryData2) {
			int result = cellComparator.compare(entryData1, entryData2);
			return ascending ? result : -result;
		}
	}
}
