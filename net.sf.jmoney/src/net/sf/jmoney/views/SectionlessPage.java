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
package net.sf.jmoney.views;

import net.sf.jmoney.IBookkeepingPageListener;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class SectionlessPage extends FormPage {

    public static final String PAGE_ID = "old_style_page";

	protected NodeEditor fEditor;
    protected IBookkeepingPageListener pageListener;
	protected OldStyleWrapperFormPart formPart;

    /**
     * Create a new page to edit entries.
     * 
     * @param editor Parent editor
     */
    public SectionlessPage(NodeEditor editor, IBookkeepingPageListener pageListener, String pageName) {
        super(editor, pageListener.getClass().getName(), pageName);
        fEditor = editor;
        this.pageListener = pageListener;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.forms.editor.FormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
     */
    protected void createFormContent(IManagedForm managedForm) {
        ScrolledForm form = managedForm.getForm();
        GridLayout layout = new GridLayout();
        form.getBody().setLayout(layout);

        formPart = new OldStyleWrapperFormPart(this, form.getBody(), pageListener);

//        fPropertiesSection.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//        form.getBody().setLayoutData(new GridData(GridData.FILL_BOTH));

        managedForm.addPart(formPart);

        if (pageListener.getClass().getName().equals("net.sf.jmoney.bookkeepingPages.EntryListPage")) {
            form.setText("Account Entries");
        } else if (pageListener.getClass().getName().equals("net.sf.jmoney.bookkeepingPages.AccountPropertiesPages")) {
            form.setText("Account Properties");
        } else {
        	form.setText("Form for Old Style Page Extension");
        }
    }

	/**
	 * @return
	 */
	public Object getSelectedObject() {
		return fEditor.getSelectedObject();
	}
}
