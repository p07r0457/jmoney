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

import net.sf.jmoney.views.NodeEditor;import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntriesPage extends FormPage {

    public static final String PAGE_ID = "entries";

	protected NodeEditor fEditor;
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
        ScrolledForm form = managedForm.getForm();
        GridLayout layout = new GridLayout();
        form.getBody().setLayout(layout);

        fEntriesFilterSection = new EntriesFilterSection(this, form.getBody());
        fEntriesFilterSection.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        managedForm.addPart(fEntriesFilterSection);

        fEntriesSection = new EntriesSection(this, form.getBody());
        fEntriesSection.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
        managedForm.addPart(fEntriesSection);

        fEntrySection = new EntrySection(this, form.getBody());
        fEntrySection.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        managedForm.addPart(fEntriesSection);

        form.setText("Accounting Entries");
    }

}