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
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.SessionChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * A property control to handle ordinary text input.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class TextEditor implements IPropertyControl {

    private ExtendableObject extendableObject;

    private ScalarPropertyAccessor<String> propertyAccessor;

    private Text propertyControl;

    private SessionChangeListener amountChangeListener = new SessionChangeAdapter() {
		public void objectChanged(ExtendableObject changedObject, ScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
			if (changedObject.equals(extendableObject) && changedProperty == propertyAccessor) {
	            String text = extendableObject.getPropertyValue(propertyAccessor);
	            propertyControl.setText(text == null ? "" : text);
			}
		}
	};
	
    /** Creates new TextEditor */
    public TextEditor(Composite parent, int style, ScalarPropertyAccessor<String> propertyAccessor) {
        propertyControl = new Text(parent, style);
        this.propertyAccessor = propertyAccessor;
    	
    	propertyControl.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
		    	if (extendableObject != null) {
		    		extendableObject.getObjectKey().getSessionManager().removeChangeListener(amountChangeListener);
		    	}
			}
		});
    }

    public void load(ExtendableObject object) {
    	if (extendableObject != null) {
    		extendableObject.getObjectKey().getSessionManager().removeChangeListener(amountChangeListener);
    	}
    	
        extendableObject = object;

        if (object == null) {
            propertyControl.setText("");
    	} else {
            String text = object.getPropertyValue(propertyAccessor);
            propertyControl.setText(text == null ? "" : text);
        	
        	/*
        	 * We must listen to the model for changes in the value
        	 * of this property.
        	 */
            object.getObjectKey().getSessionManager().addChangeListener(amountChangeListener);
    	}
    	propertyControl.setEnabled(object != null);
    }

    public void save() {
        String text = propertyControl.getText();
        extendableObject.setPropertyValue(propertyAccessor, text.length() == 0 ? null : text);
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#getControl()
     */
    public Control getControl() {
        return propertyControl;
    }

}