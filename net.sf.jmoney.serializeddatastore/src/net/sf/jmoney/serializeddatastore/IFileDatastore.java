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
package net.sf.jmoney.serializeddatastore;

import java.io.File;

import org.eclipse.ui.IWorkbenchWindow;

/**
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 *
 * @author Nigel Westbury
 */
public interface IFileDatastore {

	/**
	 * Read data from the file
	 */
	void readSession(File sessionFile, SessionManager sessionManager, IWorkbenchWindow window);
    
    /**
     * Write data to a file
     */
    void writeSession(SessionManager sessionManager, File sessionFile, IWorkbenchWindow window);
}
