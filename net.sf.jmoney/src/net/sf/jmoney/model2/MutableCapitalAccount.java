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

/**
 *
 * @author  Nigel
 */
public interface MutableCapitalAccount extends CapitalAccount, MutableAccount {
    /**
     * @param aCurrency the default currency for this account.
     */
    void setCurrency(Currency aCurrency);
    
    /**
     * @param aBank the name of this account.
     */
    void setBank(String aBank);
    
    /**
     * Sets the account number of this account.
     * @param anAccountNumber the account number
     */
    void setAccountNumber(String anAccountNumber);
    
    /**
     * Sets the initial balance of this account.
     * @param s the start balance
     */
    void setStartBalance(long s);
    
    /**
     * @param m the minimal balance which may be null.
     */
    void setMinBalance(Long m);
    
    /**
     * @param anAbbrevation the abbrevation of this account.
     */
    void setAbbreviation(String anAbbreviation);
    
    /**
     * @param aComment the comment of this account.
     */
    void setComment(String aComment);
    
    CapitalAccount commit();
}
