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

package net.sf.jmoney.model2;

import org.eclipse.swt.widgets.Control;

/**
 * @author Nigel
 *
 */
public interface IPropertyControl {
	/**
	 * This method gives access to the underlying control.
	 * Do not use this method to get the control for the
	 * purposes of getting and setting property values
	 * to and from the control.  Instead use the methods in
	 * this interface.  Use this method to get the control
	 * for the purpose of adding focus listeners and other
	 * such functionality. 
	 * 
	 * @return The underlying control.
	 */
	Control getControl();
	
	/**
	 * Load the value into the control object.
	 * 
	 * @param object The object that contains the value of
	 * the property.  This object must be a mutable object.
	 */
	void load(Object object);
	
	/**
	 * Load the value into the control object.  This method should
	 * be used when someone else has the object locked and
	 * so the control object should be disabled.  The value of the
	 * property should be shown in the control object.
	 * 
	 * @param object The object that contains the value of
	 * the property.  This object is a non-mutable object.
	 */
	void loadDisabled(Object object);
	
	void save();
}
