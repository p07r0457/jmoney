package net.sf.jmoney.handlers;

import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.PageEntry;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.views.NodeEditorInput;
import net.sf.jmoney.views.TreeNode;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

public class SessionEditorInput implements IEditorInput, IPersistableElement {

    protected IMemento memento;
    
	public SessionEditorInput(Session session, IMemento memento) {
		this.memento = memento;
	}

	public boolean exists() {
		// TODO: Should return false if session not open?
		return true;
	}

	public ImageDescriptor getImageDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return Messages.SessionEditorInput_Name;
	}

	/* (non-Javadoc)
     * @see org.eclipse.ui.IEditorInput#getPersistable()
     */
    public IPersistableElement getPersistable() {
        // This class implements the IPersistableElement
    	// methods, so return a pointer to this object.
        return this;
    }

	public String getToolTipText() {
		// TODO Auto-generated method stub
		return Messages.SessionEditorInput_ToolTipText;
	}

	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override	
    public boolean equals(Object obj) {
        if (obj == this) return true;

        if (obj instanceof SessionEditorInput) {
        	SessionEditorInput input = (SessionEditorInput) obj;
        	return true; // TODO: check same session?
//            return nodeObject.equals(input.nodeObject);
        }

        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override	
    public int hashCode() {
    	// TODO: look at session?
        return SessionEditorInput.class.hashCode();
    }

    /**
     * @return The input object for this editor
     */
    public Session getSession() {
        return null; //session;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPersistableElement#getFactoryId()
	 */
	public String getFactoryId() {
		return "net.sf.jmoney.sessionEditor"; //$NON-NLS-1$
	}

	/**
	 * @param memento
	 */
	public void saveState(IMemento memento) {
		// Views and editors can be restored in any order, and
		// all must be able to be restored independently of the
		// others.  Therefore the session memento must be saved in the memento 
		// for every view and editor.
		
    	// Save the details of the session.
    	DatastoreManager sessionManager = JMoneyPlugin.getDefault().getSessionManager();
		if (sessionManager != null) {
			IMemento sessionMemento = memento.createChild("session"); //$NON-NLS-1$
			IPersistableElement pe = (IPersistableElement)sessionManager.getAdapter(IPersistableElement.class);
			sessionMemento.putString("currentSessionFactoryId", pe.getFactoryId()); //$NON-NLS-1$
			pe.saveState(sessionMemento.createChild("currentSession")); //$NON-NLS-1$
		}
		
	}

	/**
	 * @return
	 */
	public IMemento getMemento() {
		return memento;
	}
}
