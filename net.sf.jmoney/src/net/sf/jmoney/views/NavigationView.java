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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import net.sf.jmoney.Constants;
import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.AccountInfo;
import net.sf.jmoney.fields.CapitalAccountInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ISessionFactory;
import net.sf.jmoney.model2.ISessionManager;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.PropertySetNotFoundException;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.SessionChangeListener;

/**
 * This class implements a workbench view that contains the
 * main navigation tree.
 * <p>
 */

public class NavigationView extends ViewPart {
    public static final String ID_VIEW =
        "net.sf.jmoney.views.NavigationView"; //$NON-NLS-1$

	private TreeViewer viewer;
	private DrillDownAdapter drillDownAdapter;
	private ILabelProvider labelProvider; 
	private Vector newAccountActions = new Vector();  // Element type: Action
	private Action deleteAccountAction;

	private AccountsNode accountsRootNode;
	
	private Session session;
	
	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	 
	
	
	
	
	class TreeNode implements IAdaptable {
		private String name;
		private Image image;
		private TreeNode parent;
		private String parentId;
		private int position;
		protected ArrayList children = null;
		private Vector pageListeners = new Vector(); // element: IBookkeepingPage

		public TreeNode(String name, Image image, TreeNode parent, int position) {
			this.name = name;
			this.image = image;
			this.parent = parent;
			this.position = position;
		}
		public TreeNode(String name, Image image, String parentId, int position) {
			this.name = name;
			this.image = image;
			this.parentId = parentId;
			this.position = position;
		}
		public String getName() {
			return name;
		}
		public TreeNode getParent() {
			return parent;
		}
		int getPosition() {
			return position;
		}
		public String toString() {
			return getName();
		}
		public Object getAdapter(Class key) {
			return null;
		}
		public Image getImage() {
			return image;
		}
		public void addChild(Object child) {
			if (children == null) {
				children = new ArrayList();
			}
			children.add(child);
		}
		
		public void removeChild(Object child) {
			children.remove(child);
		}
		public Object [] getChildren() {
			if (children == null) {
				return new Object[0];
			} else {
				return children.toArray();
			}
		}
		public boolean hasChildren() {
			return children != null && children.size()>0;
		}
		/**
		 * @return
		 */
		public Object getParentId() {
			return parentId;
		}

		/**
		 * @param parentNode
		 */
		public void setParent(TreeNode parent) {
			this.parent = parent;
		}

		/**
		 * @param pageListener
		 */
		public void addPageListener(IBookkeepingPage pageListener) {
			pageListeners.add(pageListener);
		}
		/**
		 * @return An array of objects that implement the IBookkeepingPageListener
		 * 		interface.  The returned value is never null but the Vector may
		 * 		be empty if there are no listeners for this node.
		 */
		public Vector getPageListeners() {
			return pageListeners;
		}
	}

	// TODO: Should the list of accounts be cached by the TreeNode object?
	// Or should we change this code and send the request to the datastore each time the tree view requests
	// a list of accounts or sub-accounts?
	class AccountsNode extends TreeNode {
		public AccountsNode(String name, Image image, TreeNode parent) {
			super(name, image, parent, 100);
			setSession(JMoneyPlugin.getDefault().getSession());
		}
		
