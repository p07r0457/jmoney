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
 *
 */
package net.sf.jmoney.ui.internal.pages.account.capital;

import java.util.Iterator;

import net.sf.jmoney.ui.internal.JMoneyUIPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
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
public class EntriesFilterSection extends SectionPart {

	protected EntriesPage fPage;
    protected Combo fFilterCombo;
    protected Combo fFilterTypeCombo;
    protected Combo fOperationCombo;
    protected Text fFilterText;

	public EntriesFilterSection(EntriesPage page, Composite parent) {
        super(parent, page.getManagedForm().getToolkit(), Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
        fPage = page;
        getSection().addExpansionListener(new ExpansionAdapter() {
    		public void expansionStateChanged(ExpansionEvent e) {
    			// TODO warn the user if the section is being collapsed
    			// while a filter is in effect.  Alternatively, we can
    			// show some other indication that the entries list
    			// is filtered.  If we don't do anything, the user may
    			// get confused because entries are not being shown.
    		}
    	});
    	getSection().setText("Entries Filter");
        getSection().setDescription("Show only entries in the table below that match your filter criteria.");
		createClient(page.getManagedForm().getToolkit());
	}

	protected void createClient(FormToolkit toolkit) {
        Composite container = toolkit.createComposite(getSection());

        GridLayout layout = new GridLayout();
        layout.numColumns = 4;
        container.setLayout(layout);

        fFilterCombo = new Combo(container, toolkit.getBorderStyle() | SWT.READ_ONLY);
        toolkit.adapt(fFilterCombo, true, true);
        String[] fFilterComboItems = { JMoneyUIPlugin.getResourceString("EntriesFilterSection.filter"),
                JMoneyUIPlugin.getResourceString("EntriesFilterSection.clear")};
        fFilterCombo.setItems(fFilterComboItems);
        fFilterCombo.select(0);

        // Build an array of the localized names of the properties
        // on which a filter may be based.
        String[] filterTypes = new String[fPage.allEntryDataObjects.size() + 1];
        int i = 0;
        filterTypes[i++] = JMoneyUIPlugin.getResourceString("EntryFilter.entry"); 
        for (Iterator iter = fPage.allEntryDataObjects.iterator(); iter.hasNext(); ) {
        	IEntriesTableProperty entriesSectionProperty = (IEntriesTableProperty)iter.next();
            filterTypes[i++] = entriesSectionProperty.getText();
        }
        
        fFilterTypeCombo = new Combo(container, toolkit.getBorderStyle() | SWT.READ_ONLY);
        toolkit.adapt(fFilterTypeCombo, true, true);
        fFilterTypeCombo.setItems(filterTypes);
        fFilterTypeCombo.select(0);
        
        fOperationCombo = new Combo(container, toolkit.getBorderStyle() | SWT.READ_ONLY);
        toolkit.adapt(fOperationCombo, true, true);
        String[] fOperationComboItems = { JMoneyUIPlugin.getResourceString("EntriesFilterSection.contains"),};
        fOperationCombo.setItems(fOperationComboItems);
        fOperationCombo.select(0);

        fFilterText = toolkit.createText(container, "");
        fFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        getSection().setClient(container);
        toolkit.paintBordersFor(container);
        refresh();
    }

}
