/*
 * Created on Feb 2, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.jmoney.ui.internal.pages.account.capital;

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
	
	//Entry getEntryForDualPurposeFields();
	
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
	
	
}
