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
 * Some properties are only applicable if another property is set.
 * The other property may be a boolean property that must be set to
 * true or an enumerated type that must be set to a certain value.
 * For example, the 'credit card limit' property would only be
 * applicable if the 'account type' was set to 'credit card'.
 * For a property to depend on another property in this way, the other
 * property must be able to provide an implementation of this
 * IPropertyDependency interface.
 * <P>
 * @author  Nigel
 */
public interface IPropertyDependency {
    /**
     * @param object the object containing the property on which
     * 		the applicability of a property depends.
     */
	boolean isSelected(IExtendableObject object);
}
