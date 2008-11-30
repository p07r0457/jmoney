package net.sf.jmoney.navigator;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.CurrentSessionChangeListener;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.views.TreeNode;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.navigator.CommonNavigator;

public class JMoneyCommonNavigator extends CommonNavigator {
	public static String ID = "net.sf.jmoney.navigationView"; //$NON-NLS-1$
	
	/**
	 * Control for the text that is displayed when no session
	 * is open.
	 */
	private Label noSessionMessage;
	private Composite composite;
	private StackLayout stackLayout;
	
	@Override	
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		init(site);

		// Restore any session that was open when the workbench
		// was last closed.
		JMoneyPlugin.openSession(memento.getChild("session")); //$NON-NLS-1$
	}

	@Override	
	public void saveState(IMemento memento) {
		// Save the information required to re-create this navigation view.

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
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	@Override	
	public void createPartControl(final Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		stackLayout = new StackLayout();
		composite.setLayout(stackLayout);
		
		// Create the control that will be visible if no session is open
		noSessionMessage = new Label(composite, SWT.WRAP);
		noSessionMessage.setText(Messages.JMoneyCommonNavigator_NoSession);
		noSessionMessage.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));

		// Create the tree viewer
		super.createPartControl(composite);
		
		/*
		 * Listen for changes to the model that may affect the tree view.
		 * Changes that affect this view include changes to account names and
		 * new or deleted accounts.
		 */
		updateTopControl();
		JMoneyPlugin.getDefault().addSessionChangeListener(new MyCurrentSessionChangeListener(), getCommonViewer().getControl());
	}
	
	@Override
	protected IAdaptable getInitialInput() {
		return TreeNode.getInvisibleRoot();
	}

	private class MyCurrentSessionChangeListener extends SessionChangeAdapter implements CurrentSessionChangeListener {
		public void sessionReplaced(Session oldSession, Session newSession) {
			// Close all editors
			IWorkbenchWindow window = getSite().getWorkbenchWindow();
			boolean allClosed = window.getActivePage().closeAllEditors(true);
			if (!allClosed) {
				// User hit 'cancel' when prompted to save some
				// unsaved data or perhaps an error occurred.
				// We probably should veto the session replacement,
				// but by this time it is too late.
				// This is not an immediate problem but may become
				// a problem as JMoney is further developed.
			}

			updateTopControl();
			composite.layout(false);
		}
	}

	private void updateTopControl() {
		// Make either the label or the tree control visible, depending
		// on whether the new session is null or not.
		if (JMoneyPlugin.getDefault().getSession() == null) {
			stackLayout.topControl = noSessionMessage;
		} else {
			stackLayout.topControl = getCommonViewer().getControl();
		}
	}
}
