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
import net.sf.jmoney.IBookkeepingPageFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
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
    	System.out.println("adding pages");
        NodeEditorInput cInput = (NodeEditorInput)this.getEditorInput();
        IMemento memento = cInput.getMemento();
        
        IBookkeepingPage pages [] = new IBookkeepingPage[pageListeners.size()];
        
    	for (int i = 0; i < pageListeners.size(); i++) {
    		TreeNode.PageEntry entry = (TreeNode.PageEntry)pageListeners.get(i);
    		String pageId = (String)entry.getPageId();
    		IBookkeepingPageFactory pageListener = (IBookkeepingPageFactory)entry.getPageFactory();
    		pages[i] = pageListener.createFormPage(this, memento==null?null:memento.getChild(pageId));
    	}
    	
    	cInput.pages = pages;
    }

    public void dispose() {
    	// When the editor is disposed, let the editor input
    	// know that there is no editor open.  This is necessary
    	// because the input is kept around by Eclipse even when the
    	// editor is closed and kept in a Most Recently Used list.
    	// This list is persisted in the workbench memento, but the
    	// editor input must not try to persist the values of the controls
    	// because the controls no longer exist.
    	// We indicate this situation by clearing out the list
    	// of pages.
        NodeEditorInput cInput = (NodeEditorInput)this.getEditorInput();
    	cInput.pages = null;
    	
    	super.dispose();
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
     */
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);

        NodeEditorInput cInput = (NodeEditorInput) input;

        navigationTreeNode = cInput.getNode();
    	pageListeners = cInput.getPageListeners();

       	setPartName(cInput.getName());
		setTitleImage(cInput.getImage());
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