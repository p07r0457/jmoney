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

import net.sf.jmoney.VerySimpleDateFormat;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertyAccessor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * Editor class for date properties. These dates are formatted according to the
 * selected format in the preferences.
 *
 * @author Johann Gyger
 */
public class DateEditor implements IPropertyControl {

    protected VerySimpleDateFormat fDateFormat;
    protected PropertyAccessor fPropertyAccessor;
    protected Text fPropertyControl;
    protected ExtendableObject fExtendableObject;

    /**
     * Create a new date editor.
     */
    public DateEditor(Composite parent, PropertyAccessor propertyAccessor, VerySimpleDateFormat dateFormat) {
        fPropertyControl = new Text(parent, 0);
        fPropertyAccessor = propertyAccessor;
        fDateFormat = dateFormat;
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#load(net.sf.jmoney.model2.ExtendableObject)
     */
    public void load(ExtendableObject object) {
        Date d = (Date) object.getPropertyValue(fPropertyAccessor);
        fPropertyControl.setText(fDateFormat.format(d));
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#save()
     */
    public void save() {
        String text = fPropertyControl.getText();
        fExtendableObject.setPropertyValue(fPropertyAccessor, fDateFormat.parse(text));
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#getControl()
     */
    public Control getControl() {
        return fPropertyControl;
    }

}