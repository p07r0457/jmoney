/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Johann Gyger <jgyger@users.sf.net>
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
 */

package net.sf.jmoney.pages.entries;

import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntrySection extends SectionPart {
	
	private static final Color yellow = new Color(Display.getCurrent(), 255, 255, 200);
	private static final Color green  = new Color(Display.getCurrent(), 225, 255, 225);
	
	private Composite transactionArea;
	
	private Composite entriesArea;
	
	/** Controls for the selected entry (the first entry listed
	 *  in the transaction).
	 */
	private EntryControls selectedEntryControls;
	
	/** Controls for the other entry if the row represents a
	 * simple transaction.  (Two entries in a transaction, both
	 * merged into a single row in the table).
	 */
	private EntryControls otherEntryControls;
	
	private Vector<IPropertyControl> transactionControls = new Vector<IPropertyControl>();
	
	private Composite container;
	private Composite filler = null;
	
	public EntrySection(Composite parent, FormToolkit toolkit, Session session, Currency defaultCurrencyForPage) {
		super(parent, toolkit, 
				Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE 
				| (JMoneyPlugin.getDefault().getPreferenceStore().getBoolean("expandEditEntrySection")
						? Section.EXPANDED : 0));
		
		getSection().addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				JMoneyPlugin.getDefault().getPreferenceStore().setValue("expandEditEntrySection", e.getState());
			}
		});
		getSection().setText("Selected Entry");
		
		container = toolkit.createComposite(getSection());
		
		GridLayout sectionLayout = new GridLayout();
		sectionLayout.numColumns = 1;
		sectionLayout.marginHeight = 0;
		sectionLayout.marginWidth = 0;
		container.setLayout(sectionLayout);
		
		// Create the transaction property area
		// The area is initially set to a zero size so it is not visible.
		// It will be set to the appropriate size when an entry is selected.
		transactionArea = toolkit.createComposite(container);
		transactionArea.setLayoutData(new GridData(0, 0));
		
		RowLayout layout1 = new RowLayout(SWT.HORIZONTAL);
		transactionArea.setLayout(layout1);
		
		// Add properties from the transaction.
   		for (ScalarPropertyAccessor propertyAccessor: TransactionInfo.getPropertySet().getScalarProperties3()) {
			IPropertyControl propertyControl = propertyAccessor.createPropertyControl(transactionArea, session);
			propertyControl.load(null);
			toolkit.adapt(propertyControl.getControl(), true, true);
			transactionControls.add(propertyControl);
		}
		
		// Create the entries area
		entriesArea = toolkit.createComposite(container);
		entriesArea.setLayoutData(new GridData(0, 0));
		
		GridLayout entriesAreaLayout = new GridLayout();
		entriesAreaLayout.numColumns = 5;
		entriesAreaLayout.horizontalSpacing = 0;  // Ensures no uncolored gaps between items in same row
		entriesAreaLayout.verticalSpacing = 0;  // Ensures no uncolored gaps between items in same column
		entriesAreaLayout.marginWidth = 0;
		entriesArea.setLayout(entriesAreaLayout);
		
		// This must be on the container (if on the entriesArea, recursion occurs)
		container.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				layoutSection();
			}
		});
		
		// Create the row of controls for the current entry
		// (The entry from the transaction that was shown and
		// selected in the account entries list).
		selectedEntryControls = new EntryControls(session, entriesArea, null, yellow, defaultCurrencyForPage);
		otherEntryControls = new EntryControls(session, entriesArea, null, green, defaultCurrencyForPage);
		
		// The filler is used to set the entry section to a fixed size.
		// When we want to display some data in the section, the filler is destroyed
		// and the appropriate controls are set to the required non-zero sizes.
		// By keeping the sections a constant size, we avoid reflowing the sections
		// which does not look good.  
		
		// A size of 130 is sufficient on the Windows platform for one line of
		// transaction properties and two entries each with two rows of properties.
		// TODO: Find a better way of setting the fixed size for the section.
		filler = toolkit.createComposite(container);
		filler.setLayoutData(new GridData(SWT.DEFAULT, 130));
		getSection().setClient(container);
		toolkit.paintBordersFor(container);
		refresh();
	}
	
	/**
	 * Load the values from the given entry into the property controls.
	 *
	 * @param entry Entry whose editable properties are presented to the user
	 */
	public void update(Entry entry1, Entry entry2, boolean showTransactionProps) {
		if (filler != null) {
			filler.dispose();
			filler = null;
		}

		if (entry1 == null && entry2 == null) {
			entriesArea.setLayoutData(new GridData(0, 0));
			getSection().setDescription("No entry is currently selected.  Select an entry to edit, or create a new transaction.");
		} else {
			
			GridData entriesAreaLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
			entriesAreaLayoutData.widthHint = 200;
			entriesArea.setLayoutData(entriesAreaLayoutData);
			
			selectedEntryControls.setEntry(entry1);
			
			if (showTransactionProps) {
				getSection().setDescription("Edit the currently selected entry.");
				
				// Update transaction property controls.
				transactionArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				for (Iterator iter = transactionControls.iterator(); iter.hasNext();) {
					IPropertyControl control = (IPropertyControl)iter.next();
					control.load(entry1.getTransaction());
				}
			} else {
				getSection().setDescription("Edit the currently selected split.");
				
				// Hide the transaction property controls.
				transactionArea.setLayoutData(new GridData(0, 0));
			}
			
			/*
			 If a second entry is passed (entry2 is non-null) then the
			 user has selected a row that contains both entries of a simple
			 transaction merged onto a single row.
			 We must show both entries.
			 */
			if (entry2 != null) {
				otherEntryControls.setEntry(entry2);
				otherEntryControls.setVisible(true);
			} else {
				otherEntryControls.setVisible(false);
			}
		}
		
		layoutSection();
	}
	
	
	/**
	 * Layout the entry section.  This is not done correctly by the grid and
	 * row layouts.  The problem is as follows:  The entriesArea grid gets
	 * the preferred sizes from each of the child controls.  The composite1
	 * controls (each of row layout) return the fixed preferred width and then calculates the height
	 * required to contain the child controls.  The grid layout that allocates
	 * any excess width to the column containing the composite1 controls.  The
	 * controls in the composite1 composites are then re-flowed by the row layout.
	 * However, the height is not reduced, resulting in the controls being too high
	 * and a lot of empty space at the bottom of each row.
	 * <P>
	 * The solution is to first layout the grid with a small size set for the
	 * preferred width of the composite1 controls.  Then the actual width allocated
	 * by the grid layout is set as the preferred width, then the grid is layed out
	 * again.  This results in the correct heights.
	 */
	private void layoutSection() {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd1.widthHint = 100;
		selectedEntryControls.composite1.setLayoutData(gd1);
		
		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd2.widthHint = 100;
		otherEntryControls.composite1.setLayoutData(gd2);
		
		container.layout();
		
		otherEntryControls.composite1.setLayoutData(new GridData(otherEntryControls.composite1.getSize().x, SWT.DEFAULT));
		selectedEntryControls.composite1.setLayoutData(new GridData(selectedEntryControls.composite1.getSize().x, SWT.DEFAULT));
		
		entriesArea.pack(true);
	}
	
	
}