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

import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Session;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * @author Administrateur
 */
// TODO: Should the list of accounts be cached by the TreeNode object?
// Or should we change this code and send the request to the datastore each time the tree view requests
// a list of accounts or sub-accounts?
class AccountsNode extends TreeNode {
	public AccountsNode(String label, ImageDescriptor imageDescriptor, TreeNode parent) {
		super("net.sf.jmoney.capitalAccounts", label, imageDescriptor, null, 100);
		
		setParent(parent);
	}
	
	public void setSession(Session session) {
		// Initialize with list of top level accounts from the session.
		if (children == null) {
			children = new ArrayList<Object>();
		} else {
			children.clear();
		}
		if (session != null) {
			for (Iterator<CapitalAccount> iter = session.getCapitalAccountIterator(); iter.hasNext(); ) {
				CapitalAccount account = iter.next();
				children.add(account);
			}
		}
	}
}

