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
 * @author  Nigel
 */
public interface PropertyAccessor {
    /**
     * Returns the object that acts as the key for the property set
     * which contains this property.  If the property was created by
     * the framework then null will be returned.
     */
    PropertySet getPropertySet();
    
    Method getTheGetMethod();
    
    Method getTheSetMethod();
    
    Method getTheAddMethod();
    
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
     * When someone is listening for changes to a property in an object, they usually
     * want to know about a change to the property in any of the objects of
     * the type that contains the property.
     * It would be very inefficient to add yourself as a listener to every object.
     * Therefore one can instead add a listener to the PropertyAccessor object
     * and be told about changes to the property in any object.
     *
     * To implement this, all objects of the appropriate type will, when a 
     * property changes, tell the PropertyAccessor object.
     *
     * @param propertyName  The name of the property to listen on.
     * @param listener  The PropertyChangeListener to be added
     */
    void addPropertyChangeListener(
        String propertyName,
        PropertyChangeListener listener);
    
    /**
     * Remove a PropertyChangeListener for a specific property.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The PropertyChangeListener to be removed
     */
    void removePropertyChangeListener(
        String propertyName,
        PropertyChangeListener listener);
    
    /**
     * This method must be called when the value of this property is changed in
     * any object that contains this property.
     */
    void firePropertyChange(IExtendableObject source, Object oldValue, Object newValue);

	/**
	 * Create a Control object that edits the property.
	 * 
	 * @param parent
	 * @return An interface to a wrapper class.
	 */
	IPropertyControl createPropertyControl(Composite parent);
}
