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

package net.sf.jmoney.serializeddatastore;

import java.io.File;

import net.sf.jmoney.model2.ISessionManagement;
import net.sf.jmoney.model2.Session;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Holds the fields that will be saved in a file.
 */
public class SessionManagementImpl implements ISessionManagement {

    protected File sessionFile = null;

    protected transient boolean modified = false;

	private Session session;

	/** This version is used to solve a 'chicken and egg' problem.
	 * The session object constructor may need to SessionManagement object.
	 * Therefore we allow a SessionManagement object to be created
	 * first and the Session object to be set later.
	 * 
	 * @param sessionFile
	 */
	public SessionManagementImpl(File sessionFile) {
		this.sessionFile = sessionFile;
		this.session = null;
	}
	
	/**
	 * Used for two-part construction.
	 */
	public void setSession(Session session) {
		this.session = session;
	}
	
	public Session getSession() {
		return session;
	}
	
    public File getFile() {
        return sessionFile;
    }
    
    public void setFile(File file) {
        this.sessionFile = file;
        
        // The brief description of this session contains the file name, so we must
        // fire a change so views that show this session description are updated.
        // FIXME: Title is not updated because this is commented out.
/*
        fireEvent(
        	new ISessionChangeFirer() {
        		public void fire(SessionChangeListener listener) {
        			listener.sessionPropertyChange("briefDescription", null, getBriefDescription());
        		}
       		});
*/       		
    }

    boolean isModified() {
        return modified;
    }

    /**
     * This plug-in needs to know if a session has been modified so
     * that it knows whether to save the session.  This method must
     * be called with a 'true' value whenever the session is modified and
     * must be called with a 'false' value whenever the session is saved.
     */
    void setModified(boolean modified) {
        this.modified = modified;
    }



    public boolean canClose(IWorkbenchWindow window) {
        if (isModified()) {
            return SerializedDatastorePlugin.getDefault().requestSave(this, window);
        } else {
            return true;
        }
    }

    public void close() {
        // There is nothing to do here.  No files, connections or other resources
        // are kept open so there is nothing to close.
    }
    
    public String getBriefDescription() {
        if (sessionFile == null) {
            return null;
        } else {
            return sessionFile.getName();
        }
    }

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.ISessionManagement#getFactoryId()
	 */
	public String getFactoryId() {
		return "net.sf.jmoney.serializeddatastore.factoryid";
	}

	private IPersistableElement persistableElement 
	= new IPersistableElement() {
		public String getFactoryId() {
			return "net.sf.jmoney.serializeddatastore.factoryid";
		}
		public void saveState(IMemento memento) {
			// The session must have been saved by now, because
			// JMoney will not closed until the Session object says
			// it is ok to close, and the Session object will not
			// say it is ok to close unless it has available a file
			// name to which the session can be saved.  (It will ask
			// the user if the session was created using the New menu).
			memento.putString("fileName", sessionFile.getPath());
		}
	};
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IPersistableElement.class) {
			return persistableElement;
		}
		return null;
	}

}
