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
import java.util.Comparator;

import net.sf.jmoney.model2.Session;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Represents a column of data that can be displayed in the entries table,
 * edited by the user, sorted, or used in a filter.
 * <P>
 * All columns are managed by an object of this class.  Special
 * implementations exist for the credit, debit, and balance columns.
 * More generic implementations exist for the other properties.
 */
public abstract class CellBlock extends Block {
	/**
	 * The index of this cell in the list returned by buildCellList.
	 * This is not set until buildCellList is called.
	 */
	private int index;
	
	/**
	 * The localized text to be shown in the header.
	 */
	private String text;
	
	public abstract ICellControl createCellControl(Composite parent, Session session);
	
	public CellBlock(String text, int minimumWidth, int weight) {
		this.text = text;
		this.minimumWidth = minimumWidth;
		this.weight = weight;
	}

	public String getText() {
		return text;
	}
	
	@Override
	public void buildCellList(ArrayList<CellBlock> cellList) {
		this.index = cellList.size();
		cellList.add(this);
	}

	@Override
	void layout(int width) {
		this.width = width;
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

	/**
	 * @return a comparator to be used for sorting rows based on the values in
	 *         this column, or null if this column is not suitable for sorting
	 */
	public Comparator<EntryData> getComparator() {
		return null;
	}
}
