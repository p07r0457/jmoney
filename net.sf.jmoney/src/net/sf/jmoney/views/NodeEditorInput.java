/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Johann Gyger <jgyger@users.sf.net>
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

import java.util.Vector;

import net.sf.jmoney.model2.Account;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class NodeEditorInput implements IEditorInput {

    protected Object nodeObject;
    protected Vector pageListeners;

    /**
     * Create a new account editor input.
     * 
     * @param nodeObject Account on which this input is based
     */
    public NodeEditorInput(Object nodeObject, Vector pageListeners) {
        this.nodeObject = nodeObject;
        this.pageListeners = pageListeners;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorInput#exists()
     */
    public boolean exists() {
        return false;
    }



    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
     */
    public ImageDescriptor getImageDescriptor() {
/*
		if (account instanceof TreeNode) {
			return ((TreeNode)account).getImage();
		} else if (account instanceof CapitalAccount) {
			return Constants.ACCOUNT_ICON;
		} else {
			throw new RuntimeException("");
		}
*/
        return null;

    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorInput#getName()
     */
    public String getName() {
//        return account.getName();
        // TODO: Get name from navigation tree
        if (nodeObject instanceof Account) {
        	return ((Account)nodeObject).getName();
        } else {
        	return "Some Object";
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorInput#getPersistable()
     */
    public IPersistableElement getPersistable() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorInput#getToolTipText()
     */
    public String getToolTipText() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    public Object getAdapter(Class adapter) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == this) return true;

        if (obj instanceof NodeEditorInput) {
            NodeEditorInput input = (NodeEditorInput) obj;
            return nodeObject.equals(input.nodeObject);
        }

        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return nodeObject.hashCode();
    }

    /**
     * @return Returns the account.
     */
    public Object getNode() {
        return nodeObject;
    }

    public Vector getPageListeners() {
        return pageListeners;
    }
}
