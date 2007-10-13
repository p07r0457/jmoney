/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

package net.sf.jmoney.stocks;

import java.text.NumberFormat;
import java.text.ParseException;

import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.IObjectKey;
import net.sf.jmoney.model2.IValues;
import net.sf.jmoney.model2.ListKey;

public class Stock extends Commodity {
	
	private static NumberFormat numberFormat = NumberFormat.getNumberInstance();
	static {
		numberFormat.setMaximumFractionDigits(0);
		numberFormat.setMinimumFractionDigits(0);
	}
	
	private String nominalValue;
	
    /**
     * Constructor used by datastore plug-ins to create
     * a stock object.
     */
	public Stock(
			IObjectKey objectKey,
			ListKey parentKey,
			String name,
			String nominalValue,
			IValues extensionValues) {
		super(objectKey, parentKey, name, extensionValues);
		
		this.nominalValue = nominalValue;
	}

    /**
     * Constructor used by datastore plug-ins to create
     * a stock object.
     */
	public Stock(
			IObjectKey objectKey,
			ListKey parentKey) {
		super(objectKey, parentKey);
		
		this.nominalValue = null;
	}

	@Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.stocks.stock";
	}
	
	/**
	 * @return the nominal value.  For example, "ORD 25P"
	 */
	public String getNominalValue() {
		return nominalValue;
	}
	
	public void setNominalValue(String nominalValue) {
		String oldNominalValue = this.nominalValue;
		this.nominalValue = nominalValue;

		// Notify the change manager.
		processPropertyChange(StockInfo.getNominalValueAccessor(), oldNominalValue, nominalValue);
	}
	
	@Override
	public long parse(String amountString) {
		Number amount;
		try {
			amount = numberFormat.parse(amountString);
		} catch (ParseException pex) {
			amount = new Double(0);
		}
		return Math.round(amount.doubleValue());
	}
	
	@Override
	public String format(long amount) {
		double a = amount;
		return numberFormat.format(a);
	}
	
	/**
	 * @return The scale factor.  Always 1 for stock.
	 */
	// TODO: This property should be for currency only.
	@Override
	public short getScaleFactor() {
		return 1;
	}
}

