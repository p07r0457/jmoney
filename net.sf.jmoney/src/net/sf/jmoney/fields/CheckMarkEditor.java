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

package net.sf.jmoney.fields;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertyAccessor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Editor class for boolean values that are to be edited using a check box.
 *
 * @author Nigel Westbury
 */
public class CheckMarkEditor implements IPropertyControl {

    protected PropertyAccessor fPropertyAccessor;
    protected Button fPropertyControl;
    protected ExtendableObject fExtendableObject;

    /**
     * Create a new date editor.
     */
    public CheckMarkEditor(Composite parent, PropertyAccessor propertyAccessor) {
        fPropertyControl = new Button(parent, SWT.CHECK);
        fPropertyAccessor = propertyAccessor;

        // Selection changes are reflected immediately in the
        // datastore.  This allows controls for other properties,
        // which may depend on this property, to be immediately made
        // visible or invisible.

        fPropertyControl.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                save();
            }
        });
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#load(net.sf.jmoney.model2.ExtendableObject)
     */
    public void load(ExtendableObject object) {
    	this.fExtendableObject = object;
    	if (object == null) {
            fPropertyControl.setSelection(false);
    	} else {
            Boolean value = (Boolean)object.getPropertyValue(fPropertyAccessor);
            fPropertyControl.setSelection(value.booleanValue());
    	}
    	fPropertyControl.setEnabled(object != null);
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#save()
     */
    public void save() {
        boolean value = fPropertyControl.getSelection();
        fExtendableObject.setPropertyValue(fPropertyAccessor, new Boolean(value));
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#getControl()
     */
    public Control getControl() {
        return fPropertyControl;
    }

}