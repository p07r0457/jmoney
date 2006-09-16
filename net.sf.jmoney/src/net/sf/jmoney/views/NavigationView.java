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

package net.sf.jmoney.views;

import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.AccountInfo;
import net.sf.jmoney.fields.CapitalAccountInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.PageEntry;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.SessionChangeListener;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

/**
 * This class implements a workbench view that contains the
 * main navigation tree.
 * <p>
 */

public class NavigationView extends ViewPart {
    public static final String ID_VIEW =
        "net.sf.jmoney.views.NavigationView"; //$NON-NLS-1$

    /**
     * Control for the text that is displayed when no session
     * is open.
     */
    private Label noSessionMessage;
    
	private TreeViewer viewer;
	private DrillDownAdapter drillDownAdapter;
	private ILabelProvider labelProvider; 

	private Action openEditorAction;
	private Vector<Action> newAccountActions = new Vector<Action>();
	private Action deleteAccountAction;

	private Session session;
	
	class ViewContentProvider implements IStructuredContentProvider, 
										   ITreeContentProvider {
		/**
		 * In fact the input does not change because we create our own node object
		 * that acts as the root node.  Certain nodes below the root may get
		 * their data from the model.  The accountsNode object does this.
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// The input never changes so we don't do anything here.
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}
		public Object getParent(Object child) {
			if (child instanceof TreeNode) {
				return ((TreeNode)child).getParent();
			} else if (child instanceof CapitalAccount) {
				CapitalAccount account = (CapitalAccount)child;
				if (account.getParent() != null) {
					return account.getParent();
				} else {
					return TreeNode.getAccountsRootNode();
				}
			}
			return null;
		}
		public Object [] getChildren(Object parent) {
			if (parent instanceof TreeNode) {
				return ((TreeNode)parent).getChildren();
			} else if (parent instanceof CapitalAccount) {
				CapitalAccount account = (CapitalAccount)parent;
				return account.getSubAccountCollection().toArray();
			};
			return new Object[0];
		}
		
		public boolean hasChildren(Object parent) {
			if (parent instanceof TreeNode) {
				return ((TreeNode)parent).hasChildren();
			} else if (parent instanceof CapitalAccount) {
				CapitalAccount account = (CapitalAccount)parent;
				return !account.getSubAccountCollection().isEmpty();
			}
			return false;
		}
	}

	class ViewLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			return obj.toString();
		}
		public Image getImage(Object obj) {
			if (obj instanceof TreeNode) {
				return ((TreeNode)obj).getImage();
			} else if (obj instanceof ExtendableObject) {
				ExtendableObject extendableObject = (ExtendableObject)obj;
				return PropertySet.getPropertySet(extendableObject.getClass()).getIcon();
			} else {
				throw new RuntimeException("");
			}
		}
	}

	class NameSorter extends ViewerSorter {
		public int category(Object obj) {
			if (obj instanceof TreeNode) {
				return ((TreeNode)obj).getPosition();
			} else {
				JMoneyPlugin.myAssert (obj instanceof ExtendableObject);
				return 0;
			}
		}
	}

	private SessionChangeListener listener =
		new SessionChangeAdapter() {
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
		
			// Make either the label or the tree control visible, depending
			// on whether the new session is null or not.
			if (JMoneyPlugin.getDefault().getSession() == null) {
				noSessionMessage.setSize(noSessionMessage.getParent().getSize());
				viewer.getControl().setSize(0, 0);
			} else {
				noSessionMessage.setSize(0, 0);
				viewer.getControl().setSize(viewer.getControl().getParent().getSize());
			}

			// Update the viewer (if new session is null then the
			// viewer will not be visible but it is good to release the
			// references to the account objects in the dead session).
			NavigationView.this.session = newSession;
			TreeNode.getAccountsRootNode().setSession(newSession);
			refreshViewer();
		}
		
		public void objectInserted(ExtendableObject newObject) {
			if (newObject instanceof CapitalAccount) {
				CapitalAccount newAccount = (CapitalAccount)newObject;
				if (newAccount.getParent() == null) {
					// An array of top level accounts is cached, so we add it now.
					TreeNode.getAccountsRootNode().addChild(newAccount);
				} else {
					// Sub-accounts are not cached in any tree node, so there is
					// nothing to do except to refresh the viewer.
				}
				refreshViewer();
			}
		}

		public void objectRemoved(ExtendableObject deletedObject) {
			if (deletedObject instanceof CapitalAccount) {
				CapitalAccount deletedAccount = (CapitalAccount)deletedObject;
				if (deletedAccount.getParent() == null) {
					// An array of top level accounts is cached, so we add it now.
					TreeNode.getAccountsRootNode().removeChild(deletedAccount);
				} else {
					// Sub-accounts are not cached in any tree node, so there is
					// nothing to do except to refresh the viewer.
				}
				refreshViewer ();
			}
		}
		
		public void objectChanged(ExtendableObject changedObject, ScalarPropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
			if (changedObject instanceof CapitalAccount
					&& propertyAccessor == AccountInfo.getNameAccessor()) {
				final CapitalAccount account = (CapitalAccount)changedObject;
				
				// SWTException: Invalid thread access can occur
				// without wrapping the call to update.
		        Display.getDefault().syncExec( new Runnable() {
		            public void run() {
						viewer.update(account, null);
		            }
		        });
			}
		}
	};
	
	/**
	 * Refresh the viewer Object thread-safe. 
	 *
	 */
	private void refreshViewer () {
        Display.getDefault().syncExec( new Runnable() {
            public void run() {
                viewer.refresh(TreeNode.getAccountsRootNode(), false);
            }
        });
	}

	/**
	 * The constructor.
	 */
	public NavigationView() {
	}

    public void init(IViewSite site, IMemento memento) throws PartInitException {
        init(site);

        if (memento != null) {
        	// Restore any session that was open when the workbench
        	// was last closed.
    		session = JMoneyPlugin.openSession(memento.getChild("session"));
        } else {
        	session = null;
        }

        // The accounts root node caches the top level accounts, so set
        // the session so this can be done.
        // TODO: change this when the accounts are no longer cached in the node.
        TreeNode.getAccountsRootNode().setSession(session);
        
        // init is called before createPartControl,
        // and the objects that need the memento are not
        // created until createPartControl is called so we save
        // the memento now for later use.
        // this.memento = memento; 
    }
    
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
	public void createPartControl(final Composite parent) {
		// The parent will have fill layout set by default.
		// We manage the layout ourselves because we want either
		// the navigation tree to be visible or the 'no session'
		// message to be visible.  There is no suitable layout for
		// this.  Therefore clear out the layout manager.
		parent.setLayout(null);

		// Create the control that will be visible if no session is open
		noSessionMessage = new Label(parent, SWT.WRAP);
		noSessionMessage.setText(JMoneyPlugin.getResourceString("NavigationView.noSessionMessage"));
		noSessionMessage.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));
		
