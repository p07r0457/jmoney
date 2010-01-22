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

package net.sf.jmoney.stocks.model;

import java.text.NumberFormat;
import java.text.ParseException;

import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.IObjectKey;
import net.sf.jmoney.model2.IValues;
import net.sf.jmoney.model2.ListKey;

public class Security extends Commodity {
	
	// This implementation formats all quantities as numbers with three decimal places.
	private int SCALE_FACTOR = 1000;

	private static NumberFormat numberFormat = NumberFormat.getNumberInstance();
	static {
		numberFormat.setMaximumFractionDigits(3);
		numberFormat.setMinimumFractionDigits(0);
	}
	
	private String cusip;
	private String symbol;
	
    /**
     * Constructor used by datastore plug-ins to create
     * a stock object.
     */
	public Security(
			IObjectKey objectKey,
			ListKey parentKey,
			String name,
			String cusip,
			String symbol,
			IValues extensionValues) {
		super(objectKey, parentKey, name, extensionValues);

		this.cusip = cusip;
		this.symbol = symbol;
	}

    /**
     * Constructor used by datastore plug-ins to create
     * a stock object.
     */
	public Security(
			IObjectKey objectKey,
			ListKey parentKey) {
		super(objectKey, parentKey);
		
		this.cusip = null;
		this.symbol = null;
	}

	@Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.stocks.security";
	}
	
	/**
	 * @return the 9 digit CUSIP if a US security or the CINS (one letter
	 *         followed by 8 digits) if a non-US security
	 */
	public String getCusip() {
		return cusip;
	}
	
	public String getSymbol() {
		return symbol;
	}
	
	/**
	 * @param cusip
	 *            the 9 digit CUSIP if a US security or the CINS (one letter
	 *            followed by 8 digits) if a non-US security
	 */
	public void setCusip(String cusip) {
		String oldCusip = this.cusip;
		this.cusip = cusip;

		// Notify the change manager.
		processPropertyChange(SecurityInfo.getCusipAccessor(), oldCusip, cusip);
	}

	public void setSymbol(String symbol) {
		String oldSymbol = this.symbol;
		this.symbol = symbol;

		// Notify the change manager.
		processPropertyChange(SecurityInfo.getSymbolAccessor(), oldSymbol, symbol);
	}

	@Override
	public long parse(String amountString) {
		Number amount;
		try {
			amount = numberFormat.parse(amountString);
		} catch (ParseException ex) {
			// If bad user entry, return zero
			amount = new Double(0);
		}
		return Math.round(amount.doubleValue() * SCALE_FACTOR);
	}
	
	@Override
	public String format(long amount) {
		double a = ((double) amount) / SCALE_FACTOR;
		return numberFormat.format(a);
	}
	
	/**
	 * @return The scale factor.  Always 1000 for stock for the time being.
	 */
	// TODO: This property should be for currency only.
	@Override
	public short getScaleFactor() {
		return 1000;
	}
}

