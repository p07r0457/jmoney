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

/**
 * @author Nigel
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface IExtensionPropertySetInfo {

	/**
	 * The registerExtensionProperties method provides information on
	 * the additional properties added by this extension.  However, we
	 * need a class into which we can store these properties.
	 * This class must have both getter and setter methods with public
	 * access for all the listed properties.  These getter and setter
	 * methods will be used by the framework to store the extension
	 * properties.
	 * <P>
	 * The extension class may have other methods which may be used
	 * in plug-ins that depend on the plug-in defining the extension. 
	 * <P>
	 * Properties in the base object are also registered using this
	 * interface.  However, there will be no extension class in such
	 * circumstances so null will be returned.
	 * 
	 * @return The class that contains a storage implementation
	 * 		for the properties listed, or null if the properties in
	 * 		this set are in the base object.
	 */
    Class getInterfaceClass();
    
    Class getMutableInterfaceClass();
    
    /**
     * This method is the first method called by the framework.
     * This method registers all the extension properties by calling
     * into the supplied IPropertyRegistrar interface.
     * <P>
     * The framework calls this method to get information on extension
     * properties.
     * Properties may be applicable only in certain conditions.
     * For example, the properties that relate to a stock account
     * such as the commission rates should not be shown unless
     * the account type has been set to 'stock'.
     * Only two forms are condition are supported:
     * - a boolean property is set to true
     * - an enumerated property is set to a particular value
     * <P>
     * The applicability of a property in one bookkeeping object class may 
     * depend on the value of a property in a different bookkeeping
     * object class.  For example, the applicability of the indicator 
     * in an entry as to 
     * whether an entry has been reconciled may depend on whether
     * the check box in the account properties has been set to allow
     * reconciliation.  The table is as follows:
     * - entry can depend on entry, transaction, commodity or account.
     * - transaction can depend on transaction only.
     * - account can depend on account only.
     * - commodity can depend on commodity only.
     * <P>
     * We also support custom dependencies.  The EnumerationAccessor
     * class and the PropertyAccessor_Boolean class implement the
     * IPropertyDependency interface which contains the isSelected 
     * method.  A property is set to be conditional by passing an 
     * IPropertyDependency interface.  The property
     * is applicable if the isSelected method returns true.
     * <P>
     * When a property is registered, a PropertyAccessor object is
     * returned.  The PropertyAccessor object must be used later on if
     * there are other properties that depend on this property.
     * <P>
     * Properties may depend on properties defined in other plug-ins.  
     * In that
     * case a PropertyAccessor object can be obtained by giving the fully
     * qualified name of the property.  The framework will be sure to first
     * call the registerProperties method for any plug-ins on which
     * this plug-in depends.
     * <P>
     * Properties may depend on properties defined elsewhere in this
     * same plug-in.  For example, an entry property may depend on
     * a property of the account in which the entry occurs.
     * The framework will always call registerProperties for
     * classes in the order:
     * <UL>
     * <LI>Commodity</LI>
     * <LI>CapitalAccount</LI>
     * <LI>Transaction</LI>
     * <LI>Entry</LI>
     * </UL>
     * <P>
     * The dependencies can only occur in this order, so we are safe.
     *  
     * @param propertyRegistrar The interface to be used by this method
     * 		to register the properties.
     */
    void registerProperties(IPropertyRegistrar propertyRegistrar);
}
