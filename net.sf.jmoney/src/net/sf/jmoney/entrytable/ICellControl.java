/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2007 Nigel Westbury <westbury@users.sf.net>
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

package net.sf.jmoney.entrytable;

import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;

/**
 * This interface is used to wrap the cell controls in
 * an entries table, giving all controls a common interface.
 * 
 * It is essential that all controls have a common interface
 * because the framework may have to deal with columns of data
 * that were contributed by another plug-in. 
 * 
 * @param T the type of the object that supplies the data
 * 			for this cell
 * 
 * @author Nigel Westbury
 */
public interface ICellControl<T> {

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
     * Load data data for the given row into this control.
     */
    void load(T data);

    /**
	 * This method takes the data in the control and sets it into the datastore.
	 * This method must be called before a control is destroyed or before new
	 * input is set into the control with another call to <code>load</code>,
	 * otherwise data entered by the user may not be saved.
	 * <P>
	 * Some controls will save data in response to user edits. Combo boxes
	 * typically do this. Other controls, such as text boxes, do not.
	 */
    void save();

    /**
	 * Needed only when child controls may be created later. This method should
	 * be removed at some point.
	 * 
	 * @param controlFocusListener
	 */
	void setFocusListener(FocusListener controlFocusListener);
}