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

package net.sf.jmoney.pages.entries;

import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Transaction;

/**
 * @author Nigel
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface IDisplayableItem {
	
	/**
	 * If transaction fields are to be shown (i.e. if this
	 * is a DisplayableTransaction row), return the Transaction.
	 */
	Transaction getTransactionForTransactionFields();
	
	/**
	 * If fields for capital account are to be shown,
	 * return this...
	 * @return
	 */
	Entry getEntryForAccountFields();
	
	Entry getEntryForOtherFields();
	
	Entry getEntryForCommon1Fields();

	Entry getEntryForCommon2Fields();
	
	long getAmount();
	
	/**
	 *  
	 * 
	 * 
	 * @return true if the transaction fields (the date and any
	 * 		extension properties added to the transaction) are to 
	 * 		be blank on this row
	 */
	boolean blankTransactionFields();
	
	/**
	 * 
	 * 
	 * 
	 * @return true if this row causes the balance
	 * 		(as shown in the balance column) to be a different
	 * 		value from the previous balance
	 */
	boolean isBalanceAffected();
	
	/**
	 * This method is valid only if isBalanceAffected
	 * returns true.
	 * 
	 * @return
	 */
	long getBalance();
	
	// The following two methods are used for displaying the
	// entry section at the bottom.  This needs some more work.
	
	/**
	 * @return
	 */
	Entry getEntryInAccount();
	
	/**
	 * @return
	 */
	Entry getEntryForThisRow();
}
