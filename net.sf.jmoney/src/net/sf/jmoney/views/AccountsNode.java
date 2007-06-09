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
import java.util.Iterator;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Session;

/**
 * @author Administrateur
 */
class AccountsNode extends TreeNode {

	public AccountsNode(TreeNode parent) {
		super("net.sf.jmoney.capitalAccounts", JMoneyPlugin.getResourceString("NavigationTreeModel.accounts"), JMoneyPlugin.createImageDescriptor("icons/accounts.gif"), null, 100);
	}
	
	@Override
	public boolean hasChildren() {
		Session session = JMoneyPlugin.getDefault().getSession();
		if (session != null) {
			return session.getCapitalAccountIterator().hasNext();
		} else {
			return false;
		}
	}

	@Override
	public Object[] getChildren() {
		Session session = JMoneyPlugin.getDefault().getSession();
		ArrayList<Object> children = new ArrayList<Object>();
		if (session != null) {
			for (Iterator<CapitalAccount> iter = session.getCapitalAccountIterator(); iter.hasNext(); ) {
				CapitalAccount account = iter.next();
				children.add(account);
			}
		}
		return children.toArray();
	}
}

