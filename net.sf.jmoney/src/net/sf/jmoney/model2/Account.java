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

import java.util.Iterator;

/**
 * The base interface for accounts.  Accounts include both income and
 * expense categories and capital accounts such as bank and credit card
 * accounts.
 */
public interface Account extends Comparable, IExtendableObject {
	
	/**
	 * @return the name of the account.
	 */
	String getName();
	
	Account getParent();
	
	Iterator getSubAccountIterator();
	
	// Helper methods.  These methods add useful function but these methods
	// do not depend on the implementation so are implemented here.
	
	/**
	 * @return the full qualified name of the account.
	 */
	String getFullAccountName();
}
