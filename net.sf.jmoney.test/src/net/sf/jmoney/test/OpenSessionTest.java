/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2005 Johann Gyger <jgyger@users.sf.net>
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
package net.sf.jmoney.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;
import net.sf.jmoney.serializeddatastore.IFileDatastore;
import net.sf.jmoney.serializeddatastore.SerializedDatastorePlugin;
import net.sf.jmoney.serializeddatastore.SessionManager;
import net.sf.jmoney.serializeddatastore.formats.JMoneyXmlFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * Tests regarding opening JMoney session files.
 * 
 * @author Johann Gyger
 */
public class OpenSessionTest extends TestCase {

    /**
     * Test if a JMoney file with the properties below can be opened:
     * - Old format (version 0.4.5 or prior)
     * - Empty (minimal file contents)
     * - Uncompressed
     * 
     * @throws CoreException
     * @throws IOException
     */
    public void testOldEmptyUncompressed() throws IOException, CoreException {
        File file = getSessionFile("old_empty_session.xml");
        JMoneyXmlFormat reader = new JMoneyXmlFormat();
        SessionManager manager = new SessionManager("net.sf.jmoney.serializeddatastore.xmlFormat", reader, file);
        reader.readSessionQuietly(file, manager, null);
        assertNotNull(manager);
        assertNotNull(manager.getSession());
    }

    /**
     * Test if a JMoney file with the properties below can be opened:
     * - Old format (version 0.4.5 or prior)
     * - Empty (minimal file contents)
     * - Compressed
     * 
     * @throws CoreException
     * @throws IOException
     */
    public void testOldEmptyCompressed() throws IOException, CoreException {
        File file = getSessionFile("old_empty_session.jmx");
        JMoneyXmlFormat reader = new JMoneyXmlFormat();
        SessionManager manager = new SessionManager("net.sf.jmoney.serializeddatastore.jmxFormat", reader, file);
        reader.readSessionQuietly(file, manager, null);
        assertNotNull(manager);
        assertNotNull(manager.getSession());
    }

    /**
     * Test if a JMoney file with the properties below can be opened:
     * - New format
     * - Empty (minimal file contents)
     * - Uncompressed
     * 
     * @throws CoreException
     * @throws IOException
     */
    public void testNewEmptyUncompressed() throws IOException, CoreException {
        File file = getSessionFile("new_empty_session.xml");
        JMoneyXmlFormat reader = new JMoneyXmlFormat();
        SessionManager manager = new SessionManager("net.sf.jmoney.serializeddatastore.xmlFormat", reader, file);
        reader.readSessionQuietly(file, manager, null);
        assertNotNull(manager);
        assertNotNull(manager.getSession());
    }

    /**
     * Test if a JMoney file with the properties below can be opened:
     * - New format
     * - Empty (minimal file contents)
     * - Compressed
     * 
     * @throws CoreException
     * @throws IOException
     */
    public void testNewEmptyCompressed() throws IOException, CoreException {
        File file = getSessionFile("new_empty_session.jmx");
        JMoneyXmlFormat reader = new JMoneyXmlFormat();
        SessionManager manager = new SessionManager("net.sf.jmoney.serializeddatastore.jmxFormat", reader, file);
        reader.readSessionQuietly(file, manager, null);
        assertNotNull(manager);
        assertNotNull(manager.getSession());
    }

    protected File getSessionFile(String filename) throws IOException {
        Bundle bundle = Platform.getBundle("net.sf.jmoney.test");
        URL url = bundle.getEntry("resources/" + filename);
        return new File(Platform.asLocalURL(url).getFile());
    }

}