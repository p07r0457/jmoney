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

import java.lang.reflect.Method;
import java.beans.*;   // for PropertyChangeSupport and PropertyChangeListener
import java.util.Comparator;
import javax.swing.JComponent;

import org.eclipse.swt.widgets.Composite;

/**
 * This class contains information about a property.  The property may be in the base
 * bookkeeping class or in an extension bookkeeping class.
 *
 * This object contains the metadata.
 *
 * Some properties may be allowed to take null values and
 * some may not.  The determination is made by the JMoney framework
 * by looking at the type returned by the getter method.
 * <P>
 * The following properties may take null values:
 * <UL>
 * <LI>All properties that reference extendable objects</LI>
 * <LI>All properties of type String, Date or other such simple class</LI>
 * <LI>All properties where the type is one of the classes representing intrinsic types,
 * 			such as Integer, Long</LI>
 * </UL>
 * The following properties may not take null values:
 * <UL>
 * <LI>All properties where the type is an intrinsic type, such as int, long</LI>
 * </UL>
 * 
 * @author  Nigel
 */
public interface PropertyAccessor {
    /**
     * Returns the property set which contains this property.
     */
    PropertySet getPropertySet();
    
    /**
     * Returns the extendable property set which contains this property.
     * 
     * If the property is in an extendable property set then this
     * method returns the same value as <code>getPropertySet()</code>.
     * If the property is in an extension property set then
     * the property set being extended is returned.
     */
    // TODO: Consider removing this method.  It is not used.
    PropertySet getExtendablePropertySet();
    
    Object invokeGetMethod(Object invocationTarget);
    
    void invokeSetMethod(Object invocationTarget, Object value);
    
    ExtendableObject invokeCreateMethod(Object invocationTarget);
    
    ExtendableObject invokeCreateMethod(Object invocationTarget, PropertySet actualPropertySet);
    
    boolean invokeDeleteMethod(Object invocationTarget, ExtendableObject value);
    
    /**
     * Returns the property class.
     * The value of this property must be an object of this class.
     * The value of this property may be an object of a class
     * that is derived from this class.
     * If this property is a list property then this method
     * returns the class of the elements of the list.
     * 
     * @return
     */
    Class getValueClass();
    
    /**
     * Returns the PropertySet for the values of this property.
     * This property must contain a value or values that are
     * extendable objects. 
     */
    PropertySet getValuePropertySet();

    /**
     * Return a name for this property.
     *
     * This name is used by the framework for persisting information about the property
     * in configuration files etc.  For example, if the user sorts a column based on a
     * property then that information can be stored in a configuration file so that the
     * data is sorted on the column the next time the user loads the view.
     */
    String getName();
    
    /**
     * The local name of the property is just the last part of the name, after the last
     * dot.  This will be unique within an extension but may not be unique
     * across all plugins or even across extensions to different types of
     * bookkeeping objects (entries, categories, transactions, or commodities)
     * within a plug-in.
     */
    String getLocalName();

    /**
     * A short description that is suitable as a column header when this
     * property is displayed in a table.
     */
    String getShortDescription();

    /**
     * The width weighting to be used when this property is displayed
     * in a table or a grid.
     */
    double getWidth();

    /**
     * Indicates whether users are able to sort views based on this property.
     */
    boolean isSortable();
    
    /**
     * Indicates if the property is a single intrinsic value or object
     * (not a list of values)
     */
    boolean isScalar();
    
    /**
     * Indicates if the property is a list of intrinsic values or objects.
     */
    boolean isList();
    
    /**
     * Indicates whether the property may be edited by the user.
     * Why are not all properties editable???
     */
    boolean isEditable();
    
    /**
     * Gets an instance of a bean that edits this property.
     *
     * This method can be called only if isEditable returns true.
     */
    JComponent getEditorBean();
    
    /**
     * If the bean defines a custom comparator for this property then fetch it.
     * Otherwise return null to indicate that the default comparator for the type
     * should be used.
     */
    Comparator getCustomComparator();
    
	/**
	 * Create a Control object that edits the property.
	 * 
	 * @param parent
	 * @return An interface to a wrapper class.
	 */
	IPropertyControl createPropertyControl(Composite parent);

	/**
	 * Format the value of a property so it can be embedded into a
	 * message.
	 *
	 * The returned value will look sensible when embedded in a message.
	 * Therefore null values and empty values are returned as non-empty
	 * text such as "none" or "empty".  Text values are placed in
	 * quotes unless sure that only a single word will be returned that
	 * would be readable without quotes.
	 *
	 * @return The value of the property formatted as appropriate.
	 */
	String formatValueForMessage(ExtendableObject object);

	/**
	 * Format the value of a property as appropriate for displaying in a
	 * table.
	 * 
	 * The returned value is expected to be displayed in a table or some similar
	 * view.  Null and empty values are therefore returned as empty strings.
	 * Text values are not quoted.
	 * 
	 * @return The value of the property formatted as appropriate.
	 */
	String formatValueForTable(ExtendableObject object);

	/**
	 * @return the index into the constructor parameters, where
	 * 		an index of zero indicates that the property is the
	 * 		first parameter to the constructor.  An index of -1
	 * 		indicates that the property is not passed to the
	 * 		constructor (the property value is redundant and the
	 * 		object can be fully re-constructed from the other
	 * 		properties).
	 */
	int getIndexIntoConstructorParameters();

	/**
	 * @return
	 */
	int getIndexIntoScalarProperties();

	/**
	 * 
	 * @param indexIntoConstructorParameters
	 */
	// TODO: This method should be accessible only from within the package. 
	void setIndexIntoConstructorParameters(int indexIntoConstructorParameters);

	/**
	 * @param values
	 * @return The value of this property from the
	 * 			passed <code>values</code> object.
	 */
//	public Object getValue(IExtendableObject values);

	/**
	 * 
	 */
	void initMethods();

	/**
	 * @param i
	 */
	void setIndexIntoScalarProperties(int indexIntoScalarIndex);
}
