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

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Table;
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

	private Table entryListTable;
    private TableViewer entryListViewer;

    private String entryOrderFieldName;
    private String entryOrder;

    public AccountBalancesPage() {
    	// TODO remove this
    }
	public void init(IMemento memento) {
		if (memento != null) {
			// Set the sort column, if any.
			entryOrderFieldName = memento.getString("entryOrderField");
			if (entryOrderFieldName == null) {
				entryOrderFieldName = "transaction.date";
			}
			entryOrder = memento.getString("entryOrder");
			if (entryOrder == null) {
				entryOrder = "Ascending";
			}
		} else {
			entryOrderFieldName = "transaction.date";
			entryOrder = "Ascending";
		}
        
/*        
        // Set the grid position of all columns in the grid.
        String customLayoutProperty = properties.getProperty("AccountEntriesPanel.customLayouts", "no");
        if (customLayoutProperty.equals("yes")) {
            // Read the layouts for the simple and extended layouts.
        } else {
            // Set default layouts.
        }
*/        
	}

	public void saveState(IMemento memento) {
        memento.putString("entryOrderField", entryOrderFieldName);
        memento.putString("entryOrder", entryOrder);
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
			Composite topLevelControl = new Composite(parent, SWT.NULL);

			GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			topLevelControl.setLayout(layout);

			// Set up the Swing table.
			Composite swingComposite = new Composite(topLevelControl, SWT.EMBEDDED);
//			Composite swingComposite = new Composite(parent, 0);
			java.awt.Frame accountEntriesFrame = SWT_AWT.new_Frame(swingComposite);
			java.awt.Panel panel = new java.awt.Panel(new java.awt.BorderLayout());
			accountEntriesFrame.add(panel);
			final AccountBalancesReportPanel reportPanel = new  AccountBalancesReportPanel();
			
			// TODO these following two lines and the methods called are not
			// neccessary because the report object can get the values itself.
			reportPanel.setSession(JMoneyPlugin.getDefault().getSession());
			reportPanel.setDateFormat(JMoneyPlugin.getDefault().getDateFormat());

			// This line has no effect!
			swingComposite.setSize(300, 400);

//			JScrollPane scrollPane = new JScrollPane(accountEntriesPanel);
//			panel.add(scrollPane);
			panel.add(reportPanel);

			GridData gridData = new GridData();
			gridData.horizontalAlignment = GridData.FILL;
			gridData.verticalAlignment = GridData.FILL;
			gridData.grabExcessHorizontalSpace = true;
			gridData.grabExcessVerticalSpace = true;
			swingComposite.setLayoutData(gridData);
			   
/*			
			Font font = parent.getFont();
			Label label = new Label(topLevelControl, SWT.NONE);
			label.setText("bank name");
			label.setFont(font);
*/			
			List sampleCheckButton2 = new List(topLevelControl, SWT.CHECK);
			sampleCheckButton2.add("t");
			sampleCheckButton2.add("h");
			sampleCheckButton2.add("i");
			sampleCheckButton2.add("s");
			sampleCheckButton2.add(" ");
			sampleCheckButton2.add("c");
			sampleCheckButton2.add("o");
			sampleCheckButton2.add("n");
			sampleCheckButton2.add("t");
			sampleCheckButton2.add("r");
			sampleCheckButton2.add("o");
			sampleCheckButton2.add("l");
			sampleCheckButton2.add(" ");
			sampleCheckButton2.add("f");
			sampleCheckButton2.add("o");
			sampleCheckButton2.add("r");
			sampleCheckButton2.add("c");
			sampleCheckButton2.add("e");
			sampleCheckButton2.add("s");
			sampleCheckButton2.add(" ");
			sampleCheckButton2.add("t");
			sampleCheckButton2.add("h");
			sampleCheckButton2.add("e");
			sampleCheckButton2.add(" ");
			sampleCheckButton2.add("h");
			sampleCheckButton2.add("e");
			sampleCheckButton2.add("i");
			sampleCheckButton2.add("g");
			sampleCheckButton2.add("h");
			sampleCheckButton2.add("t");
			sampleCheckButton2.add(".");
			
			Button sampleCheckButton = new Button(topLevelControl, SWT.CHECK);
			sampleCheckButton.setText("This control forces up the width of this column in the grid.  Without it, the Swing control would be very thin.");
//			sampleCheckButton.setFont(font);
			
			topLevelControl.pack();
			
			return new BookkeepingPage[] 
			{ new BookkeepingPage(topLevelControl, "Entry List") };
//			{ new BookkeepingPage(swingComposite, "Entry List") };
	}
}
