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

import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.isolation.UncommittedObjectKey;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.pages.entries.ForeignCurrencyDialog;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class Row extends Composite {
	// The darker blue and green lines for the listed entry in each transaction
	protected static final Color transactionColor = new Color(Display
			.getCurrent(), 235, 235, 255);

	protected static final Color alternateTransactionColor = new Color(Display
			.getCurrent(), 235, 255, 237);

	// The lighter colors for the sub-entry lines
	protected static final Color entryColor = new Color(Display.getCurrent(),
			245, 245, 255);

	protected static final Color alternateEntryColor = new Color(Display
			.getCurrent(), 245, 255, 255);

	protected static final Color selectedRowColor = new Color(Display.getCurrent(), 215, 215, 255);

	protected static final Color selectedCellColor = new Color(Display
	.getCurrent(), 255, 255, 255);

	/**
	 * The transaction manager used for all changes made in
	 * this row.  It is created when the contents are set into this object and
	 * remains usable for the rest of the time that this object
	 * represents a visible row.
	 */
	TransactionManager transactionManager = null;

	/**
	 * The EntryData object currently set into this object, or null
	 * if this object does not represent a currently visible row
	 */
	EntryData dataInTransaction = null;

	private Vector<ICellControl> controls = new Vector<ICellControl>();

	/**
	 * The color of this row when this row is not selected.  Set only when content is set.
	 */
	private Color rowColor;

	/**
	 * true if this row is the current selection, false otherwise
	 */
	private boolean isSelected = false;

	/**
	 * Forward key presses to the parent control
	 */
	private KeyListener keyListener = new KeyAdapter() {
		public void keyPressed(KeyEvent e) {
//			parent.keyPressed(TableRow.this, e);
		}
	};

	/**
	 * Forward traverse events to the parent control
	 */
	private TraverseListener traverseListener = new TraverseListener() {
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
//				parent.keyTraversed(TableRow.this, e);
			}
		}
	};

	private PaintListener paintListener = new PaintListener() {
		public void paintControl(PaintEvent e) {
			drawBorder(e.gc);
		}
	};

	public Row(final ContentPane parent, int style, final EntriesTable entriesTable) {
		super(parent, style);

		/*
		 * We have a margin of 1 at the top and 2 at the bottom.
		 * The reason for this is because we want a 2-pixel wide black
		 * line around the selected row.  However, a 1-pixel wide black
		 * line is drawn along the bottom of every row.  Therefore we
		 * we want to draw only a 1-pixel wide black line at the top of
		 * the selected row.  The top and bottom margins are there only
		 * so we can draw these lines.
		 */
		BlockLayout layout = new BlockLayout(entriesTable.rootBlock);
		layout.marginTop = 1;
		layout.marginBottom = 2;
		layout.verticalSpacing = 1;
		setLayout(layout);

		/*
		 * By default the child controls get the same background as
		 * this composite.
		 */
		setBackgroundMode(SWT.INHERIT_FORCE);

		for (final IEntriesTableProperty entriesSectionProperty: entriesTable.getCellList()) {
			// Create the control with no content set.
			final ICellControl cellControl = entriesSectionProperty.createCellControl(this, entriesTable.getSession());
			controls.add(cellControl);

			FocusListener controlFocusListener = new FocusAdapter() {
				public void focusLost(FocusEvent e) {
					/*
					 * Save the control and do all the processing in the
					 * focusGained method. This gives us better control over the
					 * process of moving focus. We know then both the old and
					 * new focus cell, which means we know if we are really
					 * moving cells or moving rows.
					 */
					parent.previousFocus = cellControl;
				}
				public void focusGained(FocusEvent e) {
					if (cellControl == parent.previousFocus) {
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
					
					if (parent.previousFocus != null) {
						parent.previousFocus.save();

						// Set the control back to the color of this row composite.
						parent.previousFocus.getControl().setBackground(null);
					}

					/*
					 * Note that setSelection will (if the new focus is in a different
					 * row) update the color of all the controls in both rows to reflect
					 * the new selection.  It is thus important that we set the color
					 * of the selected cell after the setSelection call.
					 */
					boolean success = parent.setSelection(Row.this, entriesSectionProperty);
					cellControl.getControl().setBackground(selectedCellColor);
					if (!success) {
						// Should only fail if there is a previous control.
						parent.getDisplay().asyncExec (new Runnable () {
							public void run () {
								parent.previousFocus.getControl().setFocus();
							}
						});
					}
				}
			};
			
			Control control = cellControl.getControl();
//			control.addKeyListener(keyListener);
			addFocusListenerRecursively(control, controlFocusListener);
//			control.addTraverseListener(traverseListener);
			
			// This is needed in case more child controls are created at a
			// later time.  This is not the cleanest code, but the UI for  these
			// split entries may be changed at a later time anyway.
			cellControl.setFocusListener(controlFocusListener);
		}

		addPaintListener(paintListener);
	}

	/**
	 * Add listeners to each control.
	 * 
	 * @param control The control to listen to.
	 */
	private void addFocusListenerRecursively(Control control, FocusListener listener) {
		control.addFocusListener(listener);
		
		if (control instanceof Composite) {
			Composite composite = (Composite) control;
			for (int i = 0; i < composite.getChildren().length; i++) {
				Control childControl = composite.getChildren()[i];
				addFocusListenerRecursively(childControl, listener);
			}
		}
	}
	
	/**
	 * Draws a border around the row.  A light gray single line is
	 * drawn at the bottom if the row is not selected.  A black double
	 * line is drawn at the bottom and a black single line at the top and
	 * sides if the row is selected.
	 * @param gc 
	 */
	protected void drawBorder(GC gc) {
		Color oldColor = gc.getBackground();
		try {
			
			
			// Get the colors we need
			Display display = Display.getCurrent();
			Color blackColor = display
			.getSystemColor(SWT.COLOR_BLACK);
			// pretty black
			Color lineColor = display
			.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
			// Looks white
			Color secondaryColor = display
			.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
			// Fairly dark gray
			Color hilightColor = display
			.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW);

			Point controlSize = getSize();

			// Draw the bottom line(s)
			if (isSelected) {
				gc.setBackground(blackColor);
				// top edge
				gc.fillRectangle(0, 0, controlSize.x, 1);
				// bottom edge
				gc.fillRectangle(0, controlSize.y - 2, controlSize.x, 2);
				// left edge
				gc.fillRectangle(0, 1, 1, controlSize.y-3);
				// right edge
				gc.fillRectangle(controlSize.x - 1, 1, 1, controlSize.y-3);
			} else {
				gc.setBackground(lineColor);
				gc.fillRectangle(0, controlSize.y - 1, controlSize.x, 1);
			}

			/* Now draw lines between the child controls.  This involves calling
			 * into the the block tree.  The reason for this is that if we
			 * draw lines to the left and right of each control then we are drawing
			 * twice as many lines as necessary, if we draw just on the left or just
			 * on the right then we would not get every case, and the end conditions
			 * would not be handled correctly.  Using the tree structure just gives
			 * us better control over the drawing.
			 * 
			 * This method is called on the layout because it uses the cached positions
			 * of the controls.
			 */
			gc.setBackground(secondaryColor);
			((BlockLayout)getLayout()).paintRowLines(gc, this);
		} finally {
			gc.setBackground(oldColor);
		}
	}

	public void setContent(EntryData data) {
		rowColor = (data.getIndex()%2 == 0)  ? alternateTransactionColor : transactionColor;
		setBackground(rowColor);

		/*
		 * Every row gets its own transaction.  This ensures that edits can be
		 * made at any time but the edits are not validated and no one else sees
		 * the changes until the selection is moved off this row.
		 */
		transactionManager = new TransactionManager(data.getBaseSessionManager());
		Entry entryInTransaction;
		if (data.getEntry() == null) {
			Transaction newTransaction = transactionManager.getSession().createTransaction();
			entryInTransaction = newTransaction.createEntry();
			newTransaction.createEntry();

			// TODO: Kludge here
			((EntriesTable)getParent().getParent().getParent()).entriesContent.setNewEntryProperties(entryInTransaction);
		} else {
			entryInTransaction = transactionManager.getCopyInTransaction(data.getEntry());
		}
		dataInTransaction = new EntryData(entryInTransaction, transactionManager);
		dataInTransaction.setIndex(data.getIndex());
		dataInTransaction.setBalance(data.getBalance());
		
		for (final ICellControl control: controls) {
			control.load(dataInTransaction);
		}
	}

	private void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
		Color backgroundColor = (isSelected ? selectedRowColor : rowColor); 
		setBackground(backgroundColor);
