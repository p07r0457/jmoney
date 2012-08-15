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

package net.sf.jmoney.property.model;

import java.util.Date;

import net.sf.jmoney.fields.CheckBoxControlFactory;
import net.sf.jmoney.fields.DateControlFactory;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IObjectKey;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.IValues;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ReferencePropertyAccessor;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

/**
 * Add extra properties to the Entry objects that are entries that represent
 * an acquisition or disposal of a real property.
 * 
 * An entry for an amount of a real property exists any time the number of
 * the real property in an account changes.  This can happen when a property
 * is acquired in any way, disposed of in any way, or transfered from one
 * account to another.
 * <P>
 * The entry will contain the amount of the property.  This amount is kept in
 * the base Amount property field.  The entry will also contain the particular
 * stock or bond concerned.  In the case of, for example, the bank account
 * entries, the currency is not kept in the entry but is kept in the account
 * object.  However, a stock account will usually contain stock in many different
 * companies (unless it is your employee stock purchase plan account). 
 * Therefore a reference to the stock Commodity
 * object is kept in the Entry object.
 * <P>
 * @author Nigel Westbury
 */
public class RealPropertyEntryInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<RealPropertyEntry> propertySet = PropertySet.addExtensionPropertySet(RealPropertyEntry.class, EntryInfo.getPropertySet(), new IExtensionObjectConstructors<RealPropertyEntry>() {

		public RealPropertyEntry construct(ExtendableObject extendedObject) {
			return new RealPropertyEntry(extendedObject);
		}

		public RealPropertyEntry construct(ExtendableObject extendedObject, IValues values) {
			return new RealPropertyEntry(
					extendedObject, 
					values.getReferencedObjectKey(getSecurityAccessor()),
					values.getScalarValue(getBargainDateAccessor())
			);
		}
	});
	
	private static ReferencePropertyAccessor<RealProperty> securityAccessor;
	private static ScalarPropertyAccessor<Date> bargainDateAccessor;
	
	public PropertySet registerProperties() {
		IPropertyControlFactory<Boolean> booleanPropertyControlFactory = new CheckBoxControlFactory();

		IReferenceControlFactory<RealPropertyEntry,RealProperty> securityPropertyControlFactory = new RealPropertyControlFactory<RealPropertyEntry>() {
			public IObjectKey getObjectKey(RealPropertyEntry parentObject) {
				return parentObject.securityKey;
			}
		};
		
		IPropertyControlFactory<Date> datePropertyControlFactory = new DateControlFactory();
		
		securityAccessor = propertySet.addProperty("security", "RealProperty", RealProperty.class, 2, 20, securityPropertyControlFactory, null);
		bargainDateAccessor = propertySet.addProperty("bargainDate", "Bargain Date", Date.class, 0, 20, datePropertyControlFactory, null);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<RealPropertyEntry> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<RealProperty> getSecurityAccessor() {
		return securityAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Date> getBargainDateAccessor() {
		return bargainDateAccessor;
	}	
}
