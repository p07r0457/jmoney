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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

/**
 * @author Nigel
 *
 */
public interface IPropertyControlFactory {
	/**
	 * Create a control that edits the property.
	 * <P>
	 * The PropertyAccessor object is not known when the factory
	 * is created so we require that it is passed as a parameter
	 * when a control is created.

	 * @return An interface to the class that wraps the
	 * 			control.
	 */
	IPropertyControl createPropertyControl(Composite parent, PropertyAccessor propertyAccessor);

	/**
	 * Create a CellEditor object that enables the property to
	 * be edited in-place in a <code>Table</code>.
	 * 
	 * @param table
	 * @return a cell editor, or null if the property cannot
	 * 			be edited in-place in a <code>Table</code>
	 */
	CellEditor createCellEditor(Table table);


	/**
	 * Get the value of a property and return it typed for
	 * use in the cell editor.
	 * <P>
	 * For example, the ComboBoxCellEditor cell editors require the
	 * value as an integer index into the selection list.  Therefore,
	 * if <code>createCellEditor</code> returns a ComboBoxCellEditor
	 * then this method must return the value as an index into the list
	 * of possible values.
	 * <P>
	 * Note that a returned value of null indicates that the property
	 * is not editable.  Therefore, if the cell is editable, null values
	 * must be converted to empty strings or some other appropriate
	 * non-null value.
	 * 
	 * @param extendableObject
	 * @return
	 */
	Object getValueTypedForCellEditor(ExtendableObject extendableObject, PropertyAccessor propertyAccessor);

	/**
	 * Set the value of a property given a value returned by the cell editor.
	 * <P>
	 * For example, the ComboBoxCellEditor cell editors give the
	 * value as an integer index into the selection list.  Therefore,
	 * if <code>createCellEditor</code> returns a ComboBoxCellEditor
	 * then this method must set the appropriate value as determined from
	 * the given index.
	 * 
	 * @param extendableObject
	 * @param value
	 */
	void setValueTypedForCellEditor(ExtendableObject extendableObject, PropertyAccessor propertyAccessor, Object value);

	/**
	 * Format the value of a property so it can be embedded into a
	 * message.
	 *
	 * The returned value must look sensible when embedded in a message.
	 * Therefore null values and empty values must return non-empty
	 * text such as "none" or "empty".  Text values should be placed in
	 * quotes unless sure that only a single word will be returned that
	 * would be readable without quotes.
	 *
	 * @return The value of the property formatted as appropriate.
	 */
	String formatValueForMessage(ExtendableObject extendableObject, PropertyAccessor propertyAccessor);

	/**
	 * Format the value of a property as appropriate for displaying in a
	 * table.
	 * 
	 * The returned value will be displayed in a table or some similar
	 * view.  Null and empty values should be returned as empty strings.
	 * Text values should not be quoted.
	 * 
	 * @return The value of the property formatted as appropriate.
	 */
	String formatValueForTable(ExtendableObject extendableObject, PropertyAccessor propertyAccessor);

	/**
	 * Indicates if the property is editable.  If the property
	 * is editable then the <code>createPropertyControl</code>
	 * method must create and return a valid property.  If the
	 * property is not editable then the <code>createPropertyControl</code>
	 * method will never be called by the framework.
	 * <P>
	 * Most properties will be editable.  However some properties,
	 * such as the creation date for each entry, cannot be edited
	 * by the user.  The rest of this interface must still be implemented
	 * so that the values can be formatted correctly for displaying
	 * to the user.
	 * 
	 * @return true if a control is provided to allow the user to
	 * 			edit the property, false if the user cannot edit
	 * 			the property
	 */
	boolean isEditable();
}