		private void setSession(Session session) {
			// Initialize with list of top level accounts from the session.
			if (children == null) {
				children = new ArrayList();
			} else {
				children.clear();
			}
			if (session != null) {
				for (Iterator iter = session.getCapitalAccountIterator(); iter.hasNext(); ) {
					CapitalAccount account = (CapitalAccount)iter.next();
					children.add(account);
				}
			}
		}
	}

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
					return accountsRootNode;
				}
			}
			return null;
		}
		public Object [] getChildren(Object parent) {
			if (parent instanceof TreeNode) {
				return ((TreeNode)parent).getChildren();
			} else if (parent instanceof CapitalAccount) {
				CapitalAccount account = (CapitalAccount)parent;
				int count = 0;
				for (Iterator iter = account.getSubAccountIterator(); iter.hasNext(); iter.next() ) {
					count++;
				}
				Object children[] = new Object[count];
				int i = 0;
				for (Iterator iter = account.getSubAccountIterator(); iter.hasNext(); ) {
					children[i++] = iter.next();
				}
				return children;
			};
			return new Object[0];
		}
		public boolean hasChildren(Object parent) {
			if (parent instanceof TreeNode) {
				return ((TreeNode)parent).hasChildren();
			} else if (parent instanceof CapitalAccount) {
				CapitalAccount account = (CapitalAccount)parent;
				return account.getSubAccountIterator().hasNext();
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
			} else if (obj instanceof CapitalAccount) {
				return Constants.ACCOUNT_ICON;
			} else {
				throw new RuntimeException("");
			}
		}
	}

	class NameSorter extends ViewerSorter {
		public int category(Object obj) {
			if (obj instanceof TreeNode) {
				return ((TreeNode)obj).getPosition();
			} else if (obj instanceof CapitalAccount) {
				return 0;
			} else {
				throw new RuntimeException("");
			}
		}
		
	}

	private SessionChangeListener listener =
		new SessionChangeAdapter() {
		public void sessionReplaced(Session oldSession, Session newSession) {
			NavigationView.this.session = newSession;
			accountsRootNode.setSession(newSession);
			refreshViewer();
		}
		public void accountAdded(Account newAccount) {
			if (newAccount instanceof CapitalAccount) {
				if (newAccount.getParent() == null) {
					// An array of top level accounts is cached, so we add it now.
					accountsRootNode.addChild(newAccount);
				} else {
					// Sub-accounts are not cached in any tree node, so there is
					// nothing to do except to refresh the viewer.
				}
				refreshViewer ();
			}
		}
		public void accountDeleted(Account oldAccount) {
			if (oldAccount instanceof CapitalAccount) {
				if (oldAccount.getParent() == null) {
					// An array of top level accounts is cached, so we add it now.
					accountsRootNode.removeChild(oldAccount);
				} else {
					// Sub-accounts are not cached in any tree node, so there is
					// nothing to do except to refresh the viewer.
				}
				refreshViewer ();
			}
		}
	    public void accountChanged(final Account account, PropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
			if (account instanceof CapitalAccount
					&& propertyAccessor == AccountInfo.getNameAccessor()) {

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
                viewer.refresh(accountsRootNode, false);
            }
        });
	}

private Map idToNodeMap = new HashMap();
	private Map pageListenerAndNodeIdMap = new HashMap();