		// Create the tree viewer
		viewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		drillDownAdapter = new DrillDownAdapter(viewer);
		labelProvider = new ViewLabelProvider();
		ViewContentProvider contentProvider = new ViewContentProvider();
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(labelProvider);
		viewer.setSorter(new NameSorter());

		TreeNode invisibleRoot = TreeNode.getInvisibleRoot();
		viewer.setInput(invisibleRoot);

		// Listen for changes to the account list.
		// (The node containing the list of accounts is currently
		// hard coded into this view).
		JMoneyPlugin.getDefault().addSessionChangeListener(listener);
		
		viewer.expandAll();
		makeActions();
		hookContextMenu();
		contributeToActionBars();
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
			   	// if the selection is empty clear the label
			   	if (event.getSelection().isEmpty()) {
			   		// I don't see how this can happen.
			   	} else if (event.getSelection() instanceof IStructuredSelection) {
			   		IStructuredSelection selection = (IStructuredSelection)event.getSelection();
			   		for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
			   			Object selectedObject = iterator.next();
			   			
			   			// Find and activate the editor for this node (if any)
			   			Vector<PageEntry> pageFactories = getPageFactories(selectedObject);
			   			if (!pageFactories.isEmpty()) {
			   					IWorkbenchWindow window = getSite().getWorkbenchWindow();
			   					IEditorInput editorInput = new NodeEditorInput(selectedObject,
			   							labelProvider.getText(selectedObject),
										labelProvider.getImage(selectedObject),
										pageFactories,
										null);
			   					IWorkbenchPart editor = window.getActivePage().findEditor(editorInput);
			   					window.getActivePage().activate(editor);
			   			}
			   			
			   			break;
			   		}
			   	}
		   }
		});

		viewer.addDoubleClickListener(new IDoubleClickListener() {
			   public void doubleClick(DoubleClickEvent event) {
				   	// if the selection is empty clear the label
				   	if (event.getSelection().isEmpty()) {
				   		// I don't see how this can happen.
				   	} else if (event.getSelection() instanceof IStructuredSelection) {
				   		IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				   		for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
				   			Object selectedObject = iterator.next();
				   			
				   			Vector<PageEntry> pageFactories = getPageFactories(selectedObject);
				   			
				   			// Create an editor for this node (or active if an editor
				   			// is already open).  However, if no pages are registered for this
				   			// node then do nothing.
				   			if (!pageFactories.isEmpty()) {
				   				try {
				   					IWorkbenchWindow window = getSite().getWorkbenchWindow();
				   					IEditorInput editorInput = new NodeEditorInput(selectedObject,
				   							labelProvider.getText(selectedObject),
											labelProvider.getImage(selectedObject),
											pageFactories,
											null);
				   					window.getActivePage().openEditor(editorInput,
				   					"net.sf.jmoney.genericEditor");
				   				} catch (PartInitException e) {
				   					JMoneyPlugin.log(e);
				   				}
				   			}
				   			
				   			break;
				   		}
				   	}
			   }
			});

		// There is no layout set on the navigation view.
		// Therefore we must listen for changes to the size of
		// the navigation view and adjust the size of the visible
		// control to match.
		parent.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				if (JMoneyPlugin.getDefault().getSession() == null) {
					noSessionMessage.setSize(parent.getSize());
					viewer.getControl().setSize(0, 0);
				} else {
					noSessionMessage.setSize(0, 0);
					viewer.getControl().setSize(parent.getSize());
				}
			}
			
		});
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
	protected Vector<PageEntry> getPageFactories(Object selectedObject) {
		if (selectedObject instanceof TreeNode) {
			return ((TreeNode)selectedObject).getPageFactories();
		} else if (selectedObject instanceof ExtendableObject) {
			ExtendableObject extendableObject = (ExtendableObject)selectedObject;
			PropertySet<?> propertySet = PropertySet.getPropertySet(extendableObject.getClass());
			return propertySet.getPageFactories();
		} else {
			return new Vector<PageEntry>();
		}
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				NavigationView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		for (Action newAccountAction: newAccountActions) {
			manager.add(newAccountAction);
		}
		manager.add(new Separator());
		manager.add(deleteAccountAction);
	}

	private void fillContextMenu(IMenuManager manager) {
		// Get the current node
		Object selectedObject = null;
		IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
		for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
			selectedObject = iterator.next();
			break;
		}
		
		if (!getPageFactories(selectedObject).isEmpty()) {
			manager.add(openEditorAction);
		}
		
		manager.add(new Separator());

		if (selectedObject instanceof AccountsNode 
				|| selectedObject instanceof Account) {
			for (Action newAccountAction: newAccountActions) {
				manager.add(newAccountAction);
			}
			if (selectedObject instanceof Account) {
				manager.add(deleteAccountAction);
			}
		}

		manager.add(new Separator());

		drillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		drillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		openEditorAction = new Action() {
			public void run() {
				Object selectedObject = null;
				IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
				for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
					selectedObject = iterator.next();
					break;
				}
				
	   			Vector<PageEntry> pageFactories = getPageFactories(selectedObject);
	   			JMoneyPlugin.myAssert (!pageFactories.isEmpty());
	   			
	   			// Create an editor for this node (or active if an editor
	   			// is already open).
	   			try {
	   				IWorkbenchWindow window = getSite().getWorkbenchWindow();
	   				IEditorInput editorInput = new NodeEditorInput(selectedObject,
	   						labelProvider.getText(selectedObject),
							labelProvider.getImage(selectedObject),
							pageFactories,
							null);
	   				window.getActivePage().openEditor(editorInput,
	   				"net.sf.jmoney.genericEditor");
	   			} catch (PartInitException e) {
	   				JMoneyPlugin.log(e);
	   			}
			}
		};
		openEditorAction.setText(JMoneyPlugin.getResourceString("MainFrame.openEditor"));
		openEditorAction.setToolTipText(JMoneyPlugin.getResourceString("MainFrame.openEditor"));
		
		// For each class of object derived (directly or indirectly)
		// from the capital account class, and that is not itself
		// derivable, add a menu item to create a new account of
		// that type.
		for (Iterator<PropertySet<? extends CapitalAccount>> iter = CapitalAccountInfo.getPropertySet().getDerivedPropertySetIterator(); iter.hasNext(); ) {
			final PropertySet<? extends CapitalAccount> derivedPropertySet = iter.next();
			
			Action newAccountAction = new Action() {
				public void run() {
					CapitalAccount account = null;
					IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
					for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
						Object selectedObject = iterator.next();
						if (selectedObject instanceof CapitalAccount) {
							account = (CapitalAccount)selectedObject;
							break;
						}
					}
					
					CapitalAccount newAccount;
					if (account == null) {
						newAccount = (CapitalAccount)session.createAccount(derivedPropertySet);
					} else {
						newAccount = (CapitalAccount)account.createSubAccount(derivedPropertySet);
					}
					//		        newAccount.setName(JMoneyPlugin.getResourceString("Account.newAccount"));
					session.registerUndoableChange("add new account");
					
					// Having added the new account, set it as the selected
					// account in the tree viewer.
					viewer.setSelection(new StructuredSelection(newAccount), true);
				}
			};
			
			Object [] messageArgs = new Object[] {
					derivedPropertySet.getObjectDescription()
			};
			
			newAccountAction.setText(
					new java.text.MessageFormat(
							JMoneyPlugin.getResourceString("MainFrame.newAccount"), 
							java.util.Locale.US)
							.format(messageArgs));
			
			newAccountAction.setToolTipText(
					new java.text.MessageFormat(
							"Create a New {0}", 
							java.util.Locale.US)
							.format(messageArgs));
			
			newAccountAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
					getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
			
			newAccountActions.add(newAccountAction);
		}
		
		deleteAccountAction = new Action() {
			public void run() {
				Session session = JMoneyPlugin.getDefault().getSession();
				CapitalAccount account = null;
				IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
				for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
					Object selectedObject = iterator.next();
					account = (CapitalAccount)selectedObject;
					break;
				}
				if (account != null) {
					session.deleteAccount(account);
			        session.registerUndoableChange("delete account");
				}
			}
		};
		deleteAccountAction.setText(JMoneyPlugin.getResourceString("MainFrame.deleteAccount"));
		deleteAccountAction.setToolTipText("Delete an account");
		deleteAccountAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}