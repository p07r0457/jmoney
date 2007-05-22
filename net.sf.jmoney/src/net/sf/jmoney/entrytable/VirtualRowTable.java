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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Slider;

public class VirtualRowTable extends Composite {

	ContentPane contentPane;
	
	/**
	 * This composite creates a two by two grid.  The header in the
	 * top left, the table of rows in the bottom left, the vertical
	 * scroll bar in the bottom right, and a blank cell in the top
	 * right.  If no vertical scroll bar is required then its size
	 * is set to zero so the header and rows take up the full
	 * width.  This layout ensures that the header is always the same
	 * width as the rows, which is good if the columns are going to
	 * line up with the header. 
	 * 
	 * @param parent
	 * @param rootBlock
	 */
	// TODO: tidy up EntriesTable parameter.  Perhaps we need to remove EntriesTable altogether?
	public VirtualRowTable(Composite parent, Block rootBlock, EntriesTable entriesTable, IRowProvider rowProvider) {
		super(parent, SWT.NONE);
		
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		setLayout(layout);
	
		Header header = new Header(this, SWT.NONE, entriesTable);
		Composite blankPane = new Composite(this, SWT.NONE);
		contentPane = new ContentPane(this, SWT.NONE, rowProvider);
		Slider vSlider = new Slider(this, SWT.VERTICAL);
		
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		blankPane.setLayoutData(new GridData(0, 0));
		contentPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		vSlider.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		contentPane.setVerticalSlider(vSlider);
	}

	/**
	 * Refreshes the content of the rows.  The set of rows is
	 * assumed unchanged.
	 */
	public void refreshContentOfAllRows() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Similar to <code>refreshContentOfAllRows</code>
	 * but updates the balances only.
	 */
	public void refreshBalancesOfAllRows() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Deletes the given row.
	 * 
	 * The row must be deleted from the underlying content.  This
	 * method is not responsible for doing that.  This method does
	 * update the display, decrement the row count.
`	 * 
	 * If the row being deleted is the selected row then any uncommitted
	 * changes are discarded without warning (it is assumed that the
	 * caller gave sufficient warning).
	 * 
	 * @param data
	 */
	public void deleteRow(int index) {
		// TODO Auto-generated method stub
		
		refreshBalancesOfAllRows();
	}

	/**
	 * Inserts the given row.
	 * 
	 * The row must have been inserted into the underlying content.  This
	 * method is not responsible for doing that.  This method does
	 * update the display, increment the row count.
	 * 
	 * This method does not affect the current selection.  It is possible that 
	 * a row is inserted by another view/editor while a row is being edited.
	 * In such a case, the editing of the row is not affected.
	 * 
	 * @param data
	 */
	public void insertRow(int index) {
		// TODO Auto-generated method stub
		
		refreshBalancesOfAllRows();
	}

	/**
	 * Sets the focus to the given column and row.
	 *  
	 * @param x
	 * @param y
	 */
	public void setSelection(int x, int y) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 
	 * @return the selected row, or -1 if no row is selected
	 */
	public int getSelection() {
		// TODO Auto-generated method stub
		return -1;
		
	}

	/**
	 * This method is called when the content changes.
	 * Specific changes to the content such as a row insert
	 * or delete can be more efficiently refreshed using ...
	 * This method is called when content is sorted or filtered.
	 * 
	 */
	// TODO: Are there more efficent methods following JFace conventions
	// to do this?
	public void refreshContent() {
		contentPane.refreshContent();
	}
}
