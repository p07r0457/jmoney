/*
*
*  JMoney - A Personal Finance Manager
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

package net.sf.jmoney.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Provides an implementation of FormPage suitable for pages
 * that do not use the sections.
 * <P>
 * The net.sf.jmoney.pages extension point require extensions
 * to provide an implementation of the IBookkeepingPage interface.
 * This interface has a method called createFormPage.  This method
 * must be implemented and must return an object that implements IFormPage.
 * However, there is a significant amount of code involved in creating an 
 * IFormPage implementation that is the same regardless of the page contents.
 * This class provides the common code.
 * <P>
 * This class requires an implementation of the createControl method.
 * This method is called to create the control that forms the body of
 * the page.  This control should contain the controls that are
 * specific to the page.
 * <P>
 * This class does not support sections.  If you wish to create
 * a page using sections then you cannot use this class.
 *
 * @author Nigel Westbury
 */
public abstract class SectionlessPage extends FormPage {
	protected NodeEditor fEditor;
	protected String formHeader;
	protected IFormPart formPart;

	private class GenericFormPart extends AbstractFormPart {
		protected SectionlessPage page;
	    protected Composite parent; 

	    public GenericFormPart(SectionlessPage page, Composite parent) {
	        this.page = page;
	        this.parent = parent;

	        FormToolkit toolkit = page.getManagedForm().getToolkit();

		Composite propertiesControl = createControl(page.getSelectedObject(), parent);
			
	        // TODO: Do this.........
//toolkit.paintBordersFor(propertiesControl);
	        
	        // Modified by Faucheux
	        // The one and only visible control in the parent
	        // should fill the entire space.
	        final Control control = propertiesControl;
	        control.setBackground(control.getDisplay().getSystemColor(
	                SWT.COLOR_DARK_BLUE));
	        control.pack(true);

	        // Force the parent (DARK_BLUE) to be as big as its container
	        control.addControlListener(new ControlListener() {
	            public void controlMoved(ControlEvent e) {  }
	            public void controlResized(ControlEvent e) {
	                Composite parent = (Composite) e.getSource();
	                System.out.println("Redraw dark blue " + parent);
	                System.out.println("  was " + parent.getSize());
//	              parent.setSize(parent.getParent().getSize());
	                parent.setSize(parent.getParent().getSize().x * 95 / 100, parent.getParent().getSize().y * 90 / 100);
	                System.out.println("  is  " + parent.getSize());
	            }
	            
	        });

	        parent.setBackground(control.getDisplay().getSystemColor(
	                SWT.COLOR_YELLOW));
	        parent.pack(true);
	        parent.addControlListener(new ControlListener() {
	            public void controlMoved(ControlEvent e) {  }
	            public void controlResized(ControlEvent e) {
	                Composite parent = (Composite) e.getSource();
	                System.out.println("Redraw yellow " + parent);
	                System.out.println("  was " + parent.getSize());
//	                parent.setSize(parent.getParent().getSize());
	                
//	                control.setSize(parent.getSize().x * 95 / 100, parent.getSize().x * 90 / 100);
	                control.setSize(parent.getSize());
	                System.out.println("  is  " + parent.getSize());
	            }
	        });
	        
	        refresh();
	    }

	    /* (non-Javadoc)
	     * @see org.eclipse.ui.forms.IFormPart#refresh()
	     */
	    public void refresh() {
	// Not sure what we do here???????
	        super.refresh();
	    }
	}

	public SectionlessPage(NodeEditor editor, String id, String title, String formHeader) {
        super(editor, id, title);
        fEditor = editor;
        this.formHeader = formHeader;
	}

    /* (non-Javadoc)
     * @see org.eclipse.ui.forms.editor.FormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
     */
    protected void createFormContent(IManagedForm managedForm) {
        ScrolledForm form = managedForm.getForm();
        GridLayout layout = new GridLayout();
        form.getBody().setLayout(layout);

        formPart = new GenericFormPart(this, form.getBody());

//        fPropertiesSection.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//        form.getBody().setLayoutData(new GridData(GridData.FILL_BOTH));

        managedForm.addPart(formPart);

       	form.setText(formHeader);
    }

    /**
	 * @return
	 */
	public Object getSelectedObject() {
		return fEditor.getSelectedObject();
	}
	/**
	 * 
	 * @param nodeObject The object representing the node in
	 * 			the navigation tree.  This may be either a
	 * 			TreeNode object or an ExtendableObject from
	 * 			the data model.
	 * @param parent The parent into which the top level control
	 * 			is to be created
	 * @return The control that contains the page specific content
	 * 			of the page
	 */
	public abstract Composite createControl(Object nodeObject, Composite parent);
}

