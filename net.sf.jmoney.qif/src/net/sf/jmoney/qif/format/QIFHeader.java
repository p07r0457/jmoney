/*
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2005 Tom Drummond
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
 */

package net.sf.jmoney.qif.format;

/**
 * A set of reference strings for use in reading & writing QIF File Headers
 * 
 * @author Tom Drummond 
 */
public interface QIFHeader {
	//  Basic Strings
	public static final char _START = '!';
	public static final char _SEPARATOR = ':';

	/** Account List. "!Account".*/
	public static final String ACCOUNT = "!Account";
	
	/** Capital Account.  "!CapitalAccount" */
	public static final String CAPITAL = "!CapitalAccount";

	// Account Types
	/** Bank account. "!Type:Bank" */
	public static final String ACCT_BANK = "!Type:Bank";

	/** Cash account.  "!Type:Cash" */
	public static final String ACCT_CASH = "!Type:Cash";

	/** Credit card account. "!Type:CCard" */
	public static final String ACCT_CARD = "!Type:CCard";

	/** Investment account. "!Type:Invst" */
	public static final String ACCT_INVST = "!Type:Invst";

	/** Asset account. "!Type:Oth A" */
	public static final String ACCT_ASSET = "!Type:Oth A";

	/** Liability account. "!Type:Oth L" */
	public static final String ACCT_LIAB = "!Type:Oth L";

	/** Microsoft Money 97 German edition. "!Type:Bar" */
	public static final String ACCT_MS97 = "!Type:Bar";
	
	// Non Account types
	/** Category list. "!Type:Cat" */
	public static final String ACCT_CAT = "!Type:Cat";

	/** Class list. "!Type:Class" */
	public static final String ACCT_CLASS = "!Type:Class";

	/** Memorized transaction list. "!Type:Memorized" */
	public static final String MEMORIZED = "!Type:Memorized";

	// Options	
	/** Option: Force import all. "!Option:AllXfr". */
	public static final String OPTION_ALL = "!Option:AllXfr";

}
