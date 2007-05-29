package net.sf.jmoney.entrytable;

import java.util.ArrayList;

import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public abstract class RowControl<T> extends Composite {

	protected static final Color selectedCellColor = new Color(Display
			.getCurrent(), 255, 255, 255);

	protected abstract boolean commitChanges();
	
	protected abstract void setSelected(boolean isSelected);
	
	protected ArrayList<ICellControl<T>> controls = new ArrayList<ICellControl<T>>();

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
	
	protected class CellFocusListener implements FocusListener {
		private ICellControl cellControl;
		private RowSelectionTracker selectionTracker;
		private FocusCellTracker focusCellTracker;
		
		public CellFocusListener(ICellControl cellControl, RowSelectionTracker selectionTracker, FocusCellTracker focusCellTracker) {
			this.cellControl = cellControl;
			this.selectionTracker = selectionTracker;
			this.focusCellTracker = focusCellTracker;
		}
		
		public void focusLost(FocusEvent e) {
			/*
			 * Save the control and do all the processing in the
			 * focusGained method. This gives us better control over the
			 * process of moving focus. We know then both the old and
			 * new focus cell, which means we know if we are really
			 * moving cells or moving rows.
			 */
			focusCellTracker.setFocusCell(cellControl);
		}
		
		public void focusGained(FocusEvent e) {
			final ICellControl previousFocus = focusCellTracker.getFocusCell();
			if (cellControl == previousFocus) {
				/*
				 * The focus has changed to a different control as far
				 * as SWT is concerned, but the focus is still within
				 * the same cell control. This can happen if the cell
				 * control is a composite that contains multiple child
				 * controls, such as the date control. Focus may move
				 * from the text box of a date control to the button in
				 * the date control, but focus has not left the cell. We
				 * take no action in this situation.
				 */
				return;
			}
			
			if (previousFocus != null) {
				previousFocus.save();

				// Set the control back to the color of this row composite.
				previousFocus.getControl().setBackground(null);
			}

			/*
			 * Note that setSelection will (if the new focus is in a different
			 * row) update the color of all the controls in both rows to reflect
			 * the new selection.  It is thus important that we set the color
			 * of the selected cell after the setSelection call.
			 */
			boolean success = selectionTracker.setSelection(RowControl.this, /*TODO: cellBlock*/null);
			cellControl.getControl().setBackground(selectedCellColor);
			if (!success) {
				// Should only fail if there is a previous control.
				cellControl.getControl().getDisplay().asyncExec (new Runnable () {
					public void run () {
						previousFocus.getControl().setFocus();
					}
				});
			}
		}
	};
}
