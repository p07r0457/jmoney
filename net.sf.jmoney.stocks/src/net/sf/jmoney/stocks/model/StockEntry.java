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

package net.sf.jmoney.stocks.model;

import java.util.Date;

import net.sf.jmoney.model2.EntryExtension;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IObjectKey;

/**
 * Property set implementation class for the properties added
 * to each Entry object by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class StockEntry extends EntryExtension {

	/**
	 * indicates if this entry represents stock going in or
	 * out of the account.
	 */
	private boolean stockChange = false;
	
	/**
	 * The commodity (Stock or Bond) involved in this entry.
	 * <P> 
	 * Do we use this field for dividend payments, where no change
	 * in stock amounts is involved but we do want to associate the
	 * cash amount with a stock?
	 */
	protected IObjectKey stockKey = null;
	
	/**
	 * The date on which the deal was made.  On some stock
	 * exchanges (for example, the London Stock Exchange)
	 * this is different from the settlement date on which
	 * the money and stock is paid or received.
	 * <P> 
	 * This property is applicable only if stockChange is set
	 * to true.
	 */
	private Date bargainDate = null;

	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 * 
	 * @param extendedObject 
	 */
	public StockEntry(ExtendableObject extendedObject) {
		super(extendedObject);
	}
	
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 */
	public StockEntry(ExtendableObject extendedObject, boolean stockChange, IObjectKey stockKey, Date bargainDate) {
		super(extendedObject);
		this.stockChange = stockChange;
		this.stockKey = stockKey;
		this.bargainDate = bargainDate;
	}
	
	/**
	 * @return true if this entry represents an addition of stock
	 * 			to the account or a removal of stock from the account,
	 * 			false if this entry represents anything else, such as
	 * 			cash dividends going into the account.
	 */
	public boolean isStockChange() {
		return stockChange;
	}
	
	/**
	 * @param stockChange true if this entry represents an addition of stock
	 * 			to the account or a removal of stock from the account,
	 * 			false if this entry represents anything else, such as
	 * 			cash dividends going into the account.
	 */
	public void setStockChange(boolean stockChange) {
		this.stockChange = stockChange;
	}
	
	/**
	 * Gets the stock involved in this entry.
	 * 
	 * @return An object of type Stock or Bond.
	 * 		Null will be returned if no value has previously
	 * 		been set.
	 */
	public Stock getStock() {
		return stockKey == null ? null : (Stock)stockKey.getObject();
	}
	
	/**
	 * Sets the stock involved in this entry.
	 * 
	 * @param stock An object of type Stock or Bond.
	 */
	public void setStock(Stock stock) {
		Stock oldStock = getStock();
		this.stockKey = (stock == null) ? null : stock.getObjectKey();

		// Notify the change manager.
		processPropertyChange(StockEntryInfo.getStockAccessor(), oldStock, stock);
	}

	/**
	 * @return The date on which the deal was made.
	 */
	public Date getBargainDate() {
		return bargainDate;
	}
	
	/**
	 * @param bargainDate The date on which the deal was made.
	 */
	public void setBargainDate(Date bargainDate) {
		Date oldBargainDate = this.bargainDate;
		this.bargainDate = bargainDate;
		processPropertyChange(StockEntryInfo.getBargainDateAccessor(), oldBargainDate, bargainDate);
	}
}
