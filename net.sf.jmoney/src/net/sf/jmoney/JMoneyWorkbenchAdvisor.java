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

import java.io.OutputStream;
import java.io.PrintStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/**
 * @author Nigel
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class JMoneyWorkbenchAdvisor extends WorkbenchAdvisor {

	public String getInitialWindowPerspectiveId() {
		// This method will not be called if
		// org.eclipse.ui/defaultPerspectiveId is set in
		// plugin_customization.ini in org.eclipse.platform
		// plugin. (This seems to be a design flaw - if
		// the application wants this perspective then it
		// would stupid to use some other).
		// TODO sort this out
		return "net.sf.jmoney.JMoneyPerspective";
	}

    /* (non-Javadoc)
     * @see org.eclipse.ui.application.WorkbenchAdvisor#createWorkbenchWindowAdvisor(org.eclipse.ui.application.IWorkbenchWindowConfigurer)
     */
    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        return  new JMoneyWorkbenchWindowAdvisor(configurer);
    }
    
		public void initialize(IWorkbenchConfigurer configurer) {
			super.initialize(configurer);

			// Turn on support for the saving and restoring of
			// view states through IMemento interfaces.
			configurer.setSaveAndRestore(true);
		}

		public boolean preWindowShellClose(IWorkbenchWindowConfigurer configurer) {
			// If a session is open, ensure we have all the information we
			// need to close it.  Some datastores need additional information
			// to save the session.  For example the serialized XML datastore
			// requires a file name which will not have been requested from the
			// user if the datastore has not yet been saved.
			
			// This call must be done here for two reasons.
			// 1. It ensures that the session data can be saved.
			// 2. The navigation view saves, as part of its state,
			//    the datastore which was open when the workbench was
			//    last shut down, allowing it to open the same session
			//    when the workbench is next opened.  In order to do this,
			//    the navigation view saves the session details.

			return JMoneyPlugin.getDefault().saveOldSession(configurer.getWindow());
		}
		
		/**
		 * Display a window with an error message when an exception take place
		 * inside the code of JMoney.
		 * @author Faucheux
		 */
		public void eventLoopException(Throwable exception) {
		    super.eventLoopException(exception);
	        Shell shell = new Shell(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
	        String errorString = ExceptionToString(exception);
	        // JobStatus jobStatus = new JobStatus(Status.ERROR, (Job) null,  "Following error has taken place: " + exception.getLocalizedMessage());
	        // ErrorDialog dialog = new ErrorDialog (shell, "An error occured", "Following error has taken place: " + exception.getLocalizedMessage(), jobStatus, Status.ERROR);

	        // Preparation of the dialog box
	        Composite transactionArea = new Composite(shell, 0);
	        GridLayout sectionLayout = new GridLayout();
	        sectionLayout.numColumns = 1;
	        sectionLayout.marginHeight = 0;
	        sectionLayout.marginWidth = 0;
	        shell.setLayout(sectionLayout);

	        GridLayout transactionAreaLayout = new GridLayout(1, false);
	        transactionArea.setLayout(transactionAreaLayout);
	        transactionArea.setLayoutData(new GridData(GridData.FILL_BOTH));
	        
	        Label label = (new Label(transactionArea, SWT.LEFT));
	        label.setText(errorString);
	        label.setEnabled(true);
	        shell.pack();
	        shell.open();
		}
		
		/**
		 * Create a String wich documents the exception.
		 * Sorry, I haven't found another way...
		 * @author Faucheux
		 */
		private String ExceptionToString (Throwable e) {
		    final int STRING_MAX_SIZE = 5000;
		    final byte[] s = new byte[STRING_MAX_SIZE];
		    PrintStream p = new PrintStream (new OutputStream() {
		        int pointer = 0;
		        public void write (int character) {
		            if (pointer < STRING_MAX_SIZE) {
		                s[pointer++] = (byte) character;
		            }
		        }
		        public void close () {
		            s[pointer++] = '\0';
		        }
		    });
		    e.printStackTrace(p);
		    p.close();
		    
		    return new String(s);
		}
}
