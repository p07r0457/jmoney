package net.sf.jmoney.serializeddatastore.handlers;


import java.io.File;
import java.util.Map;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.serializeddatastore.IFileDatastore;
import net.sf.jmoney.serializeddatastore.Messages;
import net.sf.jmoney.serializeddatastore.SerializedDatastorePlugin;
import net.sf.jmoney.serializeddatastore.SessionManager;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Shows the given perspective. If no perspective is specified in the
 * parameters, then this opens the perspective selection dialog.
 * 
 * @since 3.1
 */
public final class OpenSessionHandler extends AbstractHandler {

	private class OpenSessionException extends Exception {
		private static final long serialVersionUID = 1L;

		public OpenSessionException() {
		}

		public OpenSessionException(Exception e) {
			super(e);
		}
	}

	/**
	 * True/false value to open the perspective in a new window.
	 */
	private static final String PARAMETER_NEW_WINDOW = "net.sf.jmoney.serializeddatastore.openSession.newWindow"; //$NON-NLS-1$

	public final Object execute(final ExecutionEvent event)
			throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		final Map parameters = event.getParameters();
		final String newWindow = (String) parameters.get(PARAMETER_NEW_WINDOW);

		if (newWindow == null || newWindow.equalsIgnoreCase("false")) { //$NON-NLS-1$
			if (!JMoneyPlugin.getDefault().saveOldSession(window)) {
				/*
				 * Cancelled by user or the save failed.  If the save
				 * failed the user will have been notified, so we
				 * simply exit here.
				 */
				return null;
			}

			IWorkbenchPage activePage = window.getActivePage();
			activePage.close();
		}
		
		try {
			SessionManager newSessionManager = openSession(window);
			if (newSessionManager == null) {
				return null;
			}
			
			// This call needs to be cleaned up, but is still needed
			// to ensure a default currency is set.
			JMoneyPlugin.getDefault().setSessionManager(newSessionManager);

			/*
			 * This call will open the session in the current window if there
			 * is no page (i.e. if we closed the previous page above), or it
			 * will open the page in a new window if there is already a page
			 * in this window (i.e. if we did not close the previous page above).
			 */
			window.openPage(newSessionManager);
		} catch (WorkbenchException e) {
			ErrorDialog.openError(window.getShell(),
					"Open Session failed", e
					.getMessage(), e.getStatus());
			throw new ExecutionException("Session could not be opened.", e); //$NON-NLS-1$
		} catch (OpenSessionException e) {
			MessageDialog.openError(window.getShell(), Messages.OpenSessionAction_ErrorTitle, Messages.OpenSessionAction_ErrorMessage);
			throw new ExecutionException("Session could not be opened.", e); //$NON-NLS-1$
		}
		
		return null;
	}

	/**
	 * 
	 * @param window
	 * @return the session, or null if user canceled
	 * @throws OpenSessionException
	 */
	private SessionManager openSession(IWorkbenchWindow window) throws OpenSessionException {
		FileDialog dialog = new FileDialog(window.getShell());
		dialog.setFilterExtensions(SerializedDatastorePlugin.getFilterExtensions());
		dialog.setFilterNames(SerializedDatastorePlugin.getFilterNames());
		String fileName = dialog.open();

		if (fileName != null) {
			File sessionFile = new File(fileName);

			IConfigurationElement elements[] = SerializedDatastorePlugin.getElements(fileName);

			if (elements.length == 0) {
				/*
				 * The user has entered an extension that is not recognized.
				 */
				 throw new OpenSessionException();
			}

			// TODO: It is possible that multiple plug-ins may
			// use the same file extension.  There are two possible
			// approaches to this: either ask the user which is
			// the format of the file, or we try to load the file
			// using each in turn until one works. 

			// For time being, we simply use the first entry.
			IFileDatastore fileDatastore;
			String fileFormatId;
			try {
				fileDatastore = (IFileDatastore)elements[0].createExecutableExtension("class"); //$NON-NLS-1$
				fileFormatId = elements[0].getDeclaringExtension().getNamespaceIdentifier() + '.' + elements[0].getAttribute("id"); //$NON-NLS-1$
			} catch (CoreException e) {
				throw new OpenSessionException(e);
			}

			SessionManager sessionManager = new SessionManager(fileFormatId, fileDatastore, sessionFile);
			boolean isGoodFileRead = fileDatastore.readSession(sessionFile, sessionManager, window);
			if (!isGoodFileRead) {
				throw new OpenSessionException();
			}
			
			return sessionManager;
		} else {
			return null;
		}
	}
}
