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
import java.util.Vector;

import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
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
    protected Vector fPropertyControls = new Vector(); 

    public EntrySection(EntriesPage page, Composite parent) {
        super(parent, page.getManagedForm().getToolkit(), Section.DESCRIPTION | Section.TITLE_BAR);
        fPage = page;
        getSection().setText("Selected Entry");
        getSection().setDescription("Edit the currently selected entry.");
        createClient(page.getManagedForm().getToolkit());
    }

    /**
     * Load the values from the given entry into the property controls.
     *
     * @param entry Entry whose editable properties are presented to the user
     */
    public void update(Entry entry) {
        for (Iterator iter = fPropertyControls.iterator(); iter.hasNext();) {
            IPropertyControl control = (IPropertyControl) iter.next();
            control.load(entry);
        }
    }

    protected void createClient(FormToolkit toolkit) {
        Composite container = toolkit.createComposite(getSection());

        GridLayout layout = new GridLayout();
        layout.numColumns = 10;
        container.setLayout(layout);

        PropertySet extendablePropertySet = PropertySet.getPropertySet(Entry.class);
        for (Iterator iter = extendablePropertySet.getPropertyIterator3(); iter.hasNext();) {
            PropertyAccessor propertyAccessor = (PropertyAccessor) iter.next();
            if (propertyAccessor.isScalar() && propertyAccessor.isEditable()) {
                Label propertyLabel = new Label(container, 0);
                propertyLabel.setText(propertyAccessor.getShortDescription() + ':');
                IPropertyControl propertyControl = propertyAccessor.createPropertyControl(container);
                toolkit.adapt(propertyLabel, false, false);
                toolkit.adapt(propertyControl.getControl(), true, true);
                fPropertyControls.add(propertyControl);
            }

        }

        getSection().setClient(container);
        toolkit.paintBordersFor(container);
        refresh();
    }

}