
package net.sf.jmoney.views;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import net.sf.jmoney.Constants;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Session;

import org.eclipse.swt.graphics.Image;

/**
 * @author Administrateur
 */
// TODO: Should the list of accounts be cached by the TreeNode object?
// Or should we change this code and send the request to the datastore each time the tree view requests
// a list of accounts or sub-accounts?
public class AccountNode extends TreeNode {
    
    public Account account;
    static public HashMap hashAccountsNode = new HashMap(); 

    
	public AccountNode(String name, Image image, TreeNode parent) {
		super(name, image, parent);
		children = new ArrayList();
		this.account = null;
		//TODO: Faucheux -- always needed? 
		//setSession(JMoneyPlugin.getDefault().getSession());
	}
	
	public AccountNode (Account account, AccountNode rootParent) {
		super(account.getName(), (Image) Constants.ACCOUNT_ICON, (TreeNode) null);
		children = new ArrayList();
		this.account = account;
		// relies the account with its parent
		System.out.println("Je cherche " + account.getParent());
		AccountNode parentNode = (AccountNode) hashAccountsNode.get(account.getParent());
		if (parentNode == null) parentNode = rootParent;
		this.setParent(parentNode);
		parentNode.addChild(this);
		System.out.println("Je rentre " + account);
		hashAccountsNode.put(account, this);
	    System.out.println("A:" + hashAccountsNode.size());
	}
	
	public void removeAccount (Account account) {
		AccountNode accountNode = (AccountNode) hashAccountsNode.get(account);
		if (accountNode != null) {
		    accountNode.getParent().removeChild(accountNode);
		    accountNode.setParent(null);
		    hashAccountsNode.remove(account);
		}
	    System.out.println("D:" + hashAccountsNode.size());
	}
	
	public void setSession(Session session) {
		// Initialize with list of top level accounts from the session.
	    children.clear();
		if (session != null) {
			for (Iterator iter = session.getCapitalAccountIteratorByLevel(0); iter.hasNext(); ) {
				CapitalAccount account = (CapitalAccount)iter.next();
				children.add(account);
			}
		}
	}
	
}
