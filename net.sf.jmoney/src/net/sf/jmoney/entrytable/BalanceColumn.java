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

import net.sf.jmoney.model2.Commodity;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class BalanceColumn extends IndividualBlock<EntryData> {

	private Commodity commodityForFormatting;

	public BalanceColumn(Commodity commodityForFormatting) {
		super("Balance", 70, 2);
		this.commodityForFormatting = commodityForFormatting;
	}
	
	public int compare(EntryData entryData1, EntryData entryData2) {
		// Entries lists cannot be sorted based on the balance.
		// The caller should not do this.
		throw new RuntimeException("internal error - attempt to sort on balance");
	}

	public ICellControl<EntryData> createCellControl(Composite parent) {
		final Label balanceLabel = new Label(parent, SWT.TRAIL);
		
		return new ICellControl<EntryData>() {

			public Control getControl() {
				return balanceLabel;
			}

			public void load(EntryData data) {
				balanceLabel.setText(commodityForFormatting.format(data.getBalance()));
			}

			public void save() {
				// Not editable so nothing to do
			}

			public void setFocusListener(FocusListener controlFocusListener) {
				// Nothing to do
			}
		};
	}

	public String getId() {
		return "balance"; //$NON-NLS-1$
	}
};

