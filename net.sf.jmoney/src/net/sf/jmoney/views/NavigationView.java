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
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.AbstractDataOperation;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.AccountInfo;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CapitalAccountInfo;
import net.sf.jmoney.model2.CurrentSessionChangeListener;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.IncomeExpenseAccountInfo;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.ObjectCollection;
import net.sf.jmoney.model2.PageEntry;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.wizards.NewAccountWizard;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
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
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TreeItem;
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
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

/**
 * This class implements a workbench view that contains the main navigation
 * tree.
 * <p>
 */

public class NavigationView extends ViewPart {
	public static final String ID_VIEW = "net.sf.jmoney.views.NavigationView"; //$NON-NLS-1$

	/**
	 * Control for the text that is displayed when no session is open.
	 */
	private Label noSessionMessage;

	private TreeViewer viewer;
	private DrillDownAdapter drillDownAdapter;
	private ViewContentProvider contentProvider;
	private ILabelProvider labelProvider;

	private Action openEditorAction;
	private Vector<Action> newAccountActions = new Vector<Action>();
	private Action newCategoryAction;
	private Action deleteAccountAction;

	private Session session;

	class ViewContentProvider implements IStructuredContentProvider,
			ITreeContentProvider {
		/**
		 * In fact the input does not change because we create our own node
		 * object that acts as the root node. Certain nodes below the root may
		 * get their data from the model. The accountsNode object does this.
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
				return ((TreeNode) child).getParent();
			} else if (child instanceof CapitalAccount) {
				CapitalAccount account = (CapitalAccount) child;
				if (account.getParent() != null) {
					return account.getParent();
				} else {
					return TreeNode.getTreeNode(AccountsNode.ID);
				}
			} else if (child instanceof IncomeExpenseAccount) {
				IncomeExpenseAccount account = (IncomeExpenseAccount) child;
				if (account.getParent() != null) {
					return account.getParent();
				} else {
					return TreeNode.getTreeNode(CategoriesNode.ID);
				}
			}
			return null;
		}

		public Object[] getChildren(Object parent) {
			if (parent instanceof TreeNode) {
				return ((TreeNode) parent).getChildren();
			} else if (parent instanceof CapitalAccount) {
				CapitalAccount account = (CapitalAccount) parent;
				return account.getSubAccountCollection().toArray();
			} else if (parent instanceof IncomeExpenseAccount) {
				IncomeExpenseAccount account = (IncomeExpenseAccount) parent;
				return account.getSubAccountCollection().toArray();
			}
			return new Object[0];
		}

		public boolean hasChildren(Object parent) {
			if (parent instanceof TreeNode) {
				return ((TreeNode) parent).hasChildren();
			} else if (parent instanceof CapitalAccount) {
				CapitalAccount account = (CapitalAccount) parent;
				return !account.getSubAccountCollection().isEmpty();
			} else if (parent instanceof IncomeExpenseAccount) {
				IncomeExpenseAccount account = (IncomeExpenseAccount) parent;
				return !account.getSubAccountCollection().isEmpty();
			}
			return false;
		}
	}

	class ViewLabelProvider extends LabelProvider {
		@Override
		public String getText(Object obj) {
			return obj.toString();
		}

		@Override
		public Image getImage(Object obj) {
			if (obj instanceof TreeNode) {
				return ((TreeNode) obj).getImage();
			} else if (obj instanceof ExtendableObject) {
				ExtendableObject extendableObject = (ExtendableObject) obj;
				return PropertySet.getPropertySet(extendableObject.getClass())
						.getIcon();
			} else {
				throw new RuntimeException(Messages.NavigationView_Image);
			}
		}
	}

	class NameSorter extends ViewerSorter {
		@Override
		public int category(Object obj) {
			if (obj instanceof TreeNode) {
				return ((TreeNode) obj).getPosition();
			} else {
				Assert.isTrue(obj instanceof ExtendableObject);
				return 0;
			}
		}
	}

	private class MyCurrentSessionChangeListener extends SessionChangeAdapter
			implements CurrentSessionChangeListener {

		@Override
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
				noSessionMessage
						.setSize(noSessionMessage.getParent().getSize());
				viewer.getControl().setSize(0, 0);
			} else {
				noSessionMessage.setSize(0, 0);
				viewer.getControl().setSize(
						viewer.getControl().getParent().getSize());
			}

			NavigationView.this.session = newSession;

			// Update the viewer (if new session is null then the
			// viewer will not be visible but it is good to release the
			// references to the account objects in the dead session).
			viewer.refresh(TreeNode.getTreeNode(AccountsNode.ID), false);
			viewer.refresh(TreeNode.getTreeNode(CategoriesNode.ID), false);
		}

		@Override
		public void objectInserted(ExtendableObject newObject) {
			if (newObject instanceof Account) {
				Object parentElement = contentProvider.getParent(newObject);
				viewer.insert(parentElement, newObject, 0);
			}
		}

		@Override
		public void objectRemoved(final ExtendableObject deletedObject) {
			if (deletedObject instanceof Account) {
				/*
				 * This listener method is called before the object is deleted.
				 * This allows listener methods to access the deleted object and
				 * its position in the model. However, this is too early to
				 * refresh views. Therefore we must delay the refresh of the
				 * view until after the object is deleted.
				 */
				getSite().getShell().getDisplay().asyncExec(new Runnable() {
					public void run() {
						viewer.remove(deletedObject);
					}
				});
			}
		}

		@Override
		public void objectChanged(ExtendableObject changedObject,
				ScalarPropertyAccessor propertyAccessor, Object oldValue,
				Object newValue) {
			if (changedObject instanceof Account
					&& propertyAccessor == AccountInfo.getNameAccessor()) {
				viewer.update(changedObject, null);
			}
		}

		@Override
		public void objectMoved(ExtendableObject movedObject,
				ExtendableObject originalParent, ExtendableObject newParent,
				ListPropertyAccessor originalParentListProperty,
				ListPropertyAccessor newParentListProperty) {
			if (originalParent == session || newParent == session) {
				if (movedObject instanceof CapitalAccount) {
					viewer
							.refresh(TreeNode.getTreeNode(AccountsNode.ID),
									false);
				} else if (movedObject instanceof IncomeExpenseAccount) {
					viewer.refresh(TreeNode.getTreeNode(CategoriesNode.ID),
							false);
				}
			}
			if (originalParent instanceof Account) {
				viewer.refresh(originalParent, false);
			}
			if (newParent instanceof Account) {
				viewer.refresh(newParent, false);
			}
		}
	}

	/**
	 * The constructor.
	 */
	public NavigationView() {
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		init(site);

		if (memento != null) {
			// Restore any session that was open when the workbench
			// was last closed.
			session = JMoneyPlugin.openSession(memento
					.getChild("session")); //$NON-NLS-1$
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
		DatastoreManager sessionManager = JMoneyPlugin.getDefault()
				.getSessionManager();
		if (sessionManager != null) {
			IMemento sessionMemento = memento.createChild("session"); //$NON-NLS-1$
			IPersistableElement pe = (IPersistableElement) sessionManager
					.getAdapter(IPersistableElement.class);
			sessionMemento.putString("currentSessionFactoryId", pe //$NON-NLS-1$
					.getFactoryId());
			pe.saveState(sessionMemento.createChild("currentSession")); //$NON-NLS-1$
		}
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	@Override
	public void createPartControl(final Composite parent) {
		// The parent will have fill layout set by default.
		// We manage the layout ourselves because we want either
		// the navigation tree to be visible or the 'no session'
		// message to be visible. There is no suitable layout for
		// this. Therefore clear out the layout manager.
		parent.setLayout(null);

		// Create the control that will be visible if no session is open
		noSessionMessage = new Label(parent, SWT.WRAP);
		noSessionMessage.setText(Messages.NavigationView_NoSession);
		noSessionMessage.setForeground(Display.getDefault().getSystemColor(
				SWT.COLOR_DARK_RED));

		// Create the tree viewer
		viewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		drillDownAdapter = new DrillDownAdapter(viewer);
		labelProvider = new ViewLabelProvider();
		contentProvider = new ViewContentProvider();
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(labelProvider);
		viewer.setSorter(new NameSorter());

		TreeNode invisibleRoot = TreeNode.getInvisibleRoot();
		viewer.setInput(invisibleRoot);

		/*
		 * Register the navigation tree as a selection provider to the global
		 * selection. This enables views such as the property sheet viewer to
		 * update themselves to the current selection without the need to depend
		 * on this view.
		 */
		getSite().setSelectionProvider(viewer);

		/*
		 * Listen for changes to the model that may affect the tree view.
		 * Changes that affect this view include changes to account names and
		 * new or deleted accounts.
		 */
		JMoneyPlugin.getDefault().addSessionChangeListener(
				new MyCurrentSessionChangeListener(), viewer.getControl());

		// viewer.expandAll();
		makeActions();
		hookContextMenu();
		contributeToActionBars();

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			/**
			 * When an element is selected and the editor for that element is
			 * already open then we bring that editor to the top. We do not,
			 * however, open an editor if it is not already open. To do that,
			 * the user must double click.
			 */
			public void selectionChanged(SelectionChangedEvent event) {
				if (event.getSelection().isEmpty()) {
					// I don't see how this can happen.
				} else if (event.getSelection() instanceof IStructuredSelection) {
					IStructuredSelection selection = (IStructuredSelection) event
							.getSelection();
					for (Object selectedObject : selection.toList()) {

						// Find and activate the editor for this node (if any)
						Vector<PageEntry> pageFactories = getPageFactories(selectedObject);
						if (!pageFactories.isEmpty()) {
							IWorkbenchWindow window = getSite()
									.getWorkbenchWindow();
							IEditorInput editorInput = new NodeEditorInput(
									selectedObject, labelProvider
											.getText(selectedObject),
									labelProvider.getImage(selectedObject),
									pageFactories, null);
							IWorkbenchPart editor = window.getActivePage()
									.findEditor(editorInput);
							window.getActivePage().activate(editor);
						}

						break;
					}
				}
			}
		});

		/**
		 * When an element is double-clicked, we open the editor (if one exists)
		 * for this element.
		 */
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				for (Object selectedObject : selection.toList()) {

					Vector<PageEntry> pageFactories = getPageFactories(selectedObject);

					// Create an editor for this node (or active if an editor
					// is already open). However, if no pages are registered for
					// this
					// node then do nothing.
					if (!pageFactories.isEmpty()) {
						try {
							IWorkbenchWindow window = getSite()
									.getWorkbenchWindow();
							IEditorInput editorInput = new NodeEditorInput(
									selectedObject, labelProvider
											.getText(selectedObject),
									labelProvider.getImage(selectedObject),
									pageFactories, null);
							window.getActivePage().openEditor(editorInput,
									"net.sf.jmoney.genericEditor"); //$NON-NLS-1$
						} catch (PartInitException e) {
							JMoneyPlugin.log(e);
						}
					}
				}
			}
		});

		/*
		 * Add drag and drop support to the tree. This allows objects such as
		 * accounts to be moved.
		 */

		final DragSource dragSource = new DragSource(viewer.getControl(),
				DND.DROP_MOVE);

		// Provide data using a local reference only (can only drag and drop
		// within the Java VM)
		Transfer[] types = new Transfer[] { LocalSelectionTransfer
				.getTransfer() };
		dragSource.setTransfer(types);

		dragSource.addDragListener(new DragSourceListener() {
			public void dragStart(DragSourceEvent event) {
				IStructuredSelection selection = (IStructuredSelection) viewer
						.getSelection();
				for (Object draggedObject : selection.toArray()) {
					/*
					 * Only the model objects can be dragged. We do not allow
					 * the fixed nodes (TreeNode objects) to be moved.
					 */
					if (!(draggedObject instanceof ExtendableObject)) {
						// we don't want to accept drag
						event.doit = false;
						return;
					}
				}
			}

			public void dragSetData(DragSourceEvent event) {
				// The current selection in the tree is the dragged selection.
				if (LocalSelectionTransfer.getTransfer().isSupportedType(
						event.dataType)) {
					LocalSelectionTransfer.getTransfer().setSelection(
							viewer.getSelection());
				}
			}

			public void dragFinished(DragSourceEvent event) {
				/*
				 * Normally the object would be deleted from its original
				 * position here. However, the underlying datastore does a move
				 * (it does not support copy) so the delete must be done
				 * concurrently with the insert and is done by the drop target.
				 */
			}
		});

		viewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				dragSource.dispose();
			}
		});

		/*
		 * Add the drop support.
		 */
		final DropTarget dropTarget = new DropTarget(viewer.getControl(),
				DND.DROP_MOVE);

		// Data is provided using a local reference only (can only drag and drop
		// within the Java VM)
		Transfer[] types2 = new Transfer[] { LocalSelectionTransfer
				.getTransfer() };
		dropTarget.setTransfer(types2);

		dropTarget.addDropListener(new DropTargetAdapter() {

			private List draggedObjects;
			private TreeItem previousTreeItem = null;
			private boolean canDrop;

			@Override
			public void dragEnter(DropTargetEvent event) {
				/*
				 * We fetch the set of dragged objects, because the set of nodes
				 * onto which an object can be dropped depends on the class of
				 * the objects to be dropped. Unfortunately this is not
				 * available on all platforms, only on Windows. The following
				 * call to the nativeToJava method will return the ISelection
				 * object on Windows but null on other platforms. If we get null
				 * back, we assume the drop is valid.
				 */
				ISelection selection = (ISelection) LocalSelectionTransfer
						.getTransfer().nativeToJava(event.currentDataType);
				if (selection == null) {
					// The selection cannot be determined on this platform -
					// accept the drag
					draggedObjects = null;
					return;
				}

				if (selection instanceof StructuredSelection) {
					draggedObjects = ((StructuredSelection) selection).toList();
				} else {
					event.detail = DND.DROP_NONE;
				}
			}

			@Override
			public void dragLeave(DropTargetEvent event) {
			}

			@Override
			public void dragOperationChanged(DropTargetEvent event) {
			}

			@Override
			public void dragOver(DropTargetEvent event) {
				/*
				 * On non-Windows platforms we do not get back the list of
				 * dragged objects, this information not being made available by
				 * the OS until they are dropped. In this case, the list is null
				 * and we give feedback to the user that the drop will be
				 * accepted (even though it may not result in a successful
				 * drop).
				 */
				if (draggedObjects == null
						|| canDrop(draggedObjects, (TreeItem) event.item)) {
					event.detail = DND.DROP_MOVE;
				} else {
					event.detail = DND.DROP_NONE;
				}

				event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_EXPAND
						| DND.FEEDBACK_SCROLL;
			}

			private boolean canDrop(List draggedObjects, TreeItem item) {
				if (previousTreeItem != item) {
					canDrop = true;
					for (Object object : draggedObjects) {
						if (!canDrop(object, item.getData())) {
							canDrop = false;
							break;
						}
					}

					previousTreeItem = item;
				}

				return canDrop;
			}

			private boolean canDrop(Object draggedObject, Object targetObject) {
				boolean success = false;

				if (targetObject == TreeNode.getTreeNode(AccountsNode.ID)) {
					if (draggedObject instanceof CapitalAccount) {
						success = true;
					}
				} else if (targetObject == TreeNode
						.getTreeNode(CategoriesNode.ID)) {
					if (draggedObject instanceof IncomeExpenseAccount) {
						success = true;
					}
				} else if (targetObject instanceof CapitalAccount) {
					if (draggedObject instanceof CapitalAccount
							&& !isDescendentOf(targetObject, draggedObject)) {
						success = true;
					}
				} else if (targetObject instanceof IncomeExpenseAccount) {
					if (draggedObject instanceof IncomeExpenseAccount
							&& !isDescendentOf(targetObject, draggedObject)) {
						success = true;
					}
				}

				return success;
			}

			/**
			 * 
			 * @param object1
			 * @param object2
			 * @return true if object1 is a descendant of object2, false
			 *         otherwise
			 */
			private boolean isDescendentOf(Object object1, Object object2) {
				if (object1 instanceof Account) {
					Account ancestorOfObject1 = (Account) object1;
					do {
						if (ancestorOfObject1 == object2) {
							return true;
						}
						ancestorOfObject1 = ancestorOfObject1.getParent();
					} while (ancestorOfObject1 != null);
				}

				return false;
			}

			@Override
			public void drop(DropTargetEvent event) {
				if (event.data == null) {
					event.detail = DND.DROP_NONE;
					return;
				}

				// event.data contains a StructuredSelection with a single
				// element being the object being dragged.

				boolean success = false;

				TreeItem item = (TreeItem) event.item;
				Object targetObject = item.getData();

				if (LocalSelectionTransfer.getTransfer().isSupportedType(
						event.currentDataType)) {
					ISelection selection = LocalSelectionTransfer.getTransfer()
							.getSelection();
					if (selection instanceof StructuredSelection) {
						StructuredSelection structured = (StructuredSelection) selection;
						List draggedObjects = structured.toList();

						if (!canDrop(draggedObjects, item)) {
							event.detail = DND.DROP_NONE;
							return;
						}

						if (targetObject == TreeNode
								.getTreeNode(AccountsNode.ID)) {
							success = moveCapitalAccounts(session
									.getAccountCollection(), draggedObjects);
						} else if (targetObject == TreeNode
								.getTreeNode(CategoriesNode.ID)) {
							success = moveCategoryAccounts(session
									.getAccountCollection(), draggedObjects);
						} else if (targetObject instanceof CapitalAccount) {
							success = moveCapitalAccounts(
									((CapitalAccount) targetObject)
											.getSubAccountCollection(),
									draggedObjects);
						} else if (targetObject instanceof IncomeExpenseAccount) {
							success = moveCategoryAccounts(
									((IncomeExpenseAccount) targetObject)
											.getSubAccountCollection(),
									draggedObjects);
						}
					}
				}

				if (!success) {
					event.detail = DND.DROP_NONE;
				}
			}

			private boolean moveCapitalAccounts(
					ObjectCollection<? super CapitalAccount> accountCollection,
					List draggedObjects) {
				ArrayList<CapitalAccount> draggedAccounts = new ArrayList<CapitalAccount>();
				for (Object draggedObject : draggedObjects) {
					if (!(draggedObject instanceof CapitalAccount)) {
						return false;
					}
					draggedAccounts.add((CapitalAccount) draggedObject);
				}
				moveElements(accountCollection, draggedAccounts);
				return true;
			}

			private boolean moveCategoryAccounts(
					ObjectCollection<? super IncomeExpenseAccount> accountCollection,
					List draggedObjects) {
				ArrayList<IncomeExpenseAccount> draggedAccounts = new ArrayList<IncomeExpenseAccount>();
				for (Object draggedObject : draggedObjects) {
					if (!(draggedObject instanceof IncomeExpenseAccount)) {
						return false;
					}
					draggedAccounts.add((IncomeExpenseAccount) draggedObject);
				}
				moveElements(accountCollection, draggedAccounts);
				return true;
			}

			private <E extends ExtendableObject> void moveElements(
					final ObjectCollection<? super E> targetCollection,
					final Collection<E> objectsToMove) {
				IOperationHistory history = NavigationView.this.getSite()
						.getWorkbenchWindow().getWorkbench()
						.getOperationSupport().getOperationHistory();

				String description;
				if (objectsToMove.size() == 1) {
					Object[] messageArgs = new Object[] { objectsToMove
							.iterator().next().toString() };
					description = NLS.bind(Messages.NavigationView_Move,
							messageArgs);
				} else {
					Object[] messageArgs = new Object[] { Integer
							.toString(objectsToMove.size()) };
					description = NLS.bind(Messages.NavigationView_MultiMove,
							messageArgs);
				}

				IUndoableOperation operation = new AbstractDataOperation(
						session, description) {
					@Override
					public IStatus execute() throws ExecutionException {
						for (E objectToMove : objectsToMove) {
							targetCollection.moveElement(objectToMove);
						}
						return Status.OK_STATUS;
					}
				};
				operation.addContext(session.getUndoContext());
				try {
					history.execute(operation, null, null);
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void dropAccept(DropTargetEvent event) {
				// TODO Should the check code in 'drop' be moved here?
			}
		});

		viewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				dropTarget.dispose();
			}
		});

		/*
		 * There is no layout set on the navigation view. Therefore we must
		 * listen for changes to the size of the navigation view and adjust the
		 * size of the visible control to match.
		 */
		parent.addControlListener(new ControlAdapter() {
			@Override
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
	 * Given a node in the navigation view, returns an array containing the page
	 * factories that create each tabbed page in the editor.
	 * <P>
	 * If there are no pages to be displayed for this node then an empty array
	 * is returned and no editor is opened for the node.
	 * 
	 * @param selectedObject
	 * @return a vector of elements of type IBookkeepingPage
	 */
	protected Vector<PageEntry> getPageFactories(Object selectedObject) {
		if (selectedObject instanceof TreeNode) {
			return ((TreeNode) selectedObject).getPageFactories();
		} else if (selectedObject instanceof ExtendableObject) {
			ExtendableObject extendableObject = (ExtendableObject) selectedObject;
			ExtendablePropertySet<?> propertySet = PropertySet
					.getPropertySet(extendableObject.getClass());
			return propertySet.getPageFactories();
		} else {
			return new Vector<PageEntry>();
		}
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
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

		ActionGroup undoRedoActionGroup = new UndoRedoActionGroup(getSite(),
				getSite().getWorkbenchWindow().getWorkbench()
						.getOperationSupport().getUndoContext(), true);
		undoRedoActionGroup.fillActionBars(bars);
	}

	private void fillLocalPullDown(IMenuManager manager) {
		for (Action newAccountAction : newAccountActions) {
			manager.add(newAccountAction);
		}
		manager.add(newCategoryAction);
		manager.add(new Separator());
		manager.add(deleteAccountAction);

		ActionGroup undoRedoActionGroup = new UndoRedoActionGroup(getSite(),
				getSite().getWorkbenchWindow().getWorkbench()
						.getOperationSupport().getUndoContext(), true);
		undoRedoActionGroup.fillContextMenu(manager);

	}

	private void fillContextMenu(IMenuManager manager) {
		// Get the current node
		Object selectedObject = null;
		IStructuredSelection selection = (IStructuredSelection) viewer
				.getSelection();
		for (Object selectedObject2 : selection.toList()) {
			selectedObject = selectedObject2;
			break;
		}

		if (!getPageFactories(selectedObject).isEmpty()) {
			manager.add(openEditorAction);
		}

		manager.add(new Separator());

		if (selectedObject == TreeNode.getTreeNode(AccountsNode.ID)
				|| selectedObject instanceof CapitalAccount) {
			for (Action newAccountAction : newAccountActions) {
				manager.add(newAccountAction);
			}
		}

		if (selectedObject == TreeNode.getTreeNode(CategoriesNode.ID)
				|| selectedObject instanceof IncomeExpenseAccount) {
			manager.add(newCategoryAction);
		}

		if (selectedObject instanceof Account) {
			manager.add(deleteAccountAction);
		}

		manager.add(new Separator());

		ActionGroup ag = new UndoRedoActionGroup(getSite(), getSite()
				.getWorkbenchWindow().getWorkbench().getOperationSupport()
				.getUndoContext(), true);
		ag.fillContextMenu(manager);

		drillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		drillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		openEditorAction = new Action() {
			@Override
			public void run() {
				Object selectedObject = null;
				IStructuredSelection selection = (IStructuredSelection) viewer
						.getSelection();
				for (Object selectedObject2 : selection.toList()) {
					selectedObject = selectedObject2;
					break;
				}

				Vector<PageEntry> pageFactories = getPageFactories(selectedObject);
				Assert.isTrue(!pageFactories.isEmpty());

				// Create an editor for this node (or active if an editor
				// is already open).
				try {
					IWorkbenchWindow window = getSite().getWorkbenchWindow();
					IEditorInput editorInput = new NodeEditorInput(
							selectedObject, labelProvider
									.getText(selectedObject), labelProvider
									.getImage(selectedObject), pageFactories,
							null);
					window.getActivePage().openEditor(editorInput,
							"net.sf.jmoney.genericEditor"); //$NON-NLS-1$
				} catch (PartInitException e) {
					JMoneyPlugin.log(e);
				}
			}
		};
		openEditorAction.setText(Messages.NavigationView_Open);
		openEditorAction.setToolTipText(Messages.NavigationView_Open);

		/*
		 * For each class of object derived (directly or indirectly) from the
		 * capital account class, and that is not itself derivable, add a menu
		 * item to create a new account of that type.
		 */
		for (final ExtendablePropertySet<? extends CapitalAccount> derivedPropertySet : CapitalAccountInfo
				.getPropertySet().getDerivedPropertySets()) {

			Action newAccountAction = new Action() {
				@Override
				public void run() {
					CapitalAccount account = null;
					IStructuredSelection selection = (IStructuredSelection) viewer
							.getSelection();
					for (Object selectedObject : selection.toList()) {
						if (selectedObject instanceof CapitalAccount) {
							account = (CapitalAccount) selectedObject;
							break;
						}
					}

					NewAccountWizard wizard = new NewAccountWizard(session,
							account, derivedPropertySet);
					WizardDialog dialog = new WizardDialog(
							getSite().getShell(), wizard);
					dialog.setPageSize(600, 300);
					int result = dialog.open();
					if (result == WizardDialog.OK) {
						// Having added the new account, set it as the selected
						// account in the tree viewer.
						viewer.setSelection(new StructuredSelection(wizard
								.getNewAccount()), true);
					}
				}
			};

			Object[] messageArgs = new Object[] { derivedPropertySet
					.getObjectDescription() };

			newAccountAction.setText(NLS.bind(
					Messages.NavigationView_NewAccount, messageArgs));

			newAccountAction.setToolTipText(NLS.bind(
					Messages.NavigationView_CreateNewAccount, messageArgs));

			newAccountAction.setImageDescriptor(PlatformUI.getWorkbench()
					.getSharedImages().getImageDescriptor(
							ISharedImages.IMG_OBJS_INFO_TSK));

			newAccountActions.add(newAccountAction);
		}

		newCategoryAction = new Action() {
			@Override
			public void run() {
				IncomeExpenseAccount account = null;
				IStructuredSelection selection = (IStructuredSelection) viewer
						.getSelection();
				for (Object selectedObject : selection.toList()) {
					if (selectedObject instanceof IncomeExpenseAccount) {
						account = (IncomeExpenseAccount) selectedObject;
						break;
					}
				}

				NewAccountWizard wizard = new NewAccountWizard(session, account);
				WizardDialog dialog = new WizardDialog(getSite().getShell(),
						wizard);
				dialog.setPageSize(600, 300);
				int result = dialog.open();
				if (result == WizardDialog.OK) {
					// Having added the new account, set it as the selected
					// account in the tree viewer.
					viewer.setSelection(new StructuredSelection(wizard
							.getNewAccount()), true);
				}
			}
		};

		Object[] messageArgs = new Object[] { IncomeExpenseAccountInfo
				.getPropertySet().getObjectDescription() };

		newCategoryAction.setText(NLS.bind(Messages.NavigationView_NewAccount,
				messageArgs));

		newCategoryAction.setToolTipText(NLS.bind(
				Messages.NavigationView_CreateNewAccount, messageArgs));

		newCategoryAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(
						ISharedImages.IMG_OBJS_INFO_TSK));

		deleteAccountAction = new Action() {
			@Override
			public void run() {
				final Session session = JMoneyPlugin.getDefault().getSession();
				Account account = null;
				IStructuredSelection selection = (IStructuredSelection) viewer
						.getSelection();
				for (Object selectedObject : selection.toList()) {
					account = (Account) selectedObject;
					break;
				}
				if (account != null) {
					final Account account2 = account;

					IOperationHistory history = NavigationView.this.getSite()
							.getWorkbenchWindow().getWorkbench()
							.getOperationSupport().getOperationHistory();

					IUndoableOperation operation = new AbstractDataOperation(
							session, "delete account") { //$NON-NLS-1$
						@Override
						public IStatus execute() throws ExecutionException {
							if (account2.getParent() != null) {
								if (account2.getParent() instanceof CapitalAccount) {
									((CapitalAccount) account2.getParent())
											.getSubAccountCollection().remove(
													account2);
								} else {
									((IncomeExpenseAccount) account2
											.getParent())
											.getSubAccountCollection().remove(
													account2);
								}
							} else {
								session.deleteAccount(account2);
							}
							return Status.OK_STATUS;
						}
					};

					operation.addContext(session.getUndoContext());
					try {
						history.execute(operation, null, null);
					} catch (ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		};
		deleteAccountAction.setText(Messages.NavigationView_DeleteAccount);
		deleteAccountAction
				.setToolTipText(Messages.NavigationView_DeleteAccount);
		deleteAccountAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(
						ISharedImages.IMG_TOOL_DELETE));
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}