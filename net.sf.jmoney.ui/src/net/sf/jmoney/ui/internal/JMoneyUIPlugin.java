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
package net.sf.jmoney.ui.internal;

import java.net.MalformedURLException;import java.net.URL;import java.util.MissingResourceException;import java.util.ResourceBundle;import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;import org.eclipse.swt.graphics.Color;import org.eclipse.swt.widgets.Display;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPage;import org.eclipse.ui.IWorkbenchWindow;import org.eclipse.ui.PlatformUI;import org.eclipse.ui.plugin.AbstractUIPlugin;import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class JMoneyUIPlugin extends AbstractUIPlugin {

    public static final boolean DEBUG = "true".equalsIgnoreCase(Platform.getDebugOption("net.sf.jmoney.qif/debug"));

	private static JMoneyUIPlugin plugin;

	private ResourceBundle resourceBundle;
	private Color yellow;	private Color green;	
	/**
	 * The constructor.
	 */
	public JMoneyUIPlugin() {
		super();
		plugin = this;
		try {
			resourceBundle = ResourceBundle.getBundle("net.sf.jmoney.ui.internal.Language");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * Returns the shared instance.
	 */
	public static JMoneyUIPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = JMoneyUIPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * @return Active workbench window
	 */
    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench == null) return null;
        return workbench.getActiveWorkbenchWindow();
    }

    /**
     * @return Active page
     */
    public static IWorkbenchPage getActivePage() {
        IWorkbenchWindow window = getActiveWorkbenchWindow();
        if (window == null) return null;
        return window.getActivePage();
    }

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		Display display = getActiveWorkbenchWindow().getShell().getDisplay();				// A useful site for picking colors: http://www.drpeterjones.com/colorcalc/		yellow = new Color(display, 255, 255, 150);		green = new Color(display, 200, 255, 200);	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {		yellow.dispose();		green.dispose();		
		super.stop(context);	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}	public static ImageDescriptor createImageDescriptor(String name) {		String iconPath = "";		try {			URL installURL = getDefault().getBundle().getEntry("/");			URL url = new URL(installURL, iconPath + name);			return ImageDescriptor.createFromURL(url);		} catch (MalformedURLException e) {			// should not happen			return ImageDescriptor.getMissingImageDescriptor();		}	}	/**	 * @return	 */	public Color getGreenColor() {		return green;	}	/**	 * @return	 */	public Color getYellowColor() {		return yellow;	}

}
