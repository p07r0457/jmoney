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

import java.util.LinkedList;
import java.util.ListIterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Slider;

public class ContentPane extends Composite {

	/** the number of rows in the data structure */
	private int rowCount;

	/**
	 * the 0-based index of the object in the underlying model that is currently
	 * the top row in the visible area
	 */ 
	private int topVisibleRow = 0;

	/**
	 * the list of row objects for all the rows that are currently visible
	 */
	private LinkedList<Row> rows = new LinkedList<Row>();

	/**
	 * the currently selected row, as a 0-based index into the underlying rows,
	 * or -1 if no row is selected.
	 * To get the selected row as an index into the <code>rows</code> list,
	 * you must subtract currentVisibleTopRow from this value.
	 */
	private int currentRow = -1;

	/**
	 * Size of this composite, cached for performance reasons only
	 */
	private Point clientAreaSize;

	private IRowProvider rowProvider;

	/**
	 * Must be non-null.
	 */
	private Slider vSlider;

	/**
	 * The position of the slider.  This content pane and the slider both
	 * update each other.  For example, if page down is pressed in this
	 * content pane then the slider must be updated, but if the slider is
	 * dragged then this content pane must be updated.
	 * 
	 * This class listens to the slider for changes.  However, if this content pane
	 * updates the slider then we don't want the listener to process the change.
	 * We can avoid this from happened if this class always sets the new position
	 * in <code>sliderPosition</code> before changing the slider and if this class's
	 * listener checks the value against <code>sliderPosition</code> before processing. 
	 */
	private int sliderPosition = 0;

