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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;


public class BlockLayout extends Layout {

	private Block block;

	/**
	 * marginTop specifies the number of pixels of vertical margin
	 * that will be placed along the top edge of the layout.
	 *
	 * The default value is 0.
	 */
	public int marginTop = 0;

	/**
	 * marginBottom specifies the number of pixels of vertical margin
	 * that will be placed along the bottom edge of the layout.
	 *
	 * The default value is 0.
	 */
	public int marginBottom = 0;

	/**
	 * verticalSpacing specifies the number of pixels between the bottom
	 * edge of one cell and the top edge of its neighbouring cell underneath.
	 *
	 * The default value is 1.
	 */
 	public int verticalSpacing = 1;
	
	public BlockLayout(Block block) {
		this.block = block;
	}
	
	@Override
	protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
		int width;
		if (wHint == SWT.DEFAULT) {
			width = block.minimumWidth;
		} else {
			width = wHint; 
		}
		
		// TODO:
		// Not the best.  We should not be actually laying out the controls in order
		// to compute the height of the row.
		// This has an EXTREMELY high probability of getting the layout wrong,
		// because layout may not be called after this, leaving it wrong.
		block.layout(width - Block.marginLeft - Block.marginRight);
		block.positionControls(Block.marginLeft, marginTop, verticalSpacing, composite.getChildren(), flushCache);
		int height = marginTop + block.getHeight(verticalSpacing, composite.getChildren()) + marginBottom;
		return new Point(block.minimumWidth, height);
	}

	@Override
	protected boolean flushCache (Control control) {
		// TODO:
		return false;
	}

	@Override
	protected void layout(Composite composite, boolean flushCache) {
		Rectangle rect = composite.getClientArea ();
		block.layout(rect.width - Block.marginLeft - Block.marginRight);
		
		Control [] children = composite.getChildren ();
		block.positionControls(rect.x + Block.marginLeft, rect.y + marginTop, verticalSpacing, children, flushCache);
	}

	public void paintRowLines(GC gc, Composite composite) {
		block.paintRowLines(gc, Block.marginLeft, marginTop, verticalSpacing, composite.getChildren());
	}
}
