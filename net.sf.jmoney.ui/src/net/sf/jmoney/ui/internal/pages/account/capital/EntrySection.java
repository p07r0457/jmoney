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

import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
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
    
    protected Entry currentEntry = null;
    
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
    	currentEntry = entry;

        for (Iterator iter = fPage.allEntryDataObjects.iterator(); iter.hasNext();) {
        	EntriesSectionProperty entriesSectionProperty = (EntriesSectionProperty)iter.next();
        	ExtendableObject object = entriesSectionProperty.getObjectContainingProperty(entry);
            IPropertyControl control = entriesSectionProperty.getControl();

            // If the object is null this mean the property is not applicable.
            // An example of when this might happen:
            // If the the transaction contains more than two entries
            // (a split transaction) then the properties that are taken
            // from the other entry (currently account and description)
            // are not meaningful and the controls should be disabled.
            // Setting a null object in the controls will blank and
            // disable the control.
            
            // If the property was not editable then no control will exist.
            if (control != null) {
            	control.load(object);
            }
        }
    }

    protected void createClient(FormToolkit toolkit) {
        Composite container = toolkit.createComposite(getSection());

        GridLayout layout = new GridLayout();
        layout.numColumns = 10;
        container.setLayout(layout);

        // When the user selects an entry, property values are displayed that
        // the user may edit.  These properties do not all come from the selected
        // entry.  Some properties come from the other entry in a double entry,
        // and some properties come from the transaction.
        
        final Session session = fPage.getAccount().getSession();

        // Create an edit control for each property.
        for (Iterator iter = fPage.allEntryDataObjects.iterator(); iter.hasNext();) {
        	final EntriesSectionProperty entriesSectionProperty = (EntriesSectionProperty)iter.next(); 
            final PropertyAccessor propertyAccessor = entriesSectionProperty.getPropertyAccessor();

            if (propertyAccessor.isEditable()) {
                Label propertyLabel = new Label(container, 0);
                propertyLabel.setText(propertyAccessor.getShortDescription() + ':');
                final IPropertyControl propertyControl = propertyAccessor.createPropertyControl(container);
        		propertyControl.load(null);
                toolkit.adapt(propertyLabel, false, false);
                toolkit.adapt(propertyControl.getControl(), true, true);
                entriesSectionProperty.setControl(propertyControl);
				propertyControl.getControl().addFocusListener(
						new FocusAdapter() {

							// When a control gets the focus, save the old value here.
							// This value is used in the change message.
							String oldValueText;
							
							public void focusLost(FocusEvent e) {
								System.out.println("Focus lost: " + propertyAccessor.getLocalName());
								
								if (session.isSessionFiring()) {
									return;
								}
								
								propertyControl.save();
								String newValueText = propertyAccessor.formatValueForMessage(
										entriesSectionProperty.getObjectContainingProperty(currentEntry));
								
								String description = 
										"change " + propertyAccessor.getShortDescription() + " property"
										+ " from " + oldValueText
										+ " to " + newValueText;
								
								session.registerUndoableChange(description);
							}
							public void focusGained(FocusEvent e) {
								System.out.println("Focus gained: " + propertyAccessor.getLocalName());
								// Save the old value of this property for use in our 'undo' message.
								oldValueText = propertyAccessor.formatValueForMessage(
										entriesSectionProperty.getObjectContainingProperty(currentEntry));
							}
						});
            }
        }

        	
        getSection().setClient(container);
        toolkit.paintBordersFor(container);
        refresh();
    }

}