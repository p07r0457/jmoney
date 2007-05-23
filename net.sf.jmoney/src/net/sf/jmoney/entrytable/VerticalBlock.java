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
import org.eclipse.swt.widgets.Control;

public class VerticalBlock extends Block {
	private Block [] children;
	
	public VerticalBlock(Block [] children) {
		this.children = children;

		/*
		 * The minimumWidth, weight, and height are set in the
		 * constructor These values can all be calculated from the list
		 * of child blocks.
		 */
		minimumWidth = 0;
		weight = 0;
		for (Block child: children) {
			minimumWidth = Math.max(minimumWidth, child.minimumWidth);
			weight += Math.max(minimumWidth, child.weight);
		}
	}

	@Override
	public void buildCellList(ArrayList<CellBlock> cellList) {
		for (Block child: children) {
			child.buildCellList(cellList);
		}
	}

	@Override
	void layout(int width) {
		if (this.width != width) {
			this.width = width;
			for (Block child: children) {
				child.layout(width);
			}
		}
	}

	@Override
	void positionControls(int x, int y, int verticalSpacing, Control[] controls, boolean flushCache) {
		for (Block child: children) {
			child.positionControls(x, y, verticalSpacing, controls, flushCache);
			y += child.getHeight(verticalSpacing, controls) + verticalSpacing;
		}
	};

	@Override
	int getHeight(int verticalSpacing, Control[] controls) {
		int height = 0; 
		for (Block child: children) {
			height += child.getHeight(verticalSpacing, controls);
		}
		height += (children.length - 1) * verticalSpacing;
		return height;
	}

	@Override
	void paintRowLines(GC gc, int x, int y, int verticalSpacing, Control[] controls) {
		/* Paint the horizontal lines between the controls.
		 * 
		 * We need to make nested calls in case there are nested blocks that
		 * need separator lines within them.
		 */
		for (int i = 0; i < children.length; i++) {
			children[i].paintRowLines(gc, x, y, verticalSpacing, controls);
			
			// Draw a horizontal separator line only if this is not the last control.
			if (i != children.length - 1) {
				y += children[i].getHeight(verticalSpacing, controls);
				gc.fillRectangle(x, y, width, verticalSpacing);
				y += verticalSpacing;
			}
		}
	}
}
