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

import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.views.NodeEditor;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntriesPage extends FormPage implements IBookkeepingPage {

    public static final String PAGE_ID = "entries";

	protected NodeEditor fEditor;

	/** Element: EntriesSectionProperty */
	protected Vector allEntryDataObjects = new Vector();

    protected EntriesFilterSection fEntriesFilterSection;
    protected EntriesSection fEntriesSection;
	protected EntrySection fEntrySection;

    /**
     * Create a new page to edit entries.
     * 
     * @param editor Parent editor
     */
    public EntriesPage(NodeEditor editor) {
        super(editor, PAGE_ID, "Entries");
        fEditor = editor;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.forms.editor.FormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
     */
    protected void createFormContent(IManagedForm managedForm) {
    	// Build an array of all possible properties that may be
    	// displayed in the table.
        
        // Add properties from the transaction.
        for (Iterator iter = TransactionInfo.getPropertySet().getPropertyIterator3(); iter.hasNext();) {
            final PropertyAccessor propertyAccessor = (PropertyAccessor) iter.next();
            if (propertyAccessor.isScalar()) {
            	allEntryDataObjects.add(new EntriesSectionProperty(propertyAccessor) {
					ExtendableObject getObjectContainingProperty(Entry entry) {
						return entry.getTransaction();
					}
            	});
            }
        }

        // Add properties from this entry.
        // For time being, this is all the properties except the account and description,
        // which come from the other entry.
        for (Iterator iter = EntryInfo.getPropertySet().getPropertyIterator3(); iter.hasNext();) {
            PropertyAccessor propertyAccessor = (PropertyAccessor) iter.next();
            if (propertyAccessor != EntryInfo.getAccountAccessor() && propertyAccessor != EntryInfo.getDescriptionAccessor()) {
            	if (propertyAccessor.isScalar() && propertyAccessor.isEditable()) {
            		allEntryDataObjects.add(new EntriesSectionProperty(propertyAccessor) {
    					ExtendableObject getObjectContainingProperty(Entry entry) {
    						return entry;
    					}
                	});
            	}
            }
        }

        // Add properties from the other entry.
        // For time being, this is just the account and description.
        PropertySet extendablePropertySet = EntryInfo.getPropertySet();
        for (Iterator iter = extendablePropertySet.getPropertyIterator3(); iter.hasNext();) {
            PropertyAccessor propertyAccessor = (PropertyAccessor) iter.next();
            if (propertyAccessor == EntryInfo.getAccountAccessor() || propertyAccessor == EntryInfo.getDescriptionAccessor()) {
            	allEntryDataObjects.add(new EntriesSectionProperty(propertyAccessor) {
					ExtendableObject getObjectContainingProperty(Entry entry) {
						if (entry.getTransaction().hasMoreThanTwoEntries()) {
							return null;
						} else {
							return entry.getTransaction().getOther(entry);
						}
					}
            	});
            }
        }

    	
    	
    	ScrolledForm form = managedForm.getForm();
        GridLayout layout = new GridLayout();
        form.getBody().setLayout(layout);

        fEntriesFilterSection = new EntriesFilterSection(this, form.getBody());
        fEntriesFilterSection.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        managedForm.addPart(fEntriesFilterSection);
        fEntriesFilterSection.initialize(managedForm);
        
        fEntriesSection = new EntriesSection(this, form.getBody());
        fEntriesSection.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
        managedForm.addPart(fEntriesSection);
        fEntriesSection.initialize(managedForm);

        fEntrySection = new EntrySection(this, form.getBody());
        fEntrySection.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        managedForm.addPart(fEntriesSection);
        fEntrySection.initialize(managedForm);

        form.setText("Accounting Entries");
    }
    
    public CurrencyAccount getAccount () {
        return (CurrencyAccount) fEditor.getSelectedObject();
    }

	public void saveState(IMemento memento) {
		// Save view state (e.g. the sort order, the set of extension properties that are
		// displayed in the table).
	}

}