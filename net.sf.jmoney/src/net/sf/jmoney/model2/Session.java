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
import java.util.Vector;

import org.eclipse.core.runtime.IAdaptable;

/**
 * Holds the fields that will be saved in a file.
 */
public interface Session extends IExtendableObject {

    Currency getDefaultCurrency();
    
    void setDefaultCurrency(Currency currency);
    
    Iterator getCommodityIterator();

    Iterator getAccountIterator();
    
    Iterator getCapitalAccountIterator();
   
    public Iterator getCapitalAccountIteratorByLevel(int level);

    Iterator getIncomeExpenseAccountIterator();
    
    Iterator getTransactionIterator();
    
	Account createAccount(PropertySet actualPropertySet);

    MutableTransaction createNewTransaction();

    boolean removeCommodity(Commodity commodity);
    
    boolean removeAccount(Account account);
    
    boolean removeTransaction(Transaction transaction);

//  Object[] getAvailableCurrencies();
    Currency getCurrencyForCode(String code);
    
    void addSessionChangeListener(SessionChangeListener listener);
    
    void removeSessionChangeListener(SessionChangeListener listener);
    
    void addSessionChangeFirerListener(SessionChangeFirerListener listener);
    
    void removeSessionChangeFirerListener(SessionChangeFirerListener listener);

    // This should have package protection.
    // However, this interface should be merged with the implementation
    // so this issue will then go away.
    void fireEvent(ISessionChangeFirer firer);

    /**
     * get all Accounts from level 0 (accounts without any parent) until the given level  
     * @author Faucheux
     */
    Vector getAccountsUntilLevel (int level);
    
	Account getAccountByFullName(String name);

}
