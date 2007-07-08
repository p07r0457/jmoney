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

public class HorizontalBlock<T> extends Block<T> {
	private Block<T> [] children;
	
	public HorizontalBlock(Block<T>... children) {
		this.children = children;

		/*
		 * The minimumWidth, weight, and height are set in the
		 * constructor These values can all be calculated from the list
		 * of child blocks.
		 */
		minimumWidth = 0;
		weight = 0;
		for (Block<T> child: children) {
			minimumWidth += child.minimumWidth;
			weight += child.weight;
		}
		
		// Add spacing for each gap between child blocks (the number of gaps
		// will be one less than the number of child blocks).
		minimumWidth += (children.length - 1) * Block.horizontalSpacing;
	}

	@Override
	public void buildCellList(ArrayList<CellBlock<T>> cellList) {
		for (Block<T> child: children) {
			child.buildCellList(cellList);
		}
	}

	@Override
	public void createHeaderControls(Composite parent) {
		for (Block<T> child: children) {
			child.createHeaderControls(parent);
		}
	}

	@Override
	void layout(int width) {
		if (this.width != width) {
			this.width = width;

			int[] widths = distributeWidth(width);

			for (int i = 0; i < children.length; i++) {
				children[i].layout(widths[i]);
			}
		}
	}

	private int[] distributeWidth(int width) {
		int [] widths = new int[children.length];
		for (int i = 0; i < children.length; i++) {
			widths[i] = children[i].minimumWidth;
		}

		// Do we have extra width and columns with expansion weight?
		if (weight > 0 && width > minimumWidth) {
			// Now distribute the rest to the columns with weight.
			int rest = width - minimumWidth;
			int totalDistributed = 0;
			for (int i = 0; i < children.length; i++) {
				int pixels = children[i].weight * rest / weight;
				totalDistributed += pixels;
				widths[i] += pixels;
			}

			/*
			 * We may still have a few pixels left to allocate
			 * because the above divisions round the pixel count
			 * downwards. Distribute any remaining pixels to columns
			 * with weight. The number of pixels left can never be
			 * more than the number of columns with non-zero
			 * weights.
			 */
			int diff = rest - totalDistributed;
			for (int i = 0; i < children.length && diff > 0; i++) {
				if (children[i].weight > 0) {
					++widths[i];
					--diff;
				}
			}
		}
		return widths;
	}

	@Override
	void positionControls(int left, int top, int verticalSpacing, Control[] controls, boolean flushCache) {
		int x = left;
		for (Block<T> child: children) {
			child.positionControls(x, top, verticalSpacing, controls, flushCache);
			x += child.width + Block.horizontalSpacing;
		}
	}

	@Override
	int getHeight(int verticalSpacing, Control[] controls) {
		int height = 0; 
		for (Block<T> child: children) {
			height = Math.max(height, child.getHeight(verticalSpacing, controls));
		}
		return height;
	}

	@Override
	void paintRowLines(GC gc, int left, int top, int verticalSpacing, Control[] controls) {
		/* Paint the vertical lines between the controls.
		 * 
		 * We need to make nested calls in case there are nested blocks that
		 * need separator lines within them.
		 */
		int x = left;
		for (int i = 0; i < children.length; i++) {
			children[i].paintRowLines(gc, x, top, verticalSpacing, controls);
			
			// Draw a vertical separator line only if this is not the last control.
			if (i != children.length - 1) {
				x += children[i].width;
				gc.fillRectangle(x, top, Block.horizontalSpacing, getHeight(verticalSpacing, controls));
				x += Block.horizontalSpacing;
			}
		}
	}

	@Override
	int getHeightForGivenWidth(int width, int verticalSpacing, Control[] controls, boolean changed) {
		int[] widths = distributeWidth(width);
		int height = 0; 
		for (int i = 0; i < children.length; i++) {
			height = Math.max(height, children[i].getHeightForGivenWidth(widths[i], verticalSpacing, controls, changed));
		}
		return height;
	}
}
