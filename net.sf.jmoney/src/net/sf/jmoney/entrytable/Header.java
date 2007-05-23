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

import java.util.Comparator;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * by extending AbstractNativeHeader, we get a native header control which
 * allows the user to resize the columns by dragging and also allows the columns
 * to be sorted.
 * 
 * @author Administrator
 *
 */
public class Header extends Composite {

	private EntriesTable entriesTable;

	private Vector<IEntriesTableProperty> properties = new Vector<IEntriesTableProperty>();

	public Header(Composite parent, int style, EntriesTable entriesTable) {
		super(parent, style);

		this.entriesTable = entriesTable;

		BlockLayout layout = new BlockLayout(entriesTable.rootBlock);
		setLayout(layout);

		setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		
		for (IEntriesTableProperty entriesSectionProperty: entriesTable.getCellList()) {
				Label label = new Label(this, SWT.NULL);
				label.setText(entriesSectionProperty.getText());
				label.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));

				label.setMenu(buildPopupMenu(entriesSectionProperty));
		}
	}
	
	protected boolean sortOnColumn(int column, int sortDirection) {
		IEntriesTableProperty sortProperty = properties.get(column);
		entriesTable.sort(sortProperty, sortDirection == SWT.UP);

		// TODO: Is there a better way of getting the table?
		entriesTable.table.refreshContent();
        
        return true;
	}
	
	public void setEntriesTable(EntriesTable entriesTable) {
		this.entriesTable = entriesTable;

		/*		
*/		
	}

	private class RowComparator implements Comparator<EntryData> {
		private IEntriesTableProperty sortProperty;
		private boolean ascending;
		
		RowComparator(IEntriesTableProperty sortProperty, boolean ascending) {
			this.sortProperty = sortProperty;
			this.ascending = ascending;
		}
		
		public int compare(EntryData entryData1, EntryData entryData2) {
			int result = sortProperty.compare(entryData1, entryData2);
			return ascending ? result : -result;
		}
	}
	
	private Menu buildPopupMenu(IEntriesTableProperty entriesSectionProperty) {
		// Bring up a pop-up menu.
		// It would be a more consistent interface if this menu were
		// linked to the column header.  However, TableColumn has
		// no setMenu method, nor does the column header respond to
		// SWT.MenuDetect nor any other event when right clicked.
		// This code works but does not follow the popup-menu conventions
		// on even one platform!

		Menu popupMenu = new Menu(getShell(), SWT.POP_UP);

		MenuItem removeColItem = new MenuItem(popupMenu, SWT.NONE);

		MenuItem shiftColLeftItem = new MenuItem(popupMenu,
				SWT.NONE);

		MenuItem shiftColRightItem = new MenuItem(popupMenu,
				SWT.NONE);

		Object[] messageArgs = new Object[] { entriesSectionProperty.getText() };

		removeColItem.setText(new java.text.MessageFormat(
				"Remove {0} column", java.util.Locale.US)
				.format(messageArgs));
		shiftColLeftItem.setText(new java.text.MessageFormat(
				"Move {0} column left", java.util.Locale.US)
				.format(messageArgs));
		shiftColRightItem.setText(new java.text.MessageFormat(
				"Move {0} column right", java.util.Locale.US)
				.format(messageArgs));

		removeColItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// TODO:
			}
		});

		shiftColLeftItem
				.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						// TODO: shift left if we can
					}
				});

		shiftColRightItem
				.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						// TODO: shift right if we can
					}
				});
/* TODO: complete implementation of this.
 * We need to allow the user to add columns?????
 
		new MenuItem(popupMenu, SWT.SEPARATOR);
		
		for (final IEntriesTableProperty entriesSectionProperty: entriesContent.getAllEntryDataObjects()) {
			boolean found = false;
			for (int index = 0; index < fTable.getColumnCount(); index++) {
				IEntriesTableProperty entryData2 = (IEntriesTableProperty) (fTable
						.getColumn(index).getData());
				if (entryData2 == entriesSectionProperty) {
					found = true;
					break;
				}
			}

			if (!found) {
				Object[] messageArgs2 = new Object[] { entriesSectionProperty
						.getText() };

				MenuItem addColItem = new MenuItem(popupMenu,
						SWT.NONE);
				addColItem.setText(new java.text.MessageFormat(
						"Add {0} column", java.util.Locale.US)
						.format(messageArgs2));

				addColItem
						.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(
									SelectionEvent e) {
								addColumn(entriesSectionProperty,
										Math.max(1, column));
							}
						});
			}
		}
*/
//		popupMenu.setVisible(true);
		return popupMenu;
	}
}
