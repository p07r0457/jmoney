/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

package net.sf.jmoney.reports;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.ui.IMemento;

import net.sf.jmoney.IBookkeepingPageListener;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Session;

/**
 * @author Nigel
 *
 * As each folder view will load its own instances of the extension classes,
 * and each folder view will only display the tab items for a single
 * object at any point of time, this class can cache the tab items
 * and re-use them for each selected object.
 */
public class AccountBalancesPage implements IBookkeepingPageListener {

	public void init(IMemento memento) {
		// No view state to restore
	}

	public void saveState(IMemento memento) {
		// No view state to save
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#getPageCount(java.lang.Object)
	 */
	public int getPageCount(Object selectedObject) {
		return 1;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public BookkeepingPage[] createPages(Object selectedObject, Session session, Composite parent) {
			// Set up the Swing table.
			Composite swingComposite = new Composite(parent, SWT.EMBEDDED);

			java.awt.Frame accountEntriesFrame = SWT_AWT.new_Frame(swingComposite);
			java.awt.Panel panel = new java.awt.Panel(new java.awt.BorderLayout());
			accountEntriesFrame.add(panel);
			final AccountBalancesReportPanel reportPanel = new  AccountBalancesReportPanel();
			
			// TODO these following two lines and the methods called are not
			// neccessary because the report object can get the values itself.
			reportPanel.setSession(JMoneyPlugin.getDefault().getSession());
			reportPanel.setDateFormat(JMoneyPlugin.getDefault().getDateFormat());

			panel.add(reportPanel);

			return new BookkeepingPage[] 
			{ new BookkeepingPage(swingComposite, "Entry List") };
	}
}
