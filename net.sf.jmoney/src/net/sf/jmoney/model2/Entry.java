/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

import java.util.Date;

/**
 * The data model for an entry.
 */
public interface Entry extends IMutableExtendableObject {
	
	/**
	 * Returns the transaction.
	 */
	Transaxion getTransaxion();
	
	/**
	 * Indicate is this entry object may be modified by
	 * calling the setters.
	 */
	boolean isMutable();
	
	/**
	 * If this entry is a mutable entry then return the original
	 * entry which is being edited, or, if this mutable entry is
	 * a new entry that has never been committed to the datastore
	 * then return null.
	 *
	 * @exception RuntimeException This entry is not mutable.
	 */
	Entry getOriginalEntry();
	
	/**
	 * Returns the creation.
	 */
	long getCreation();
	
	/**
	 * Returns the check.
	 */
	String getCheck();
	
	/**
	 * Returns the valuta.
	 */
	Date getValuta();
	
	/**
	 * Returns the description.
	 */
	String getDescription();
	
	/**
	 * Returns the account.
	 */
	Account getAccount();
	
	// TODO: should really be in a utility class.
	String getFullAccountName();
	
	/**
	 * Returns the amount.
	 */
	long getAmount();
	
	/**
	 * Returns the memo.
	 */
	String getMemo();
	
	/**
	 * Sets the creation.
	 */
	void setCreation(long aCreation);
	
	/**
	 * Sets the check.
	 */
	void setCheck(String aCheck);
	
	/**
	 * Sets the valuta.
	 */
	void setValuta(Date aValuta);
	
	/**
	 * Sets the description.
	 */
	void setDescription(String aDescription);
	
	/**
	 * Sets the account.
	 */
	void setAccount(Account account);
	
	/**
	 * Sets the amount.
	 */
	void setAmount(long anAmount);
	
	/**
	 * Sets the memo.
	 */
	void setMemo(String aMemo);
}
