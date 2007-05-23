/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.reconciliation.reconcilePage;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.ButtonCellControl;
import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.EntriesTable;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.HorizontalBlock;
import net.sf.jmoney.entrytable.ICellControl;
import net.sf.jmoney.entrytable.IEntriesContent;
import net.sf.jmoney.entrytable.PropertyBlock;
import net.sf.jmoney.entrytable.EntriesTable.IMenuItem;
import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.reconciliation.BankStatement;
import net.sf.jmoney.reconciliation.ReconciliationEntryInfo;
import net.sf.jmoney.reconciliation.ReconciliationPlugin;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Class implementing the section containing the unreconciled
 * entries on the account reconciliation page.
 * 
 * @author Nigel Westbury
 */
public class UnreconciledSection extends SectionPart {

	private ReconcilePage fPage;

	private EntriesTable fUnreconciledEntriesControl;

	private FormToolkit toolkit;

	private IEntriesContent unreconciledTableContents = null;

	private ArrayList<CellBlock> cellList;

	public UnreconciledSection(ReconcilePage page, Composite parent) {
		super(parent, page.getManagedForm().getToolkit(), Section.TITLE_BAR);
		getSection().setText("Unreconciled Entries");
		fPage = page;
		this.toolkit = page.getManagedForm().getToolkit();

		unreconciledTableContents = new IEntriesContent() {
			public Collection<Entry> getEntries() {
				/* The caller always sorts, so there is no point in us returning
				 * sorted results.  It may be at some point we decide it is more
				 * efficient to get the database to sort for us, but that would
				 * only help the first time the results are fetched, it would not
				 * help on a re-sort.  It also only helps if the database indexes
				 * on the date.		
				CurrencyAccount account = fPage.getAccount();
		        Collection<Entry> accountEntries = 
		        	account
						.getSortedEntries(TransactionInfo.getDateAccessor(), false);
				 */
				Collection<Entry> accountEntries = fPage.getAccount().getEntries();

				Vector<Entry> requiredEntries = new Vector<Entry>();
				for (Entry entry: accountEntries) {
					if (entry.getPropertyValue(ReconciliationEntryInfo.getStatementAccessor()) == null) {
						requiredEntries.add(entry);
					}
				}

				return requiredEntries;
			}

			public boolean isEntryInTable(Entry entry) {
				// This entry is to be shown if the account
				// matches and no statement is set.
				BankStatement statement = entry.getPropertyValue(ReconciliationEntryInfo.getStatementAccessor());
				return fPage.getAccount().equals(entry.getAccount())
				&& statement == null;
			}

			public boolean filterEntry(EntryData data) {
				// No filter here, so entries always match
				return true;
			}

			public long getStartBalance() {
				// TODO: figure out how we keep this up to date.
				// The EntriesTree class has no mechanism for refreshing
				// the opening balance.  It should have.
				return 0;
			}

			public void setNewEntryProperties(Entry newEntry) {
				newEntry.setAccount(fPage.getAccount());
			}
		};

		IMenuItem reconcileAction = new IMenuItem() {

			public String getText() {
				return "Reconcile";
			}

			public void run(Entry selectedEntry) {
				if (fPage.getStatement() != null) {
					// If the user double clicked on the blank new entry row, then
					// entry will be null.  We must guard against that.

					// The EntriesTree control will always validate and commit
					// any outstanding changes before firing a default selection
					// event.  We set the property to put the entry into the
					// statement and immediately commit the change.
					if (selectedEntry != null) {
						selectedEntry.setPropertyValue(ReconciliationEntryInfo.getStatementAccessor(), fPage.getStatement());
						fPage.transactionManager.commit("Reconcile Entry");
					}
				}
			}
		};

		// Load the 'reconcile' indicator
		URL installURL = ReconciliationPlugin.getDefault().getBundle().getEntry("/icons/reconcile.gif");
		final Image reconcileImage = ImageDescriptor.createFromURL(installURL).createImage();
		parent.addDisposeListener(new DisposeListener(){
			public void widgetDisposed(DisposeEvent e) {
				reconcileImage.dispose();
			}
		});
		
		CellBlock reconcileButton = new CellBlock("", 20, 0) {

			public int compare(EntryData trans1, EntryData trans2) {
				// TODO Sort this out.  We cannot sort on this.
				return 0;
			}

			public ICellControl createCellControl(Composite parent,
					Session session) {
				return new ButtonCellControl(parent, reconcileImage, "Reconcile this Entry to the above Statement") {

					@Override
					protected void run(EntryData data) {
						unreconcileEntry(data);
					}
				};
			}

			public String getId() {
				return "unreconcile";
			}
		};

		CellBlock transactionDateColumn = PropertyBlock.createTransactionColumn(TransactionInfo.getDateAccessor());

		/*
		 * Setup the layout structure of the header and rows.
		 */
		Block rootBlock = new HorizontalBlock(new Block [] {
				reconcileButton,
				transactionDateColumn,
				PropertyBlock.createEntryColumn(EntryInfo.getValutaAccessor()),
				PropertyBlock.createEntryColumn(EntryInfo.getCheckAccessor()),
				PropertyBlock.createEntryColumn(EntryInfo.getMemoAccessor()),
				fPage.debitColumnManager,
				fPage.creditColumnManager,
				fPage.balanceColumnManager,
		});

		cellList = new ArrayList<CellBlock>();
		rootBlock.buildCellList(cellList);

		// Create the table control.
		fUnreconciledEntriesControl = new EntriesTable(getSection(), toolkit, rootBlock, unreconciledTableContents, fPage.getAccount().getSession(), transactionDateColumn, new IMenuItem [] { reconcileAction } ); 

		// Allow entries in the account to be moved from the unreconciled list
		final DragSource dragSource = new DragSource(fUnreconciledEntriesControl.getControl(), DND.DROP_MOVE);

//		Provide data in Text format
		Transfer[] types = new Transfer[] {TextTransfer.getInstance()};
		dragSource.setTransfer(types);

		dragSource.addDragListener(new DragSourceListener() {
			public void dragStart(DragSourceEvent event) {
				/*
				 * It is not possible to start a drag without the current selection
				 * first being set to the dragged item.  We can therefore use the
				 * current selection as the dragged item.
				 */
				Entry entry = fUnreconciledEntriesControl.getSelectedEntry();

				// Do not start the drag if the empty 'new entry' row is being dragged.
				if (entry == null) {
					event.doit = false;
				}
			}
			public void dragSetData(DragSourceEvent event) {
				// Provide the data of the requested type.
				if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
					Entry entry = fUnreconciledEntriesControl.getSelectedEntry();
					fPage.entryBeingDragged = entry;
					event.data = "get entry from fPage";
					//event.data = entry;
				}
			}
			public void dragFinished(DragSourceEvent event) {
				if (event.detail == DND.DROP_MOVE) {
					/*
					 * Normally the dragged item would be removed at this point.
					 * However, the move is handled by the destination onto which
					 * the item is dropped.
					 */
				}
			}
		});

		fUnreconciledEntriesControl.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				dragSource.dispose();
			}
		});

		getSection().setClient(fUnreconciledEntriesControl);
		toolkit.paintBordersFor(fUnreconciledEntriesControl);
		refresh();
	}

	public void unreconcileEntry(EntryData data) {
		if (fPage.getStatement() != null) {
			Entry entry = data.getEntry();

			// If the user double clicked on the blank new entry row, then
			// entry will be null.  We must guard against that.

			// TODO: What do we do about the blank entry???
			
			// The EntriesTree control will always validate and commit
			// any outstanding changes before firing a default selection
			// event.  We set the property to put the entry into the
			// statement and immediately commit the change.
			if (entry != null) {
				entry.setPropertyValue(ReconciliationEntryInfo.getStatementAccessor(), fPage.getStatement());
				fPage.transactionManager.commit("Reconcile Entry");
			}
		} else {
			MessageDialog.openError(getSection().getShell(), "Action is Not Available", "You must select a statement first before you can reconcile an entry.  The entry will then reconcile to the statement in the upper table.");
		}
	}
}
