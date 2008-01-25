package net.sf.jmoney.entrytable;

import java.util.HashMap;
import java.util.Map;

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
	protected Map<CellBlock, ICellControl<? super T>> controls = new HashMap<CellBlock, ICellControl<? super T>>();

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
	
	/**
	 * This method is called when the selected row may not be visible (because the
	 * user scrolled the table) but we want to make it visible again because there
	 * was an error in it and we were unable to move the selection off the row.
	 */
	protected abstract void scrollToShowRow();
}
