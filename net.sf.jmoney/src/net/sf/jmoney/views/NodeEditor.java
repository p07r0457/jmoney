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

import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.IBookkeepingPageListener;
import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class NodeEditor extends FormEditor {

    protected Object navigationTreeNode;
    protected Vector pageListeners;

    /* (non-Javadoc)
     * @see org.eclipse.ui.forms.editor.FormEditor#addPages()
     */
    protected void addPages() {

        try {
        	for (int i = 0; i < pageListeners.size(); i++) {
        		if (pageListeners.get(i) instanceof IBookkeepingPage) {
        			// The extension listener is a new-style editor page.
                    IBookkeepingPage pageListener = (IBookkeepingPage)pageListeners.get(i);

                    addPage(pageListener.createFormPage(this));
        		} else if (pageListeners.get(i) instanceof IBookkeepingPageListener) {
        			// The extension listener is an old style control originally
        			// designed to be created in the 'folder' view.
        			// We use an IFormPage implementation that wraps these old
        			// style controls.
                    IBookkeepingPageListener pageListener = (IBookkeepingPageListener)pageListeners.get(i);

                    // TODO: We can remove all this if we convert over to the 'new style' pages.
                	String name;
                    if (pageListener.getClass().getName().equals("net.sf.jmoney.accountentriespanel.EntryListPage")) {
                        name = "Old Style Entries";
                    } else if (pageListener.getClass().getName().equals("net.sf.jmoney.bookkeepingPages.AccountPropertiesPages")) {
                        name = "Properties";
                    } else {
                        name = "Old Style Page Contents";
                    }
                	
                    addPage(new SectionlessPageWrapper(this, pageListener, name));
        		}    
        	}
        } catch (PartInitException e) {
            JMoneyPlugin.log(e);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
     */
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);

        NodeEditorInput cInput = (NodeEditorInput) input;

        navigationTreeNode = cInput.getNode();
    	pageListeners = cInput.getPageListeners();

        // TODO: Get name from navigation tree label provider?
       	setPartName(navigationTreeNode.toString());

/*       	
		if (obj instanceof TreeNode) {
			return ((TreeNode)obj).getImage();
		} else if (obj instanceof CapitalAccount) {
			return Constants.ACCOUNT_ICON;
		} else {
			throw new RuntimeException("");
		}
*/
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void doSave(IProgressMonitor monitor) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.ISaveablePart#doSaveAs()
     */
    public void doSaveAs() {
        throw new RuntimeException("Illegal invocation");
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
     */
    public boolean isSaveAsAllowed() {
        return false;
    }

	/**
	 * @return
	 */
	public Object getSelectedObject() {
		return navigationTreeNode;
	}
}