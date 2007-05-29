/**
 * 
 */
package net.sf.jmoney.entrytable;

class FocusCellTracker {
	/**
	 * The control that is currently set up as the control
	 * with the focus.
	 */
	protected ICellControl cellControl = null;

	public void setFocusCell(ICellControl cellControl) {
		this.cellControl = cellControl;
	}

	public ICellControl getFocusCell() {
		return cellControl;
	}
}