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

import java.applet.Applet;
import java.applet.AudioClip;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.pages.entries.ForeignCurrencyDialog;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
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

public abstract class BaseEntryRowControl<T extends EntryData> extends RowControl<T> {
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

	private static AudioClip clip;
	static {
		IPath path = new Path("icons").append("ding.au");
		URL url = FileLocator.find(Platform.getBundle(JMoneyPlugin.PLUGIN_ID), path, null);
		clip = Applet.newAudioClip(url);
	}

	private VirtualRowTable rowTable;
	
	/**
	 * The transaction manager used for all changes made in
	 * this row.  It is created when the contents are set into this object and
	 * remains usable for the rest of the time that this object
	 * represents a visible row.
	 */
	TransactionManager transactionManager = null;

	/**
	 * The EntryData object on which this row is based.  This will contain
	 * the committed version of the entry, or a null Entry object if this row
	 * represents the 'new entry' row.
	 */
	protected T committedEntryData = null;

	/**
	 * The EntryData object currently set into this object, or null
	 * if this object does not represent a currently visible row
	 */
	protected T uncommittedEntryData = null;

	/**
	 * true if this row is the current selection, false otherwise
	 */
	private boolean isSelected = false;

	/**
	 * Forward key presses to the parent control
	 */
	private KeyListener keyListener = new KeyAdapter() {
		@Override
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
			 * controls in the container hierarchy.  If e.doit is false,
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

	private ArrayList<IBalanceChangeListener> balanceChangeListeners = new ArrayList<IBalanceChangeListener>();

	public BaseEntryRowControl(final Composite parent, int style, VirtualRowTable rowTable, Block<T, ?> rootBlock) {
		super(parent, style);
		this.rowTable = rowTable;
		
		/*
		 * We have a margin of 1 at the top and 2 at the bottom.
		 * The reason for this is because we want a 2-pixel wide black
		 * line around the selected row.  However, a 1-pixel wide black
		 * line is drawn along the bottom of every row.  Therefore we
		 * we want to draw only a 1-pixel wide black line at the top of
		 * the selected row.  The top and bottom margins are there only
		 * so we can draw these lines.
		 */
		BlockLayout layout = new BlockLayout(rootBlock, false);
		layout.marginTop = 1;
		layout.marginBottom = 2;
		layout.verticalSpacing = 1;
		setLayout(layout);

		/*
		 * By default the child controls get the same background as
		 * this composite.
		 */
		setBackgroundMode(SWT.INHERIT_FORCE);

		addPaintListener(paintListener);
	}

	/**
	 * This method must always be called by the constructor of the final derived
	 * classes of this class.  Why do we not just call it from the constructor
	 * of this class?  The reason is two fold:
	 * 
	 * 1. The controls that are created by this method have a back reference to
	 * this object.  These back references are typed (using generics) to the
	 * final derived type.  These controls will expect field initializers and
	 * possibly constructor initialization to have been done on the final derived type.
	 * However, at the time the base constructor is called, neither will have
	 * been initialized.
	 * 
	 * 2. (A lesser reason).  We need parameterize just this method.  If this were
	 * done in the constructor, the entire class would need to be parameterized.
	 *  
	 * @param rootBlock
	 * @param selectionTracker
	 * @param focusCellTracker
	 */
	protected <R extends BaseEntryRowControl<T>> void init(R thisRowControl, Block<T, ? super R> rootBlock,
			RowSelectionTracker<R> selectionTracker,
			FocusCellTracker focusCellTracker) {
		for (CellBlock<? super T, ? super R> cellBlock: rootBlock.buildCellList()) {
			// Create the control with no content set.
			final ICellControl<? super T> cellControl = cellBlock.createCellControl(thisRowControl);
			controls.put(cellBlock, cellControl);

			FocusListener controlFocusListener = new CellFocusListener<R>(thisRowControl, cellControl, selectionTracker, focusCellTracker);
			
			Control control = cellControl.getControl();
//			control.addKeyListener(keyListener);
			addFocusListenerRecursively(control, controlFocusListener);
//			control.addTraverseListener(traverseListener);
			
			// This is needed in case more child controls are created at a
			// later time.  This is not the cleanest code, but the UI for  these
			// split entries may be changed at a later time anyway.
			cellControl.setFocusListener(controlFocusListener);
		}
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

	public void setContent(T committedEntryData) {
		this.committedEntryData = committedEntryData;

		setAppropriateBackgroundColor();

		/*
		 * Every row gets its own transaction.  This ensures that edits can be
		 * made at any time but the edits are not validated and no one else sees
		 * the changes until the selection is moved off this row.
		 */
		// TODO: Some cleanup.  We don't need to create a new transaction each time.
		// Perhaps we should have separate derived classes for the regular rows and
		// the new entry row.
		transactionManager = new TransactionManager(committedEntryData.getBaseSessionManager());
		Entry entryInTransaction;
		if (committedEntryData.getEntry() == null) {
			Transaction newTransaction = transactionManager.getSession().createTransaction();
			entryInTransaction = createNewEntry(newTransaction);
		} else {
			entryInTransaction = transactionManager.getCopyInTransaction(committedEntryData.getEntry());
		}
		uncommittedEntryData = createUncommittedEntryData(entryInTransaction, transactionManager);
		uncommittedEntryData.setIndex(committedEntryData.getIndex());
		uncommittedEntryData.setBalance(committedEntryData.getBalance());
		
		for (final ICellControl<? super T> control: controls.values()) {
			control.load(uncommittedEntryData);
		}
	}

	protected abstract T createUncommittedEntryData(Entry entryInTransaction,
			TransactionManager transactionManager);

	@Override
	protected void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
		setAppropriateBackgroundColor();
		
		/*
		 * We need to tell the table that contains this row. This allows the
		 * table to release the row if it is not currently visible and is now
		 * not selected.
		 */
		if (!isSelected) {
			rowTable.rowDeselected(this);
		}
	}

	@Override
	protected boolean commitChanges() {
		return commitChanges(
				(committedEntryData.getEntry() == null)
				? "New Transaction"
				: "Transaction Changes");
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
	 *         nor discarded (and thus remain outstanding)
	 */
	public boolean commitChanges(String transactionLabel) {
		// If changes have been made then check they are valid and ask
		// the user if the changes should be committed.
		if (transactionManager.hasChanges()) {
			// Validate the transaction.

			long totalAmount = 0;
			Commodity commodity = null;
			boolean mixedCommodities = false;

			// TODO: itemWithError is not actually used.  See if there is an
			// easy way of accessing the relevant controls.  Otherwise we should
			// delete this.

			try {
				if (uncommittedEntryData.getEntry().getTransaction().getDate() == null) {
					throw new InvalidUserEntryException(
							"The date cannot be blank.",
							null);
				}

				for (Entry entry: uncommittedEntryData.getEntry().getTransaction().getEntryCollection()) {
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
					if (uncommittedEntryData.hasSplitEntries()) {
						throw new InvalidUserEntryException(
								"The transaction does not balance.  " +
								"Unless some entries in the transaction are in different currencies, " +
								"the sum of all the entries in a transaction must add up to zero.",
								null);
					} else {
						Entry accountEntry = uncommittedEntryData.getEntry();
						Entry otherEntry = uncommittedEntryData.getOtherEntry();
						otherEntry.setAmount(-accountEntry.getAmount());
					}
				}

				/*
				 * Check for zero amounts. A zero amount is normally a user
				 * error and will not be accepted. However, if this is not a
				 * split transaction and the currencies are different then we
				 * prompt the user for the amount of the other entry (the income
				 * and expense entry). This is very desirable because the
				 * foreign currency column (being used so little) is not
				 * displayed by default.
				 */
				// TODO: We could drop down the shell as though this is a split
				// entry whenever the currencies do not match.  This would expose
				// the amount of the other entry.
				if (!uncommittedEntryData.hasSplitEntries()
						&& uncommittedEntryData.getEntry().getAmount() != 0
						&& uncommittedEntryData.getOtherEntry().getAmount() == 0
						&& uncommittedEntryData.getOtherEntry().getCommodity() != uncommittedEntryData.getEntry().getCommodity()) {
					ForeignCurrencyDialog dialog = new ForeignCurrencyDialog(
							getShell(),
							uncommittedEntryData);
					dialog.open();
				} else {
					for (Entry entry: uncommittedEntryData.getEntry().getTransaction().getEntryCollection()) {
						if (entry.getAmount() == 0) {
							throw new InvalidUserEntryException(
									"A non-zero credit or debit amount must be entered.",
									null);
						}
					}
				}
			} catch (InvalidUserEntryException e) {
		        MessageDialog dialog = new MessageDialog(
						getShell(), 
						"Incomplete or invalid data in entry", 
		        		null, // accept the default window icon
						e.getLocalizedMessage(),
						MessageDialog.ERROR, 
						new String[] { "Discard", IDialogConstants.CANCEL_LABEL }, 
						1);
		        int result = dialog.open();
		        if (result == 0) {
		        	// Discard
		        	
		        	// TODO: Some of this code is duplicated below.
		        	
					transactionManager = new TransactionManager(committedEntryData.getBaseSessionManager());
					Entry entryInTransaction;
					if (committedEntryData.getEntry() == null) {
						Transaction newTransaction = transactionManager.getSession().createTransaction();
						entryInTransaction = createNewEntry(newTransaction);
					} else {
						entryInTransaction = transactionManager.getCopyInTransaction(committedEntryData.getEntry());
					}
					
					// Update the controls.

					uncommittedEntryData = createUncommittedEntryData(entryInTransaction, transactionManager);
					uncommittedEntryData.setIndex(committedEntryData.getIndex());
					uncommittedEntryData.setBalance(committedEntryData.getBalance());

					for (final ICellControl<? super T> control: controls.values()) {
						control.load(uncommittedEntryData);
					}
					
		        	return true;
		        } else {
		        	// Cancel the selection change
		        	if (e.getItemWithError() != null) {
		        		e.getItemWithError().setFocus();
		        	}
					return false;
		        }
			}

			// Commit the changes to the transaction
			transactionManager.commit(transactionLabel);
			
			// Sound the tone
	        clip.play();
	 			
			/*
			 * It may be that this was a new entry not previously committed. If
			 * so, the committed entry in the EntryData object will be null. In
			 * this case we now clear out the controls so that it is ready for
			 * the next new transaction. (A new row will have been created for
			 * the new entry that we have just committed because the table is
			 * listening for new entries).
			 * 
			 * This listener should also have caused the balance for the new entry
			 * row to be updated.
			 */
			if (committedEntryData.getEntry() == null) {
				Transaction newTransaction = transactionManager.getSession().createTransaction();
				Entry entryInTransaction = createNewEntry(newTransaction);

				// Update the controls.
				
				uncommittedEntryData = createUncommittedEntryData(entryInTransaction, transactionManager);
				uncommittedEntryData.setIndex(committedEntryData.getIndex());
				uncommittedEntryData.setBalance(committedEntryData.getBalance());
				
				for (final ICellControl<? super T> control: controls.values()) {
					control.load(uncommittedEntryData);
				}
			}
		}

		return true;
	}

	/**
	 * Given an accounting transaction, which must be empty (no properties
	 * set or entries created), create entries and set properties as appropriate
	 * for a new entry.
	 * 
	 * Because the appropriate initialization of a new entry is dependent on what
	 * is being shown in the table, this initialization is passed on to the context
	 * provider.
	 * 
	 * @param newTransaction
	 * @return
	 */
	private Entry createNewEntry(Transaction newTransaction) {
		// TODO: Kludge here
		return ((EntriesTable)getParent().getParent().getParent()).entriesContent.createNewEntry(newTransaction);
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
		Transaction targetTransaction = uncommittedEntryData.getEntry().getTransaction(); 
		
		copyData(sourceEntryData.getEntry(), uncommittedEntryData.getEntry());

		Iterator<Entry> iter = uncommittedEntryData.getSplitEntries().iterator();
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
		targetEntry.setMemo(sourceEntry.getMemo());
		targetEntry.setIncomeExpenseCurrency(transactionManager.getCopyInTransaction(sourceEntry.getIncomeExpenseCurrency()));
		targetEntry.setAmount(sourceEntry.getAmount());
	}

	public void arrive(int currentColumn) {
		setSelected(true);
		getChildren()[currentColumn].setFocus();
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

	public Entry getUncommittedTopEntry() {
		return uncommittedEntryData.getEntry();
	}

	public T getUncommittedEntryData() {
		return uncommittedEntryData;
	}

	@Override
	protected void scrollToShowRow() {
		rowTable.scrollToShowRow(this);
	}

	/*
	 * Refreshes the balance and other properties of the row control that
	 * may be affected when other rows change.
	 */
	public void refreshBalance() {
		// The new balance will have been updated in the committed entry data object,
		// but not in the uncommitted entry data object.
		// As the balances are based on the committed data, this is a little funny.
		// This must be done before balanceChanged is fired below, because that
		// looks at the uncommitted entry data.
		uncommittedEntryData.setIndex(committedEntryData.getIndex());
		uncommittedEntryData.setBalance(committedEntryData.getBalance());
		
		setAppropriateBackgroundColor();
		
		for (IBalanceChangeListener listener: balanceChangeListeners) {
			listener.balanceChanged();
		}
	}
	
	private void setAppropriateBackgroundColor() {
		if (isSelected) {
			setBackground(selectedRowColor);
		} else {
			if (committedEntryData.getIndex()%2 == 0) {
				setBackground(alternateTransactionColor);
			} else {
				setBackground(transactionColor);
			}
		}
	}

	public void addBalanceChangeListener(IBalanceChangeListener listener) {
		balanceChangeListeners.add(listener);
	}
}

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
}

