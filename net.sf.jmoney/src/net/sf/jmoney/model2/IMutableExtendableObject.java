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
 * This is the base class for all objects that may have extension
 * property sets added by plug-ins.  The framework supports the
 * following objects that may be extended:
 * <UL>
 * <LI>Session</LI>
 * <LI>Commodity</LI>
 * <LI>Account</LI>
 * <LI>Transaxion</LI>
 * <LI>Entry</LI>
 * </UL>
 * <P>
 * Plug-ins are also able to create new classes of extensible
 * objects by deriving classes from this class.
 */
public interface IMutableExtendableObject extends IExtendableObject {

	void setPropertyValue(PropertyAccessor propertyAccessor, Object value);
	
	void setIntegerPropertyValue(PropertyAccessor propertyAccessor, int value);
	
	void setLongPropertyValue(PropertyAccessor propertyAccessor, long value);
	
	void setStringPropertyValue(PropertyAccessor propertyAccessor, String value);
	
	void setCharacterPropertyValue(PropertyAccessor propertyAccessor, char value);
	
	// TODO: check whether we need this method.
	void setPropertyValueFromString(PropertyAccessor propertyAccessor, String value);
}

