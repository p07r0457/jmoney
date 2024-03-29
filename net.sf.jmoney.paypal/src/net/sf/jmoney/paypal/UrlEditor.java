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

package net.sf.jmoney.paypal;

import java.net.MalformedURLException;
import java.net.URL;

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
public class UrlEditor implements IPropertyControl<ExtendableObject> {

    private ExtendableObject extendableObject;

    private ScalarPropertyAccessor<URL> propertyAccessor;

    private Text propertyControl;

    private SessionChangeListener amountChangeListener = new SessionChangeAdapter() {
		@SuppressWarnings("unchecked")
		@Override
		public void objectChanged(ExtendableObject changedObject, ScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
			if (changedObject.equals(extendableObject) && changedProperty == propertyAccessor) {
	            URL url = extendableObject.getPropertyValue(propertyAccessor);
	            propertyControl.setText(url == null ? "" : url.toExternalForm());
			}
		}
	};
	
    /** Creates new TextEditor */
    public UrlEditor(Composite parent, ScalarPropertyAccessor<URL> propertyAccessor) {
        propertyControl = new Text(parent, SWT.NONE);
        this.propertyAccessor = propertyAccessor;
    	
    	propertyControl.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
		    	if (extendableObject != null) {
		    		extendableObject.getDataManager().removeChangeListener(amountChangeListener);
		    	}
			}
		});
    }

    public void load(ExtendableObject object) {
    	if (extendableObject != null) {
    		extendableObject.getDataManager().removeChangeListener(amountChangeListener);
    	}
    	
        extendableObject = object;

        if (object == null) {
            propertyControl.setText("");
    	} else {
            URL url = object.getPropertyValue(propertyAccessor);
            propertyControl.setText(url == null ? "" : url.toExternalForm());
        	
        	/*
        	 * We must listen to the model for changes in the value
        	 * of this property.
        	 */
            object.getDataManager().addChangeListener(amountChangeListener);
    	}
    	propertyControl.setEnabled(object != null);
    }

    public void save() {
        String text = propertyControl.getText();
		try {
			URL url = text.length() == 0 ? null : new URL(text);
	        extendableObject.setPropertyValue(propertyAccessor, url);
		} catch (MalformedURLException e) {
			// TODO Throw error so user is told of bad URL
			e.printStackTrace();
		}
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#getControl()
     */
    public Control getControl() {
        return propertyControl;
    }

}