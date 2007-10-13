package net.sf.jmoney.entrytable;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public abstract class RowControl<T> extends Composite {

	protected static final Color selectedCellColor = new Color(Display
			.getCurrent(), 255, 255, 255);

	protected abstract boolean commitChanges();
	
	protected abstract void setSelected(boolean isSelected);
	
	// Although currently the keys of this map are never used
	// (and it may as well be a list of the values only), a map
	// allows us to do stuff like move the focus to the control
	// in error during transaction validation.
	protected Map<CellBlock, ICellControl<T>> controls = new HashMap<CellBlock, ICellControl<T>>();

	public RowControl(Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * This version is called when the selection changed as a result
	 * of the user clicking on a control in another row.  Therefore
	 * we do not set the cell selection.
	 */
	public void arrive() {
		setSelected(true);
	}

	public boolean canDepart() {
		if (!commitChanges()) {
			return false;
		}

		setSelected(false);
		return true;
	}
	
	protected class CellFocusListener extends FocusAdapter {
		private ICellControl<T> cellControl;
		private RowSelectionTracker selectionTracker;
		private FocusCellTracker focusCellTracker;
		
		public CellFocusListener(ICellControl<T> cellControl, RowSelectionTracker selectionTracker, FocusCellTracker focusCellTracker) {
			this.cellControl = cellControl;
			this.selectionTracker = selectionTracker;
			this.focusCellTracker = focusCellTracker;
		}
		
	    @Override	
		public void focusGained(FocusEvent e) {
			final ICellControl<?> previousFocus = focusCellTracker.getFocusCell();
			if (cellControl == previousFocus) {
				/*
				 * The focus has changed to a different control as far as SWT is
				 * concerned, but the focus is still within the same cell
				 * control. This can happen if the cell control is a composite
				 * that contains multiple child controls, such as the date
				 * control. Focus may move from the text box of a date control
				 * to the button in the date control, but focus has not left the
				 * cell. We take no action in this situation.
				 * 
				 * This can also happen if focus was lost to a control outside
				 * of the table. This does not change the focus cell within the
				 * table so when focus is returned to the table we will not see
				 * a cell change here.
				 */
				return;
			}

			/*
			 * It is important to set the new focus cell straight away. The
			 * reason is that if, for example, a dialog box is shown (such as
			 * may happen in the selectionTracker.setSelection method below)
			 * then focus will move away from the control to the dialog then
			 * back again when the dialog is closed. If the new focus is already
			 * set then nothing will happen the second time the control gets
			 * focus (because of the test above).
			 */
			focusCellTracker.setFocusCell(cellControl);

			/*
			 * Make sure any changes in the control are written back to the model.
			 */
			if (previousFocus != null) {
				previousFocus.save();
			}

			/*
			 * Opening dialog boxes (as may be done by the
			 * selectionTracker.setSelection method below) and calling setFocus
			 * both cause problems if done from within the focusGained method.
			 * We therefore queue up a new task on this same thread to check
			 * whether the row selection can change and either update the
			 * display (background colors and borders) to show the row selection
			 * or revert the focus to the original cell.
			 */
			cellControl.getControl().getDisplay().asyncExec(new Runnable() {
				public void run() {
					boolean success = selectionTracker.setSelection(RowControl.this, /*TODO: cellBlock*/null);
					if (success) {
						/*
						 * The row selection will have been set by the setSelection method
						 * but we must also update the cell selection.
						 */ 
						if (previousFocus != null) {
							previousFocus.getControl().setBackground(null);
						}

						cellControl.getControl().setBackground(selectedCellColor);
					} else {
						/*
						 * The row selection change was rejected so restore the original cell selection.
						 */
						
						selectionTracker.getSelectedRow().scrollToShowRow();
						
						// TODO: Should we be restoring selection to the cell that needs correcting?
						focusCellTracker.setFocusCell(previousFocus);
						previousFocus.getControl().setFocus();
					}
				}
			});
		}
	}

	/**
	 * This method is called when the selected row may not be visible (because the
	 * user scrolled the table) but we want to make it visible again because there
	 * was an error in it and we were unable to move the selection off the row.
	 */
	protected abstract void scrollToShowRow();
}
