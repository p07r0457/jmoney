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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntrySection extends SectionPart {

	protected EntriesPage fPage;
    protected Text fDescription; 

	public EntrySection(EntriesPage page, Composite parent) {
        super(parent, page.getManagedForm().getToolkit(), Section.DESCRIPTION | Section.TITLE_BAR);
        getSection().setText("Selected Entry");
        getSection().setDescription("Edit the currently selected entry.");
		createClient(page.getManagedForm().getToolkit());
	}

	protected void createClient(FormToolkit toolkit) {
        Composite container = toolkit.createComposite(getSection());

		GridLayout layout = new GridLayout();
		layout.numColumns = 10;
		container.setLayout(layout);
        
        toolkit.createLabel(container, "Check:");
        fDescription = toolkit.createText(container, "");
        toolkit.createLabel(container, "Date:");
        fDescription = toolkit.createText(container, "");
        toolkit.createLabel(container, "Description:");
        fDescription = toolkit.createText(container, "");
//        toolkit.createLabel(container, "Debit:");
//        fDescription = toolkit.createText(container, "");
//        toolkit.createLabel(container, "Credit:");
//        fDescription = toolkit.createText(container, "");
//        toolkit.createLabel(container, "");
//        toolkit.createLabel(container, "");
//        toolkit.createLabel(container, "Valuta:");
//        fDescription = toolkit.createText(container, "");
//        toolkit.createLabel(container, "Category:");
//        fDescription = toolkit.createText(container, "");
//        toolkit.createLabel(container, "Category:");
//        fDescription = toolkit.createText(container, "");
        
        getSection().setClient(container);
        toolkit.paintBordersFor(container);
        refresh();
    }

}
