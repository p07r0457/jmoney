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

import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CapitalAccountImpl;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertyAccessor;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Composite;

/**
 * Note that this class has neither get/set methods for the value being edited
 * and no support for property change listeners.  This is
 * because objects of this class are tied to an CapitalAccount object.  
 * Changes to this
 * object are reflected by this object in the CapitalAccount class objects.  
 * Consumers who are interested in changes to the CapitalAccount class objects should
 * add themselves as listeners to the appropriate PropertyAccessor object.
 * <P>
 * 
 * @author  Nigel
 */
public class TextEditor implements IPropertyControl {
    
    private CapitalAccountImpl account = null;
    
    private PropertyAccessor propertyAccessor;
    
    private Text propertyControl;
    
    /** Creates new TextEditor */
    public TextEditor(Composite parent, PropertyAccessor propertyAccessor) {
    	propertyControl = new Text(parent, 0);
    	this.propertyAccessor = propertyAccessor;
    }
    
    /** Creates new TextEditor */
    public TextEditor(Composite parent, int style, PropertyAccessor propertyAccessor) {
    	propertyControl = new Text(parent, style);
    	this.propertyAccessor = propertyAccessor;
    }
    
    /**
     * Load the control with the value from the given account.
     */
    public void load(Object object) {
    	account = (CapitalAccountImpl)object;
    	
		String text = account.getStringPropertyValue(propertyAccessor);
		propertyControl.setText(text == null ? "" : text);

		propertyControl.setEnabled(true);
    }
    
    /**
     * Load the control with the value from the given account.
     */
    public void loadDisabled(Object object) {
    	CapitalAccount nonMutableAccount = (CapitalAccount)object;
    	account = null;
    	
		String text = account.getStringPropertyValue(propertyAccessor);
		propertyControl.setText(text == null ? "" : text);
		
		propertyControl.setEnabled(false);
    }
    
    /**
     * Save the value from the control back into the account object.
     *
     * Editors may update the property on a regular basis, not just when
     * the framework calls the <code>save</code> method.  However, the only time
     * that editors must update the property is when the framework calls this method.
     */
    public void save() {
		account.setStringPropertyValue(propertyAccessor, propertyControl.getText());
    }

    /* (non-Javadoc)
	 * @see net.sf.jmoney.model2.IPropertyControl#getControl()
	 */
	public Control getControl() {
		return propertyControl;
	}
}
