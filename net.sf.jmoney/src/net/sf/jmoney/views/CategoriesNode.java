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

package net.sf.jmoney.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;

/**
 * @author Administrateur
 */
public class CategoriesNode implements IDynamicTreeNode {

    public static final String ID = "net.sf.jmoney.categoriesNode"; //$NON-NLS-1$

    private DatastoreManager sessionManager;
    
    public CategoriesNode(DatastoreManager sessionManager) {
    	this.sessionManager = sessionManager;
    }
    
	public boolean hasChildren() {
		return sessionManager.getSession().getIncomeExpenseAccountIterator().hasNext();
	}

	public Collection<Object> getChildren() {
		ArrayList<Object> children = new ArrayList<Object>();
		for (Iterator<IncomeExpenseAccount> iter = sessionManager.getSession().getIncomeExpenseAccountIterator(); iter.hasNext(); ) {
			IncomeExpenseAccount account = iter.next();
			children.add(account);
		}
		return children;
	}

	public Session getSession() {
		return sessionManager.getSession();
	}
}

