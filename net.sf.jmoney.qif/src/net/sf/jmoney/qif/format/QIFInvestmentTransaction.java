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
public interface QIFInvestmentTransaction{

	public static final char ACTION = 'N';

	public static final char SECURITY = 'Y';

	public static final char PRICE = 'I';

	public static final char QUANTITY = 'Q'; // Number of shares or split ratio

	public static final char TRANSACTION_AMOUNT = 'T';

	public static final char COMMISSION = 'O';

	public static final char ACCOUNT_FOR_TRANSFER = 'L';

	public static final char AMOUNT_TRANSFERRED = '$';

}
