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

package net.sf.jmoney.amazon;

import java.util.Date;

import net.sf.jmoney.fields.DateControlFactory;
import net.sf.jmoney.fields.TextControlFactory;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IValues;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

/**
 * Add extra properties to the Entry objects that are entries that have
 * been imported from Amazon.
 */
public class AmazonEntryInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<AmazonEntry> propertySet = PropertySet.addExtensionPropertySet(AmazonEntry.class, EntryInfo.getPropertySet(), new IExtensionObjectConstructors<AmazonEntry>() {

		public AmazonEntry construct(ExtendableObject extendedObject) {
			return new AmazonEntry(extendedObject);
		}

		public AmazonEntry construct(ExtendableObject extendedObject, IValues values) {
			return new AmazonEntry(
					extendedObject, 
					values.getScalarValue(getOrderIdAccessor()),
					values.getScalarValue(getShipmentDateAccessor()),
					values.getScalarValue(getAsinOrIsbnAccessor())
			);
		}
	});
	
	private static ScalarPropertyAccessor<String> orderIdAccessor;
	private static ScalarPropertyAccessor<Date> shipmentDateAccessor;
	private static ScalarPropertyAccessor<String> asinOrIsbnAccessor;
		
	public PropertySet<AmazonEntry> registerProperties() {
		IPropertyControlFactory<String> textPropertyControlFactory = new TextControlFactory();
		IPropertyControlFactory<Date> datePropertyControlFactory = new DateControlFactory();
		
		orderIdAccessor = propertySet.addProperty("orderId", "Amazon Order Id", String.class, 0, 80, textPropertyControlFactory, null);
		shipmentDateAccessor = propertySet.addProperty("shipmentDate", "Shipment Date", Date.class, 0, 100, datePropertyControlFactory, null);
		asinOrIsbnAccessor = propertySet.addProperty("asinOrIsbn", "ASIN/ISBN", String.class, 0, 80, textPropertyControlFactory, null);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<AmazonEntry> getPropertySet() {
		return propertySet;
	}

	public static ScalarPropertyAccessor<String> getOrderIdAccessor() {
		return orderIdAccessor;
	}

	public static ScalarPropertyAccessor<Date> getShipmentDateAccessor() {
		return shipmentDateAccessor;
	}	

	public static ScalarPropertyAccessor<String> getAsinOrIsbnAccessor() {
		return asinOrIsbnAccessor;
	}	
}
