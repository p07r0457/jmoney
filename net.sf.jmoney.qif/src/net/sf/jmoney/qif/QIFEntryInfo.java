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

package net.sf.jmoney.qif;

import net.sf.jmoney.model2.CurrencyImpl;
import net.sf.jmoney.model2.IExtensionPropertySetInfo;
import net.sf.jmoney.model2.IPropertyRegistrar;

/**
 * @author Nigel
 *
 * Add extra properties to the Entry objects to support QIF import
 * and export.  The plug-in in which this class appears provides support
 * to import and export QIF files.  However not all properties supported
 * by QIF files are included in the base set of JMoney properties.
 * We do not want to drop any data when importing a QIF file because,
 * even though the JMoney framework does not know about all the
 * properties, plug-ins may be able to make use of the properties.
 * This class adds all the properties supported by QIF that are
 * not base JMoney properties.
 * <P>
 * The data can be accessed by a plug-in in one of three ways:
 * <LI>A plug-in can depend on this plug-in.  That plug-in can then
 * 		access the properties in this class.</LI>
 * <LI>A propagator plug-in can progagate property values between this
 * 		class and properties in other Entry extension property set 
 * 		classes.
 * 		This approach should be taken if using a plug-in that was
 * 		developed without any knowledge of this plug-in.</LI>
 * <LI>Even if no other plug-in accesses a property imported by QIF
 * 		import, the property value will be maintained for as long as
 * 		the entry is not deleted and will be written out if a QIF
 * 		export is performed.</LI>
 * <P>
 */
public class QIFEntryInfo implements IExtensionPropertySetInfo {

    public QIFEntryInfo() {
    }

	public Class getImplementationClass() {
		return QIFEntry.class;
	}
	
    public Class getInterfaceClass() {
        return QIFEntry.class;
    }
    
	public void registerProperties(IPropertyRegistrar extensionRegistrar) {
		extensionRegistrar.addProperty("reconcilingState", "what???", 2.0, null, null, null);
	}

}