	public ContentPane(Composite parent, int style, IRowProvider rowProvider) {
		super(parent, style);
		this.rowProvider = rowProvider;

		rowCount = rowProvider.getRowCount();

		clientAreaSize = getSize(); // Does this do anything???

		addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Point newSize = getSize();

				if (newSize.x != clientAreaSize.x) {
					// Width has changed.  Re-layout the row controls.
					for (Row rowControl: rows) {
						int rowHeight = rowControl.computeSize(newSize.x, SWT.DEFAULT).y;
						rowControl.setSize(newSize.x, rowHeight);
					}
				}

				clientAreaSize = newSize;

				// Refresh.  The first visible row (even if only partially visible)
				// is positioned at the top of the visible area.
				scrollToGivenFix(topVisibleRow, 0);  
			}
		});
		
		// EXPERIMENTAL:
		addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				/*
				 * FEATURE IN SWT: When SWT needs to resolve a mnemonic (accelerator)
				 * character, it recursively calls the traverse event down all
				 * controls in the containership hierarchy.  If e.doit is false,
				 * no control has yet matched the mnemonic, and we don't have to
				 * do anything since we don't do mnemonic matching and no mnemonic
				 * has matched.
				 */
				if (e.doit) {
//					parent.keyTraversed(TableRow.this, e);
				}
			}
		});

	}

	// TODO: this is messy because this finishes up the construction.
	// Stuff here should be in the constructor but can't because no slider
	// is known then.
	public void setVerticalSlider(Slider vSlider) {
		this.vSlider = vSlider;

		vSlider.addSelectionListener(sliderSelectionListener);

		setTopRow(0);
	}


	/**
	 * Scroll the table so that the given row in fully visible. The table is
	 * scrolled by the least amount possible, which means:
	 * <ol>
	 * <li>The row is already fully visible - do not scroll</li>
	 * <li>The row is partially or fully off the top - scroll so the row is
	 * aligned with the top of the visible area</li>
	 * <li>The row is partially or fully off the bottom - scroll so the row is
	 * aligned with the bottom of the visible area</li>
	 * </ol>
	 * 
	 * If the given row is bigger than the visible area then it will not be
	 * possible to fully show that row. In such case, the table is scrolled by
	 * the least amount so that no part of any other row is visible.
	 * 
	 * @param index
	 *            of the row to show, where 0 is the index of the first row
	 *            in the underlying list of rows
	 */
	private void scrollToShowRow(int row) {
		if (row < topVisibleRow) {
			setTopRow(row);
		} else if (row == topVisibleRow) {
			Control rowControl = rows.get(row - topVisibleRow);
			if (rowControl.getLocation().y < 0) {
				setTopRow(row);
			}		
		} else if (row == topVisibleRow+rows.size()-1) {
			Control rowControl = rows.get(row - topVisibleRow);
			if (rowControl.getBounds().y + rowControl.getBounds().height > clientAreaSize.y) {
				setBottomRow(row);
			}
		} else if (row >= topVisibleRow+rows.size()) {
			setBottomRow(row);
		}
	}

	/**
	 * Scroll the view to the given fix and update the scrollbar
	 * to reflect the new position of the visible area.
	 * 
	 * @param anchorRowNumber
	 * @param anchorRowPosition
	 * @return the position at which any attached vertical scrollbar should
	 *     be positioned if it is to match the position of the contents
	 */
	private void scrollToGivenFix(int anchorRowNumber, int anchorRowPosition) {
		scrollViewToGivenFix(anchorRowNumber, anchorRowPosition);

		// Having updated the view, we must move the scroll bar to match.

		/*
		 * This code is all about calculating the position of the vertical
		 * scroll bar that matches the visible content of the visible area.
		 * This is a little bit complex, but I think this code is correct.
		 */

		System.out.println("-----------------");
		for (int row = topVisibleRow; row < topVisibleRow + rows.size(); row++) {
			Rectangle bounds = getTableRow(row).getBounds();

			// so p = (r * height(r) - top(r)) / (n * height(r) - cah)

			double p = ((double)(row * bounds.height - bounds.y)) / (rowCount * bounds.height - clientAreaSize.y);

			double start = ((double)row) / rowCount;
			double end = ((double)(row + 1)) / rowCount;
			System.out.println("p = " + p + ", start = " + start + ", end = " + end + ", height = " + bounds.height);
			if (p >= ((double)row) / rowCount && p <= ((double)(row + 1)) / rowCount) {
				double maximum = vSlider.getMaximum() - vSlider.getThumb();
				sliderPosition = (int)(p * maximum);
				vSlider.setSelection(sliderPosition);
				return;
			}
		}
		System.out.println("no match!!!");
	}

	/**
	 * This method scrolls the table. It updates the list of TableRow
	 * objects and sets the positions appropriately.
	 * 
	 * A row and its position is specified. This method will position rows
	 * above and below in order to fill the visible area.
	 * 
	 * Normally the row must be specified as a valid index into the list of
	 * underlying row objects. That is, it must be not less than zero and
	 * less than the number of rows in the underlying list. However, there
	 * is one exception. This method does allow the index to be equal to the
	 * number of rows (representing a row after the last row) if the
	 * position of that row is at or below the bottom of the visible area.
	 * 
	 * This method does not handle adjustment of the focus or anything like
	 * that. That is up to the caller.
	 * 
	 * @param anchorRowNumber
	 *            the index of a row to be displayed, where the index is a
	 *            0-based index into the underlying model.
	 * @param anchorRowPosition
	 *            the position at which the top of the given row is to be
	 *            positioned, relative to the top of the visible client area
	 */
	private void scrollViewToGivenFix(int anchorRowNumber, int anchorRowPosition) {

		/*
		 * The <code>rows</code> field contains a list of consecutive rows
		 * and <code>topVisibleRow</code> contains the absolute index
		 * of the first row in the list.  These fields must be updated for
		 * the new scroll position.
		 * 
		 * It is very likely that there is an overlap between the new list
		 * and the old list.  However, if a big scroll was done then there
		 * will be no overlap.  If there is an overlap then we really want
		 * to re-use the existing row controls as that saves loading the data
		 * into the controls again.  The approach taken here is to see if the
		 * anchor row was a visible row or was one out from being visible.  If it was then we start
		 * with the previous list of rows and append or remove rows from the
		 * ends as appropriate.  If, however, the new anchor row was not a 
		 * visible row and was more than one row out from being visible then we erase the list and start afresh.
		 */
		if (anchorRowNumber < topVisibleRow-1 || anchorRowNumber > topVisibleRow + rows.size()) {
			while (!rows.isEmpty()) {
				Row rowControl = rows.removeFirst();
				rowProvider.releaseRow(rowControl);
			}
		}

		// Note that we add the following rows before we add the prior rows.  This ensures
		// that the anchor row is added first (if it needs to be added), which ensures we
		// do not break the 'consecutiveness' of the rows.

		/*
		 * Add following rows until we reach the bottom of the client area.  If we
		 * run out of rows in the underlying model then re-align the bottom row
		 * with the bottom. 
		 */
		int rowPosition = anchorRowPosition;
		int rowNumber = anchorRowNumber;
		while (rowPosition < clientAreaSize.y) {
			if (rowNumber == rowCount) {
				// We have run out of rows.  Re-align to bottom.
				anchorRowNumber = rowNumber;
				anchorRowPosition = clientAreaSize.y;
				break;
			}

			Control rowControl = getTableRow(rowNumber);
			int rowHeight = rowControl.computeSize(clientAreaSize.x, SWT.DEFAULT).y;
			rowControl.setSize(clientAreaSize.x, rowHeight);

			rowNumber++;
			rowPosition += rowHeight;
		}

		/* Actually one more than the index of the bottom visible row */
		int newBottomRow = rowNumber; 

		/*
		 * Add prior rows until we reach the top of the client area.  If we
		 * run out of rows in the underlying model then we must scroll to
		 * the top.
		 */
		rowPosition = anchorRowPosition;
		rowNumber = anchorRowNumber;
		while (rowPosition > 0) {
			if (rowNumber == 0) {
				// We have run out of rows.  Re-align to top.
				anchorRowNumber = rowNumber;
				anchorRowPosition = 0;
				break;
			}

			rowNumber--;
			Control rowControl = getTableRow(rowNumber);
			int rowHeight = rowControl.computeSize(clientAreaSize.x, SWT.DEFAULT).y;
			rowControl.setSize(clientAreaSize.x, rowHeight);

			rowPosition -= rowHeight;
		}

		int newTopRow = rowNumber; 
		int newTopRowOffset = rowPosition;

		// Remove extra rows at the start, bringing
		// currentVisibleTopRow (the absolute index of the start
		// of the list) up to newTopRow.
		while (topVisibleRow < newTopRow) {
			Row rowControl = rows.removeFirst();
			rowProvider.releaseRow(rowControl);
			topVisibleRow++;
		}

		// Remove extra rows at the end, bringing
		// currentVisibleTopRow + rows.size (the absolute index of the end
		// of the list) down to newBottomRow.
		while (topVisibleRow + rows.size() > newBottomRow) {
			Row rowControl = rows.removeLast();
			rowProvider.releaseRow(rowControl);
		}

		/*
		 * We have to move the controls front-to-back if we're scrolling
		 * forwards and back-to-front if we're scrolling backwards to avoid ugly
		 * screen refresh artifacts.
		 * 
		 * However, the problem is that 'scrolling forwards' and 'scrolling
		 * backwards' are not well-defined. For example, what happens if a row
		 * in the middle of the visible area is being deleted. The rows below
		 * scroll up a bit to fill the gap. But suppose the rows below are small
		 * and there are not enough to fill the gap. The rows above would then
		 * have to move down to fill the gap. An unlikely situation, but this
		 * demonstrates the difficulty in determining in a well defined way
		 * whether we are scrolling up or down.
		 * 
		 * The solution is that we make two passes through the controls. First
		 * iterate through the row controls moving only the controls that are
		 * being moved upwards, then iterate in reverse moving only the controls
		 * that are being moved downwards.
		 */
		int topPosition = newTopRowOffset;

		ListIterator<Row> rowsIter = rows.listIterator();
		while (rowsIter.hasNext()) {
			Row rowControl = rowsIter.next();
			rowControl.layout(true); // must do first now???? - nrw
			int rowHeight = rowControl.computeSize(clientAreaSize.x, SWT.DEFAULT).y;
			if (rowControl.getBounds().y >= topPosition) {
				rowControl.setBounds(0, topPosition, clientAreaSize.x, rowHeight);
			}
			topPosition += rowHeight;
		}

		while (rowsIter.hasPrevious()) {
			Row rowControl = rowsIter.previous();
			rowControl.layout(true); // must do first now???? - nrw
			int rowHeight = rowControl.computeSize(clientAreaSize.x, SWT.DEFAULT).y;
			topPosition -= rowHeight;
			if (rowControl.getBounds().y < topPosition) {
				rowControl.setBounds(0, topPosition, clientAreaSize.x, rowHeight);
			}
		}
	}

	void setTopRow(int topRow) {
		scrollToGivenFix(topRow, 0);

	}

	private void setBottomRow(int bottomRow) {
		scrollToGivenFix(bottomRow + 1, clientAreaSize.y);
	}

	/**
	 * Returns the TableRow object for the given row, creating a row
	 * if it does not exist.
	 * 
	 * This method should only be called when we know that the given
	 * index is either within the currently set visible range or is
	 * one out (the next row before the first or the next row after
	 * the last).  This method may also be called when the currently
	 * set visible range is empty.
	 * 
	 * @param rowNumber the 0-based index of the required row, based
	 * 			on the rows in the underlying model
	 * @return
	 */
	private Row getTableRow(int rowNumber) {
		if (rowNumber >= topVisibleRow && rowNumber < topVisibleRow + rows.size()) {
			Row row = rows.get(rowNumber - topVisibleRow);
			if (row == null)
				System.out.println("");
			return row;
		} else {
			// we must create a new row object
			Row newRow = rowProvider.getNewRow(this, rowNumber);
			if (newRow == null)
				System.out.println("");

			Control rowControl = newRow;
			int rowHeight = rowControl.computeSize(clientAreaSize.x, SWT.DEFAULT).y;
			rowControl.setSize(clientAreaSize.x, rowHeight);

			if (rows.isEmpty()) {
				rows.add(newRow);
				topVisibleRow = rowNumber;
			} else if (rowNumber == topVisibleRow-1) {
				rows.addFirst(newRow);
				topVisibleRow = rowNumber;
			} else if (rowNumber == topVisibleRow+rows.size()) {
				rows.addLast(newRow);
			} else {
				throw new RuntimeException("internal error");
			}
			if (rows.size() > 20) {
				System.out.println("big rows");
			}

			return newRow;
		}
	}

	/**
	 * Page up will scroll the table so that the row above the first
	 * fully visible row becomes the last row, with the bottom of the row
	 * aligned with the bottom of the visible area.  If there is no fully
	 * visible row but there are two partially visible rows then the table
	 * will be scrolled so that the bottom of the top row is aligned with
	 * the bottom of the visible area.  If there is only one row visible and
	 * it is only partially visible then the table is scrolled by the visible height.
	 */
	public void doPageUp() {
		if (currentRow == -1) {
			return;
		}

		if (currentRow > 0) {
			Row selectedRow = getSelectedRow();

			if (!selectedRow.canDepart()) {
				return;
			}

			int currentColumn = selectedRow.getCurrentColumn();

			/*
			 * Get previous row until we reach a row that, if positioned at the
			 * top of the visible area, puts the bottom of the previous current
			 * row at the bottom or below the bottom of the visible area.
			 */
			int totalHeight = 0;
			do {
				int rowHeight = getTableRow(currentRow).getBounds().height;
				totalHeight += rowHeight;
				if (totalHeight >= clientAreaSize.y) {
					break;
				}
				currentRow--;
			} while (currentRow > 0);

			scrollToShowRow(currentRow);
			getTableRow(currentRow).arrive(currentColumn);
		}
	}

	public void doPageDown() {
		if (currentRow == -1) {
			return;
		}

		if (currentRow < rowCount - 1) {
			Row selectedRow = getSelectedRow();

			if (!selectedRow.canDepart()) {
				return;
			}

			int currentColumn = selectedRow.getCurrentColumn();

			/*
			 * Get previous row until we reach a row that, if positioned at the
			 * bottom of the visible area, puts the top of the previous current
			 * row at the top or above the top of the visible area.
			 */
			int totalHeight = 0;
			do {
				int rowHeight = getTableRow(currentRow).getBounds().height;
				totalHeight += rowHeight;
				if (totalHeight >= clientAreaSize.y) {
					break;
				}
				currentRow++;
			} while (currentRow < rowCount - 1);

			scrollToShowRow(currentRow);
			getTableRow(currentRow).arrive(currentColumn);
		}
	}

	public void doRowUp() {
		if (currentRow == -1) {
			return;
		}

		if (currentRow > 0) {
			Row selectedRow = getSelectedRow();

			if (!selectedRow.canDepart()) {
				return;
			}

			int currentColumn = selectedRow.getCurrentColumn();

			currentRow--;
			scrollToShowRow(currentRow);
			getTableRow(currentRow).arrive(currentColumn);
		}
	}

	public void doRowDown() {
		if (currentRow == -1) {
			return;
		}

		if (currentRow < rowCount - 1) {
			Row selectedRow = getSelectedRow();

			if (!selectedRow.canDepart()) {
				return;
			}

			int currentColumn = selectedRow.getCurrentColumn();

			currentRow++;
			scrollToShowRow(currentRow);
			getTableRow(currentRow).arrive(currentColumn);
		}
	}

	private Row getSelectedRow() {
		if (currentRow == -1) {
			return null;
		} else {
			return rows.get(currentRow - this.topVisibleRow);
		}
	}

	/**
	 * Sets the focus to the given row and column.
	 * 
	 * @param row
	 * @param entriesSectionProperty
	 * @return true if the new row selection could be made, false if there
	 * 		are issues with a previously selected row that prevent the change
	 * 		in selection from being made
	 */
	public boolean setSelection(Row row,
			IEntriesTableProperty entriesSectionProperty) {
		Row currentRowControl = getSelectedRow();
		if (row != currentRowControl) {
			if (currentRowControl != null) {
				if (!currentRowControl.canDepart()) {
					return false;
				}
			}
			
			row.arrive();  // Causes the selection colors etc.  Focus is already set.
			currentRow = rows.indexOf(row) + topVisibleRow; 
		}
		return true;
	}

	/**
	 * The SelectionListener for the table's vertical slider control.
	 * 
	 * Note that the selection never changes when the table is scrolled using
	 * the scroll bar.  The UI would be rather confusing otherwise, because
	 * then dragging the scroll bar would either cause a dialog to pop up asking if
	 * the changes in the current selection should be committed or would cause
	 * the changes to be committed without warning.  The usual convention with
	 * tables is to allow the selection to be scrolled off the screen, so that
	 * is what we do.
	 */
	private SelectionListener sliderSelectionListener = new SelectionListener() {
		public void widgetSelected(SelectionEvent e) {
			switch (e.detail) {
			case SWT.ARROW_DOWN:
			{
				/*
				 * Scroll down so that the next row below the top visible row
				 * becomes the top visible row.
				 */
				scrollToGivenFix(topVisibleRow + 1, 0);
			}
			break;

			case SWT.ARROW_UP:
			{
				/*
				 * Scroll up so that the next row above the top visible row
				 * becomes the top visible row.
				 */
				if (topVisibleRow > 0) {
					scrollToGivenFix(topVisibleRow - 1, 0);
				}
			}
			break;

			case SWT.PAGE_DOWN:
			{
				/*
				 * Page down so that the lowest visible row (or
				 * partially visible row) becomes the top row.
				 * 
				 *  However, if the lowest visible row is also the top
				 *  row (i.e. the row is so high that it fills the visible
				 *  area) then scroll up by 90% of the height of the visible
				 *  area.
				 */
				int bottomRow = topVisibleRow + rows.size() - 1;
				Control rowControl = rows.get(bottomRow - topVisibleRow); 
				if (rows.size() == 1) {
					scrollToGivenFix(bottomRow, rowControl.getBounds().y - clientAreaSize.y * 90 / 100);
				} else {
					scrollToGivenFix(bottomRow, 0);
				}
			}
			break;

			case SWT.PAGE_UP:
			{
				/*
				 * Page up so that the first visible row (or
				 * partially visible row) becomes the bottom row,
				 * aligned with the bottom of the visible area.
				 * 
				 *  However, if the first visible row is also the bottom
				 *  row (i.e. the row is so high that it fills the visible
				 *  area) then scroll down by 90% of the height of the visible
				 *  area.
				 */
				Control rowControl = rows.get(topVisibleRow); 
				if (rows.size() == 1) {
					scrollToGivenFix(topVisibleRow, rowControl.getBounds().y + clientAreaSize.y * 90 / 100);
				} else {
					scrollToGivenFix(topVisibleRow+1, clientAreaSize.y);
				}
			}
			break;

			case SWT.NONE:
			case SWT.DRAG:
			default:
				// Assume scroll bar dragged.
			{
				/*
				 * The JavaDoc is incorrect.  It states that the selection can take
				 * any value from 'minimum' to 'maximum'.  In fact the largest value
				 * it can take is maximum - thumb (it has this value when the slider
				 * is scrolled completely to the bottom).
				 * 
				 * We cannot rely on the thumb size because it changes.  The thumb size
				 * will be appropriate for the last position but may not be correct
				 * for the new position.  We therefore calculate a proportion between
				 * 0 and 1.
				 */
				int selection = vSlider.getSelection();
				if (selection == sliderPosition) {
					return;
				}
				sliderPosition = selection;

				if (sliderPosition == 90) {
					System.out.println("");
				}
				double maximum = vSlider.getMaximum() - vSlider.getThumb();
				double portion = selection / maximum;

				/*
				 * find the 'anchor' row.  All other visible rows are positioned upwards
				 * and downwards from this control.
				 */
				double rowDouble = portion * rowCount;
				int anchorRowNumber = Double.valueOf(Math.floor(rowDouble)).intValue();
				double rowRemainder = rowDouble - anchorRowNumber;

				// If this row is not in the set of previously visible rows then
				// start afresh.
				if (anchorRowNumber < topVisibleRow || anchorRowNumber >= topVisibleRow + rows.size()) {
					while (!rows.isEmpty()) {
						Row row = rows.removeFirst();
						rowProvider.releaseRow(row);
					}
					rows.clear();
				}

				if (anchorRowNumber == rowCount) {
					scrollViewToGivenFix(anchorRowNumber, clientAreaSize.y);
				} else {
				Row anchorRowControl = getTableRow(anchorRowNumber);
				int anchorRowHeight = anchorRowControl.computeSize(clientAreaSize.x, SWT.DEFAULT).y;
				int anchorRowPosition = (int)(portion * clientAreaSize.y - anchorRowHeight * rowRemainder);
				anchorRowControl.setSize(clientAreaSize.x, anchorRowHeight);
				scrollViewToGivenFix(anchorRowNumber, anchorRowPosition);
				}
			}
			}
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
	};

	protected ICellControl previousFocus = null;

	public void refreshContent() {
		for (Row rowControl: rows) {
			rowProvider.releaseRow(rowControl);
		}
		
		rowCount = rowProvider.getRowCount();
		rows.clear();
		
		setTopRow(0);
	}
}


