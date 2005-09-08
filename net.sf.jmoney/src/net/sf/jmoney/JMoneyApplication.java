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

package net.sf.jmoney;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.WorkbenchAdvisor;

/**
 * The "main program" for JMoney RCP.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class JMoneyApplication implements IPlatformRunnable {

    public Object run(Object args) {
        Display display = PlatformUI.createDisplay();

        try {
            Shell shell = new Shell(display, SWT.ON_TOP);

            try {
                // This is the place where application data is stored.
                initializeInstanceLocation(shell);
            } finally {
                if (shell != null)
                    shell.dispose();
            }

            WorkbenchAdvisor workbenchAdvisor = new JMoneyWorkbenchAdvisor();
            int returnCode = PlatformUI.createAndRunWorkbench(display, workbenchAdvisor);
            if (returnCode == PlatformUI.RETURN_RESTART) {
                return IPlatformRunnable.EXIT_RESTART;
            } else {
                return IPlatformRunnable.EXIT_OK;
            }
        } finally {
            if (display != null)
                display.dispose();
        }
    }

    private void initializeInstanceLocation(Shell shell) {
        Location instanceLoc = Platform.getInstanceLocation();

        // Launch argument -data with a valid path has been set.
        // We leave it just as it is.
        if (instanceLoc.isSet()) {
            return;
        }

        // Determine an OS dependent path.
        String os = Platform.getOS();
        String pathname = null;
        if (Platform.OS_WIN32.equals(os)) {
            pathname = "Application Data" + File.separator + "JMoney";
        } else if (Platform.OS_MACOSX.equals(os)) {
            pathname = "Library" + File.separator + "JMoney";
        } else if (Platform.OS_LINUX.equals(os)) {
            pathname = ".jmoney";
        } else {
            // No special location, use default instance location.
            return;
        }

        // Prepend user directory
        pathname = System.getProperty("user.home") + File.separator + pathname;

        // Make sure that the directory exists.
        File workspace = new File(pathname);
        if (!workspace.exists())
            workspace.mkdir();

        // We can't use File.toURL() due to Eclipse bug 54081.
        pathname = workspace.getAbsolutePath().replace(File.separatorChar, '/');
        try {
            URL url = new URL("file", null, pathname);
            // TODO Lock workspace
            instanceLoc.setURL(url, false);
        } catch (MalformedURLException e) {
            MessageDialog.openError(shell, "Invalid Workspace", "Invalid pathname for JMoney workspace: " + pathname);
        }
    }

}