//	private Vector accountPageListeners = new Vector();
	private Map objectToPagesMap = new HashMap();  // PropertySet to Vector of IBookkeepingPage

	private IMemento memento;
		

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
        	String factoryId = memento.getString("currentSessionFactoryId"); 
        	if (factoryId != null && factoryId.length() != 0) {
        		// Search for the factory.
        		IExtensionRegistry registry = Platform.getExtensionRegistry();
        		IExtensionPoint extensionPoint = registry.getExtensionPoint("org.eclipse.ui.elementFactories");
        		IExtension[] extensions = extensionPoint.getExtensions();
        		for (int i = 0; i < extensions.length; i++) {
        			IConfigurationElement[] elements =
        				extensions[i].getConfigurationElements();
        			for (int j = 0; j < elements.length; j++) {
        				if (elements[j].getName().equals("factory")) {
        					if (elements[j].getAttribute("id").equals(factoryId)) {
        						try {
        							ISessionFactory listener = (ISessionFactory)elements[j].createExecutableExtension("class");
        							
        							// Create and initialize the session object from 
        							// the data stored in the memento.
        							listener.openSession(memento.getChild("currentSession"), getSite().getWorkbenchWindow());
        						} catch (CoreException e) {
        							// Could not create the factory given by the 'class' attribute
        							// Log the error and start JMoney with no open session.
        							e.printStackTrace();
        						}
        						break;
        					}
        				}
        			}
        		}
        	}
        }
    	
        session = JMoneyPlugin.getDefault().getSession();
        
        // init is called before createPartControl,
        // and the objects that need the memento are not
        // created until createPartControl is called so we save
        // the memento now for later use.
        this.memento = memento; 
    }
    
    public void saveState(IMemento memento) {
    	// Save the information required to re-open any open session.
    	ISessionManager sessionManager = JMoneyPlugin.getDefault().getSessionManager();
		if (sessionManager != null) {
			IPersistableElement pe = (IPersistableElement)sessionManager.getAdapter(IPersistableElement.class);
			memento.putString("currentSessionFactoryId", pe.getFactoryId());
			pe.saveState(memento.createChild("currentSession"));
		}
    	
    	// Give each extension a child memento into which it can
    	// save state.
/* TODO: get this working when the GUI design has been completed.   	
		for (Iterator iter = pageListenerAndNodeIdMap.keySet().iterator(); iter.hasNext(); ) {
			IBookkeepingPageListener pageListener = (IBookkeepingPageListener)iter.next();
            IMemento childMemento = memento.createChild(pageListener.getClass().getName());
            pageListener.saveState(childMemento);
		}
*/		
    }
    
	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {

		// Load the extensions
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint("net.sf.jmoney.pages");
		IExtension[] extensions = extensionPoint.getExtensions();
		for (int i = extensions.length-1; i>=0; i--) {
			IConfigurationElement[] elements =
				extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals("node")) {
					
					String label = elements[j].getAttribute("label");
					String icon = elements[j].getAttribute("icon");
					String id = elements[j].getAttribute("id");
					String parentNodeId = elements[j].getAttribute("parent");
					String position = elements[j].getAttribute("position");
					
					if (id != null && id.length() != 0) {
						String fullNodeId = extensions[i].getNamespace() + '.' + id;
						
						Image image = null;
						if (icon != null) {
							// Try getting the image from this plug-in.
							ImageDescriptor descriptor = JMoneyPlugin.imageDescriptorFromPlugin(extensions[i].getNamespace(), icon); 
							if (descriptor == null) {
								// try getting the image from the JMoney plug-in. 
								descriptor = JMoneyPlugin.imageDescriptorFromPlugin("net.sf.jmoney", icon);
							}
							if (descriptor != null) {
								image = descriptor.createImage(); 
							}
						}
						
						int positionNumber = 800;
						if (position != null) {
							positionNumber = Integer.parseInt(position);
						}
						
						TreeNode node = new TreeNode(label, image, parentNodeId, positionNumber);
						idToNodeMap.put(fullNodeId, node);
					}
				}
				if (elements[j].getName().equals("pages")) {
					try {
						Object listener = elements[j].createExecutableExtension("class");
						IBookkeepingPage pageListener = (IBookkeepingPage)listener;
/* not sure about this code
    	 					IMemento pageMemento = null; 
    	 					if (memento != null) {
    	 						pageMemento = memento.getChild(pageListener.getClass().getName());
    	 					}
    	 					pageListener.init(pageMemento);
*/
						String nodeId = elements[j].getAttribute("node");
						if (nodeId != null && nodeId.length() != 0) {
							pageListenerAndNodeIdMap.put(pageListener, nodeId);
						} else {
							// No 'node' attribute so see if we have
							// an 'extendable-property-set' attribute.
							// (This means the page should be supplied if
							// the node represents an object that contains
							// the given property set).
							String propertySetId = elements[j].getAttribute("extendable-property-set");
							if (propertySetId != null) {
								try {
									PropertySet pagePropertySet = PropertySet.getPropertySet(propertySetId);
									
									for (Iterator iter = pagePropertySet.getDerivedPropertySetIterator(); iter.hasNext(); ) {
										PropertySet derivedPropertySet = (PropertySet)iter.next();
										Vector pageList = (Vector)objectToPagesMap.get(derivedPropertySet);
										if (pageList == null) {
											pageList = new Vector();
											objectToPagesMap.put(derivedPropertySet, pageList);
										}
										
										pageList.add(pageListener);
									}
								} catch (PropertySetNotFoundException e1) {
									// This is a plug-in error.
									// TODO implement properly.
									e1.printStackTrace();
								}
							}
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		// Pass two does two things:
		// 1. Set each node's parent.  If no node exists
		// with the given parent node id then the node
		// is placed at the root.
		// 2. Set the list of page constructors in each node.

		// If a node has no child nodes and no page listeners
		// then the node is removed.  This allows nodes to be
		// created by the framework or the more general plug-ins
		// that have no functionality provided by the plug-in that
		// created the node but that can be extended by other
		// plug-ins.  By doing this, rather than expecting plug-ins
		// to create their own nodes, it is more likely that
		// different plug-in developers will share nodes, and
		// thus avoiding hundreds of root nodes in the navigation
		// tree, each with a single tab view. 
		
		TreeNode invisibleRoot = new TreeNode("", null, "", 0);

		for (Iterator iter = idToNodeMap.values().iterator(); iter.hasNext(); ) {
			TreeNode treeNode = (TreeNode)iter.next();
			TreeNode parentNode;
			if (treeNode.getParentId() != null) {
				// TODO: check if map works like this:
				parentNode = (TreeNode)idToNodeMap.get(treeNode.getParentId());
				if (parentNode == null) {
					parentNode = invisibleRoot;
				}
			} else {
				parentNode = invisibleRoot;
			}
			treeNode.setParent(parentNode);
			parentNode.addChild(treeNode);
		}	
		
		for (Iterator iter = pageListenerAndNodeIdMap.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry mapEntry = (Map.Entry)iter.next();
			String nodeId = (String)mapEntry.getValue();
			TreeNode node = (TreeNode)idToNodeMap.get(nodeId);
			if (node != null) {
				IBookkeepingPage pageListener = (IBookkeepingPage)mapEntry.getKey();
				node.addPageListener(pageListener);
			} else {
				// No node found with given id, so the
				// page listener is dropped.
				// TODO Log missing node.
			}
		}


		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		drillDownAdapter = new DrillDownAdapter(viewer);
		labelProvider = new ViewLabelProvider();
		ViewContentProvider contentProvider = new ViewContentProvider();
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(labelProvider);
		viewer.setSorter(new NameSorter());

		AccountsNode accountsObject = new AccountsNode(JMoneyPlugin.getResourceString("NavigationTreeModel.accounts"), Constants.ACCOUNTS_ICON, invisibleRoot);

		invisibleRoot.addChild(accountsObject);
		
		// Certain nodes must be saved in this class so that they can be updated.
		this.accountsRootNode = accountsObject;
	
		viewer.setInput(invisibleRoot);

		// Listen for changes to the account list.
		// (The node containing the list of accounts is currently
		// hard coded into this view).
		JMoneyPlugin.getDefault().addSessionChangeListener(listener);
		
		viewer.expandAll();
		makeActions();
		hookContextMenu();
		contributeToActionBars();
		
		viewer.addDoubleClickListener(new IDoubleClickListener() {
		   public void doubleClick(DoubleClickEvent event) {
			   	// if the selection is empty clear the label
			   	if (event.getSelection().isEmpty()) {
			   		// I don't see how this can happen.
			   	} else if (event.getSelection() instanceof IStructuredSelection) {
			   		IStructuredSelection selection = (IStructuredSelection)event.getSelection();
			   		for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
			   			Object selectedObject = iterator.next();
			   			Vector pageListeners;
			   			if (selectedObject instanceof TreeNode) {
			   				pageListeners = ((TreeNode)selectedObject).getPageListeners();
			   			} else if (selectedObject instanceof ExtendableObject) {
			   				
			   				PropertySet propertySet = PropertySet.getPropertySet(selectedObject.getClass());
			   				pageListeners = (Vector)objectToPagesMap.get(propertySet);
			   			} else {
			   				pageListeners = new Vector();
			   			}
			   			
			   			// Create an editor for this node (or active if an editor
			   			// is already open).
			   			try {
			   				IWorkbenchWindow window = getSite().getWorkbenchWindow();
			   				IEditorInput editorInput = new NodeEditorInput(selectedObject,
									labelProvider.getText(selectedObject),
			   						labelProvider.getImage(selectedObject),
									pageListeners);
			   				window.getActivePage().openEditor(editorInput,
			   				"net.sf.jmoney.genericEditor");
			   			} catch (PartInitException e) {
			   				JMoneyPlugin.log(e);
			   			}
			   			
			   			break;
			   		}
			   	}
		   }
		});
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
		for (Iterator iter = newAccountActions.iterator(); iter.hasNext(); ) {
			Action newAccountAction = (Action)iter.next();
			manager.add(newAccountAction);
		}
		manager.add(new Separator());
		manager.add(deleteAccountAction);
	}

	private void fillContextMenu(IMenuManager manager) {
		for (Iterator iter = newAccountActions.iterator(); iter.hasNext(); ) {
			Action newAccountAction = (Action)iter.next();
			manager.add(newAccountAction);
		}
		manager.add(deleteAccountAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		for (Iterator iter = newAccountActions.iterator(); iter.hasNext(); ) {
			Action newAccountAction = (Action)iter.next();
			manager.add(newAccountAction);
		}
		manager.add(deleteAccountAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		// For each class of object derived (directly or indirectly)
		// from the capital account class, and that is not itself
		// derivable, add a menu item to create a new account of
		// that type.
		for (Iterator iter = CapitalAccountInfo.getPropertySet().getDerivedPropertySetIterator(); iter.hasNext(); ) {
			final PropertySet derivedPropertySet = (PropertySet)iter.next();
			
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
	
	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"Navigation",
			message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}