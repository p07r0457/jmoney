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
import java.io.FileNotFoundException;
import java.io.IOException;

import net.sf.jmoney.model2.ISessionChangeFirer;
import net.sf.jmoney.model2.ISessionManagement;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeListener;

import org.eclipse.core.runtime.CoreException;
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

	public SessionManagementImpl(File sessionFile, Session session) {
		this.sessionFile = sessionFile;
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

    public void setModified(boolean m) {
        if (modified == m)
            return;
        modified = m;
//        changeSupport.firePropertyChange("modified", !m, m);
    }

    /**
     * Other class implementations in this package may call this method
     * when classes further down inside this session are modified.
     */
    // TODO: Change to package only access when mutable stuff moved to impl.
    public void modified() {
        setModified(true);
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
