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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class VerticalBlock<T, R extends RowControl<T>> extends Block<T,R> {
	private ArrayList<Block<T,R>> children;

	public VerticalBlock(Block<T,R> child1, Block<T,R> child2) {
		ArrayList<Block<T,R>> children = new ArrayList<Block<T,R>>();
		children.add(child1);
		children.add(child2);
		init(children);
	}
	
	public VerticalBlock(Block<T,R> child1, Block<T,R> child2, Block<T,R> child3) {
		ArrayList<Block<T,R>> children = new ArrayList<Block<T,R>>();
		children.add(child1);
		children.add(child2);
		children.add(child3);
		init(children);
	}
	
	private void init(ArrayList<Block<T,R>> children) {
		this.children = children;

		/*
		 * The minimumWidth, weight, and height are set in the
		 * constructor These values can all be calculated from the list
		 * of child blocks.
		 */
		minimumWidth = 0;
		weight = 0;
		for (Block<T,R> child: children) {
			minimumWidth = Math.max(minimumWidth, child.minimumWidth);
			weight = Math.max(weight, child.weight);
		}
	}

	@Override
	public void buildCellList(ArrayList<CellBlock<T,R>> cellList) {
		for (Block<T,R> child: children) {
			child.buildCellList(cellList);
		}
	}

	@Override
	public void createHeaderControls(Composite parent) {
		for (Block<T,R> child: children) {
			child.createHeaderControls(parent);
		}
	}

	@Override
	int getHeightForGivenWidth(int width, int verticalSpacing, Control[] controls, boolean changed) {
		int height = 0; 
		for (Block<T,R> child: children) {
			height += child.getHeightForGivenWidth(width, verticalSpacing, controls, changed);
		}
		height += (children.size() - 1) * verticalSpacing;
		return height;
	}

	@Override
	void layout(int width) {
		if (this.width != width) {
			this.width = width;
			for (Block<T,R> child: children) {
				child.layout(width);
			}
		}
	}

	@Override
	void positionControls(int left, int top, int verticalSpacing, Control[] controls, boolean flushCache) {
		int y = top;
		for (Block<T,R> child: children) {
			child.positionControls(left, y, verticalSpacing, controls, flushCache);
			y += child.getHeight(verticalSpacing, controls) + verticalSpacing;
		}
	}

	@Override
	int getHeight(int verticalSpacing, Control[] controls) {
		int height = 0; 
		for (Block<T,R> child: children) {
			height += child.getHeight(verticalSpacing, controls);
		}
		height += (children.size() - 1) * verticalSpacing;
		return height;
	}

	@Override
	void paintRowLines(GC gc, int left, int top, int verticalSpacing, Control[] controls) {
		/* Paint the horizontal lines between the controls.
		 * 
		 * We need to make nested calls in case there are nested blocks that
		 * need separator lines within them.
		 */
		int y = top;
		for (int i = 0; i < children.size(); i++) {
			children.get(i).paintRowLines(gc, left, y, verticalSpacing, controls);
			
			// Draw a horizontal separator line only if this is not the last control.
			if (i != children.size() - 1) {
				y += children.get(i).getHeight(verticalSpacing, controls);
				gc.fillRectangle(left, y, width, verticalSpacing);
				y += verticalSpacing;
			}
		}
	}
}