//		this.redraw();
	}

	/**
	 * Validate the changes made by the user to this row and, if they are valid,
	 * commit them.
	 * 
	 * The changes may not be committed for a number of reasons. Perhaps they
	 * did not meet the restrictions imposed by a validating listener, or
	 * perhaps the user responded to a dialog in a way that indicated that the
	 * changes should not be committed.
	 * 
	 * if false is returned then the caller should leave this row as the
	 * selected row.
	 * 
	 * @return true if the changes are either valid and were committed or were
	 *         canceled by the user, false if the changes were neither committed
	 *         or cancelled (and thus remain outstanding)
	 */
	private boolean commitChanges() {
		// If changes have been made then check they are valid and ask
		// the user if the changes should be committed.
		if (transactionManager.hasChanges()) {
			// Validate the transacion.

			long totalAmount = 0;
			Commodity commodity = null;
			boolean mixedCommodities = false;

			// TODO: itemWithError is not actually used.  See if there is an
			// easy way of accessing the relevant controls.  Otherwise we should
			// delete this.

			class InvalidUserEntryException extends Exception {
				private static final long serialVersionUID = -8693190447361905525L;

				Control itemWithError = null;

				public InvalidUserEntryException(String message, Control itemWithError) {
					super(message);
					this.itemWithError = itemWithError;
				}

				public Control getItemWithError() {
					return itemWithError;
				}
			};

			try {
				if (dataInTransaction.getEntry().getTransaction().getDate() == null) {
					throw new InvalidUserEntryException(
							"The date cannot be blank.",
							null);
				} else {
					for (Entry entry: dataInTransaction.getEntry().getTransaction().getEntryCollection()) {
						if (entry.getAccount() == null) {
							throw new InvalidUserEntryException(
									"A category must be selected.",
									null);
						}

						if (entry.getAccount() instanceof IncomeExpenseAccount) {
							IncomeExpenseAccount incomeExpenseAccount = (IncomeExpenseAccount)entry.getAccount();
							if (incomeExpenseAccount.isMultiCurrency()
									&& entry.getIncomeExpenseCurrency() == null) {
								throw new InvalidUserEntryException(
										"A currency must be selected (" + incomeExpenseAccount.getName() + " is a multi-currency category).",
										null);
							}
						}

						if (commodity == null) {
							commodity = entry.getCommodity();
						} else if (!commodity.equals(entry.getCommodity())) {
							mixedCommodities = true;
						}

						totalAmount += entry.getAmount();
					}
				}

				/*
				 * If all the entries are in the same currency then the sum of
				 * the entries in the transaction must add to zero. In a
				 * transaction with child rows we display an error to the user
				 * if the sum is not zero. However, in a simple transaction the
				 * amount of the income and expense is not shown because it
				 * always matches the amount of the credit or debit. The amounts
				 * may not match if, for example, the currencies used to differ
				 * but the user changed the category so that the currencies now
				 * match. We present the data to the user as tho the other
				 * amount does not exist, so we should silently correct the
				 * amount.
				 */
				if (totalAmount != 0 && !mixedCommodities) {
					// TODO: For double entries where both accounts are in the same currency,
					// should the amount for one account automatically change when the user changes
					// the amount for the other account?  Currently the user must update both
					// to keep the transaction balanced and to avoid the following error message.
					if (dataInTransaction.hasSplitEntries() || dataInTransaction.isDoubleEntry()) {
						throw new InvalidUserEntryException(
								"The transaction does not balance.  " +
								"Unless some entries in the transaction are in different currencies, " +
								"the sum of all the entries in a transaction must add up to zero.",
								null);
					} else {
						Entry accountEntry = dataInTransaction.getEntry();
						Entry otherEntry = dataInTransaction.getOtherEntry();
						otherEntry.setAmount(-accountEntry.getAmount());
					}
				}

				/*
				 * Check for zero amounts. A zero amount is
				 * normally a user error and will not be accepted. However, if
				 * this is a simple transaction and the currencies are different
				 * then we prompt the user for the amount of the other entry
				 * (the income and expense entry). This is very desirable
				 * because the foreign currency column (being used so little) is
				 * not displayed by default.
				 */
				if (dataInTransaction.isSimpleEntry()
						&& dataInTransaction.getEntry().getAmount() != 0
						&& dataInTransaction.getOtherEntry().getAmount() == 0
						&& dataInTransaction.getOtherEntry().getCommodity() != dataInTransaction.getEntry().getCommodity()) {
					ForeignCurrencyDialog dialog = new ForeignCurrencyDialog(
							getShell(),
							dataInTransaction);
					dialog.open();
				} else {
					for (Entry entry: dataInTransaction.getEntry().getTransaction().getEntryCollection()) {
						if (entry.getAmount() == 0) {
							throw new InvalidUserEntryException(
									"A non-zero credit or debit amount must be entered.",
									null);
						}
					}
				}
			} catch (InvalidUserEntryException e) {
				MessageDialog.openError(
						getShell(), 
						"Incomplete or invalid data in entry", 
						e.getLocalizedMessage());

				e.getItemWithError().setFocus();

				return false;
			}

			// Commit the changes to the transaction
			transactionManager.commit("Transaction Changes");
			
			/*
			 * It may be that this was a new entry not previously committed.
			 * If so, the committed entry in the EntryData object will be null
			 * and we must set it now to the non-null newly committed entry.
			 * This ensures that the EntryData now represents a regular entry.
			 */
			if (dataInTransaction.getEntry() == null) {
				UncommittedObjectKey uncommittedKey = (UncommittedObjectKey)dataInTransaction.getEntry().getObjectKey();
				Entry committedEntry = (Entry)uncommittedKey.getCommittedObjectKey().getObject();
				dataInTransaction.setEntry(committedEntry);
			}
		}

		return true;
	}

	/**
	 * This method is used when the user wants to duplicate a transaction.
	 * This method should be called only when this row contains a new entry
	 * that has no properties set and that has not yet been committed
	 * (i.e. it is the blank row that the user may use for creating new entries).
	 * 
	 * Certain properties are copied in from the given entry.  The given entry
	 * must be a committed entry in the same session manager to which this entry
	 * would be committed.
	 * 
	 * This method leaves this row uncommitted.  Therefore it is important that
	 * the caller always sets focus to this row after calling this method.
	 * (By design, only the focus row should ever have uncommitted data).
	 */
	public void initializeFromTemplate(EntryData sourceEntryData) {
		Transaction targetTransaction = dataInTransaction.getEntry().getTransaction(); 
		
		copyData(sourceEntryData.getEntry(), dataInTransaction.getEntry());

		Iterator<Entry> iter = dataInTransaction.getSplitEntries().iterator();
		for (Entry sourceEntry: sourceEntryData.getSplitEntries()) {
			Entry targetEntry;
			if (iter.hasNext()) {
				targetEntry = iter.next();
			} else {
				targetEntry = targetTransaction.createEntry();
			}

			targetEntry.setAccount(transactionManager.getCopyInTransaction(sourceEntry.getAccount()));
			copyData(sourceEntry, targetEntry);
		}
	}

	/**
	 * Private method used when duplicating a transaction.
	 * 
	 * @param sourceEntry
	 * @param targetEntry
	 */
	private void copyData(Entry sourceEntry, Entry targetEntry) {
		targetEntry.setDescription(sourceEntry.getDescription());
		targetEntry.setMemo(sourceEntry.getMemo());
		targetEntry.setIncomeExpenseCurrency(transactionManager.getCopyInTransaction(sourceEntry.getIncomeExpenseCurrency()));
		targetEntry.setAmount(sourceEntry.getAmount());
	}

	public void arrive(int currentColumn) {
		setSelected(true);
		getChildren()[currentColumn].setFocus();
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
	 * Gets the column that has the focus.
	 * 
	 * This method is used to preserve the column selection when cursoring up
	 * and down between rows.  If the column cannot be determined that simply
	 * return 0 so that the first column gets the focus in the new row.
	 * 
	 * @return the 0-based index of the cell in this row that has the focus
	 */
	public int getCurrentColumn() {
		// TODO: We can probably save this information in the focus
		// listener, which will work much better.  For example, if the
		// focus is not in the table but a cell is still in selected mode
		// then the correct value will be returned.
		Control [] children = getChildren();
		for (int columnIndex = 0; columnIndex < children.length; columnIndex++) {
			if (hasFocus(children[columnIndex])) {
				return columnIndex;
			}
		}

		return 0;
	}

	private boolean hasFocus(Control control) {
		if (control.isFocusControl()) return true;

		// We probably have to call recursively on child controls -
		// but let's try first without.
		return false;
	}
}
