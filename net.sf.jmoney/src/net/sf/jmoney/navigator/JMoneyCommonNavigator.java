package net.sf.jmoney.navigator;

import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.CurrentSessionChangeListener;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.PageEntry;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.views.TreeNode;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
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
	public static String ID = "net.sf.jmoney.navigationView";
	
	/**
	 * Control for the text that is displayed when no session
	 * is open.
	 */
	private Label noSessionMessage;
	private Composite composite;
	private StackLayout stackLayout;
	
	private Session session;

	@Override	
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		init(site);

		if (memento != null) {
			// Restore any session that was open when the workbench
			// was last closed.
			session = JMoneyPlugin.openSession(memento.getChild("session"));
		} else {
			session = null;
		}

		// init is called before createPartControl,
		// and the objects that need the memento are not
		// created until createPartControl is called so we save
		// the memento now for later use.
		// this.memento = memento; 
	}

	@Override	
	public void saveState(IMemento memento) {
		// Save the information required to re-create this navigation view.

		// Save the details of the session.
		DatastoreManager sessionManager = JMoneyPlugin.getDefault().getSessionManager();
		if (sessionManager != null) {
			IMemento sessionMemento = memento.createChild("session");
			IPersistableElement pe = (IPersistableElement)sessionManager.getAdapter(IPersistableElement.class);
			sessionMemento.putString("currentSessionFactoryId", pe.getFactoryId());
			pe.saveState(sessionMemento.createChild("currentSession"));
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
		noSessionMessage.setText(JMoneyPlugin.getResourceString("NavigationView.noSessionMessage"));
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
	
	/**
	 * Given a node in the navigation view, returns an array
	 * containing the page factories that create each tabbed
	 * page in the editor.
	 * <P>
	 * If there are no pages to be displayed for this node
	 * then an empty array is returned and no editor is opened
	 * for the node.
	 * 
	 * @param selectedObject
	 * @return a vector of elements of type IBookkeepingPage
	 */
	private Vector<PageEntry> getPageFactories(Object selectedObject) {
		if (selectedObject instanceof TreeNode) {
			return ((TreeNode)selectedObject).getPageFactories();
		} else if (selectedObject instanceof ExtendableObject) {
			ExtendableObject extendableObject = (ExtendableObject)selectedObject;
			ExtendablePropertySet<?> propertySet = PropertySet.getPropertySet(extendableObject.getClass());
			return propertySet.getPageFactories();
		} else {
			return new Vector<PageEntry>();
		}
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

			JMoneyCommonNavigator.this.session = newSession;

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
