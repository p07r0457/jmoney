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

package net.sf.jmoney.reconciliation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Transaction;

import org.eclipse.swt.widgets.Shell;

/**
 * An implementation of this interface must be provided by all
 * extensions to the net.sf.jmoney.bankstatements extension point.
 * 
 * @author Nigel Westbury
 */
public interface IBankStatementSource {

	/**
	 * @param account
	 *            the account into which the entries are being imported.
	 *            Implementations of this method do not generally need to know
	 *            the account because the entry data are returned in a
	 *            collection of EntryData objects and it is up to the caller to
	 *            merge the data into the datastore. However, there are
	 *            instances where information from the account is needed. For
	 *            example, knowing the currency of the account may affect the
	 *            way amounts are interpreted or implementations may add
	 *            properties to the account objects that affect the import
	 *            process.
	 * @param defaultEndDate 
	 * @param defaultStartDate 
	 * @return a collection of EntryData objects if entries are available for
	 *         importing, or null if the user cancelled the operation or if an
	 *         error occured.
	 */
	Collection<EntryData> importEntries(Shell shell, CurrencyAccount account, Date defaultStartDate, Date defaultEndDate);
	
	class EntryData {
		public Date clearedDate = null;
		public Date valueDate = null;
		public String check = null;
		private String memo = null;
		private String type = null;
		private String name = null;
		private String payee = null;
		public long amount = 0;  // Use getter???
		public String uniqueId = null;
		private Map<PropertyAccessor, Object> propertyMap = new HashMap<PropertyAccessor, Object>();
		
		public void setClearedDate(Date clearedDate) {
			this.clearedDate  = clearedDate;
		}
		public void setValueDate(Date valueDate) {
			this.valueDate = valueDate;
		}
		public void setCheck(String check) {
			this.check = check;
		}
		public void setMemo(String memo) {
			this.memo = memo;
		}
		public void setType(String type) {
			this.type = type;
		}
		public void setName(String name) {
			this.name = name;
		}
		public void setPayee(String payee) {
			this.payee = payee;
		}
		public void setAmount(long amount) {
			this.amount = amount;
		}
		public void setUniqueId(String uniqueId) {
			this.uniqueId = uniqueId;
		}
		public void setProperty(PropertyAccessor propertyAccessor, Object value) {
			propertyMap.put(propertyAccessor, value);
		}
		
		/**
		 * This method is given a transaction with two entries.
		 * This method assigns the properties from the bank statement
		 * import to the properties in the transaction.
		 * <P>
		 * Other than the account and memo properties, no properties
		 * will have been set in the transaction  before this method
		 * is called.
		 * 
		 * @param transaction
		 * @param entry1 the entry in the bank account.  The account
		 * 					property will already have been set
		 * @param entry2 the other entry, typically an entry in an
		 * 					income and expense account
		 */
		public void assignPropertyValues(Transaction transaction, Entry entry1, Entry entry2) {
			if (valueDate == null) {
				transaction.setDate(clearedDate);
				entry1.setValuta(clearedDate);
			} else {
				transaction.setDate(valueDate);
				entry1.setValuta(clearedDate);
			}

			entry1.setCheck(check);
			entry1.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), uniqueId);
			
			entry1.setAmount(amount);
			entry2.setAmount(-amount);
		}
		
		@Override
		public String toString() {
			return "[a:"+amount+";n:"+name+"]";
		}
		
		/**
		 * Returns the text that is to be used for pattern matching.
		 * The patterns entered by the user are matched against the text
		 * returned by this method.
		 * 
		 * @return the text which may be empty but must never be null
		 */
		public String getTextToMatch() {
   			String text = "";
   			if (memo != null) {
   				text += "memo=" + memo;
   			}
   			if (type != null) {
   				text += "type=" + type;
   			}
   			if (name != null) {
   				/*
   				 * This is a bit of a hack, but if there is no payee then
   				 * we use the name as the payee.  The reason for this is that
   				 * Citiards.com put the reference under 'name' if OFX but it comes out
   				 * as the payee if QIF.  We want to be able to use the same matching
   				 * rules regardless of which format the user used.
   				 */
   				if (payee == null) {
   	   				text += "payee=" + name;
   				} else {
   					text += "name=" + name;
   				}
   			}
   			if (payee != null) {
   				text += "payee=" + payee;
   			}
   			
   			BigDecimal myAmount = new BigDecimal(amount).scaleByPowerOfTen(-2);
   			text += "amount=" + myAmount;
   			return text;
		}
		
		public String getName() {
			return name;
		}
		
		/**
		 * The memo if no patterns match
		 */
		public String getDefaultMemo() {
			return memo==null? (name==null?payee: name):memo;
		}

		/**
		 * The description if no patterns match
		 */
		public String getDefaultDescription() {
			return payee == null?(memo==null?name:memo):payee;
		}
		
		public Date getClearedDate() {
			return clearedDate;
		}
	}
}
