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

import java.util.ArrayList;

import net.sf.jmoney.model2.Session;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * This class represents a block that is used to display the fields from the
 * other entries in the transaction.
 * 
 * If this is a simple transaction (one other entry and that entry is an
 * income/expense account) then the properties are displayed in place.
 * 
 * If this is a split transaction then the words '--split--' is displayed with
 * a drop-down button.
 * 
 * If this is a transfer then this appears the same as a simple transaction
 * except that the account is in square brackets.
 * 
 * @author Nigel Westbury
 */
public class OtherEntriesBlock extends Block {

	private Block otherEntryBlock;
	
	private IEntriesTableProperty cellProperty;

	/**
	 * The index of this cell in the list returned by buildCellList.
	 * This is not set until buildCellList is called.
	 */
	private int index;
	
	public OtherEntriesBlock(Block otherEntryBlock) {
		this.otherEntryBlock = otherEntryBlock;
		this.minimumWidth = otherEntryBlock.minimumWidth;
		this.weight = otherEntryBlock.weight;
		
		cellProperty = new IEntriesTableProperty() {

			public int compare(EntryData trans1, EntryData trans2) {
				// TODO Auto-generated method stub
				return 0;
			}

			public ICellControl createCellControl(Composite parent,
					Session session) {
				// TODO Auto-generated method stub
				return null;
			}

			public String getId() {
				// TODO Auto-generated method stub
				return null;
			}

			public int getMinimumWidth() {
				// TODO Auto-generated method stub
				return 0;
			}

			public String getText() {
				// TODO Auto-generated method stub
				return null;
			}

			public int getWeight() {
				// TODO Auto-generated method stub
				return 0;
			}
		}; 
	}

	@Override
	public void buildCellList(ArrayList<IEntriesTableProperty> cellList) {
		this.index = cellList.size();
		cellList.add(cellProperty);
	}

	@Override
	void layout(int width) {
		this.width = width;
		otherEntryBlock.layout(width);
	}

	@Override
	void positionControls(int x, int y, int verticalSpacing, Control[] controls, boolean changed) {
		Control control = controls[index];
		int height = control.computeSize(width, SWT.DEFAULT, changed).y;
		control.setBounds(x, y, width, height);
	};

	@Override
	int getHeight(int verticalSpacing, Control[] controls) {
		Control control = controls[index];
		return control.getSize().y;
	}

	@Override
	void paintRowLines(GC gc, int x, int y, int verticalSpacing, Control[] controls) {
		// Nothing to do.
	}
}
