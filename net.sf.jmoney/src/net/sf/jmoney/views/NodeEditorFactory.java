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

import net.sf.jmoney.Constants;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Session;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
public class NodeEditorFactory implements IElementFactory {
//	private IWorkbenchWindow window;
	/**
	 * The constructor.
	 */
	public NodeEditorFactory() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
	 * 
	 */
	// While debugging this code, one can inspect the memento data
	// in the file runtime-workspace\.metadata\.plugins\org.eclipse.ui.workbench\workbench.xml.
	public IAdaptable createElement(IMemento memento) {
		// Get the session from the data in the memento.
		Session session = JMoneyPlugin.openSession(memento.getChild("session"));
		
		// See if the node is a TreeNode.
		String nodeId = memento.getString("treeNode");
		if (nodeId != null) {
			TreeNode node = TreeNode.getTreeNode(nodeId);
			return new NodeEditorInput(node, node.getLabel(), node.getImage(), node.getPageFactories(), memento);
		}
		
		// See if the node is an account.
		String fullAccountName = memento.getString("capitalAccount");
		if (fullAccountName != null) {
			System.out.println("editor factory, session = " + session);
			Account account = JMoneyPlugin.getDefault().getSession().getAccountByFullName(fullAccountName);
			if (account instanceof CapitalAccount) {
				TreeNode accountsRootNode = TreeNode.getAccountsRootNode();
				
				// TODO: use same code as in navigation view for label, image etc.
				
				return new NodeEditorInput(account, account.toString(), Constants.ACCOUNT_ICON, TreeNode.getPageListeners(account), memento);
			}
		}
		
		// null indicates the element could not be re-created.
		return null;
	}
}