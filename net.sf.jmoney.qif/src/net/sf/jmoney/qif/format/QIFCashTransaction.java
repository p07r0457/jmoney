/*
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2005 Tom Drummond
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.jmoney.qif.format;

/**
 * @author Tom Drummond 
 */
public interface QIFCashTransaction {

	/**
	 * Char indicating line contains the date. 'D'
	 */
	public static final char DATE = 'D';

	/**
	 * Char indicating line contains the total amount. 'T'
	 */
	public static final char TOTAL = 'T';

	/**
	 * Char indicating line specifies whether the transaction has
	 * been cleared. 'C'
	 */
	public static final char CLEARED = 'C';

	/**
	 * Char indicating line contains the check number. 'N'
	 */
	public static final char NUMBER = 'N';

	/**
	 * Char indicating line contains the payee. 'P'
	 */
	public static final char PAYEE = 'P';

	/**
	 * Char indicating line contains a memo. 'M'
	 */
	public static final char MEMO = 'M';

	/**
	 * Char indicating line contains the address of the payee. 'A'
	 */
	public static final char ADDRESS = 'A';

	/**
	 * Char indicating line contains the category, transfer account or the class
	 * in a class list. 'L'
	 */
	public static final char CATEGORY = 'L'; // (Category/Subcategory/Transfer/Class)

	/**
	 * Char indicating the end of the transaction. '^'
	 */
	public static final char END = '^';

	/**
	 * Comment for <code>TRANSFER_START</code>
	 */
	public static final char TRANSFER_START = '[';

	/**
	 * Comment for <code>TRANSFER_END</code>
	 */
	public static final char TRANSFER_END = ']';

}
