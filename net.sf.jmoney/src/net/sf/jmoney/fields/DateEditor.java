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

package net.sf.jmoney.fields;

import java.util.Date;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertyAccessor;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Editor class for date properties. These dates are formatted according to the
 * selected format in the preferences.
 *
 * @author Johann Gyger
 */
public class DateEditor implements IPropertyControl {

    protected PropertyAccessor fPropertyAccessor;
    protected DateControl fPropertyControl;
    protected ExtendableObject fExtendableObject;

	/**
     * Create a new date editor.
     */
    public DateEditor(final Composite parent, PropertyAccessor propertyAccessor) {
        fPropertyAccessor = propertyAccessor;
        
		Font font = parent.getFont();

		fPropertyControl = new DateControl(parent);
		fPropertyControl.setFont(font);
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#load(net.sf.jmoney.model2.ExtendableObject)
     */
    public void load(ExtendableObject object) {
    	this.fExtendableObject = object;
    	if (object == null) {
    		fPropertyControl.setDate(null);
    	} else {
            Date d = (Date) object.getPropertyValue(fPropertyAccessor);
    		fPropertyControl.setDate(d);
    	}
    	fPropertyControl.setEnabled(object != null);
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#save()
     */
    public void save() {
        fExtendableObject.setPropertyValue(fPropertyAccessor, fPropertyControl.getDate());
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#getControl()
     */
    public Control getControl() {
        return fPropertyControl;
    }
}