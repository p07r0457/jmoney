/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

package net.sf.jmoney.bookkeepingPages;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;

import net.sf.jmoney.Constants;
import net.sf.jmoney.IBookkeepingPageListener;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.categoriespanel.CategoriesPanelPlugin;
import net.sf.jmoney.model2.AccountAddedEvent;
import net.sf.jmoney.model2.AccountDeletedEvent;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.MutableIncomeExpenseAccount;
import net.sf.jmoney.model2.ObjectLockedForEditException;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.SessionChangeListener;
import net.sf.jmoney.model2.SessionReplacedEvent;
import net.sf.jmoney.views.FolderView;

/**
 * @author Nigel
 *
 * As each folder view will load its own instances of the extension classes,
 * and each folder view will only display the tab items for a single
 * object at any point of time, this class can cache the tab items
 * and re-use them for each selected object.
 */
public class CategoryPage implements IBookkeepingPageListener {

	private TreeViewer viewer;
//	private DrillDownAdapter drillDownAdapter;
	private Action newAccountAction;
	private Action newSubAccountAction;
	private Action deleteAccountAction;

	/**
	 * The account for which the name in the name Text field
	 * applies.  If selectedAccount is null then the  name
	 * Text field should be disabled.
	 */
	private IncomeExpenseAccount selectedAccount = null;
	
	private Label nameLabel;
	private Text nameField;
	
	private Session session;

	public void init(IMemento memento) {
		if (memento != null) {
//			entryOrder = memento.getString("entryOrder");
		} else {
		}
        
/*        
        // Set the grid position of all columns in the grid.
        String customLayoutProperty = properties.getProperty("AccountEntriesPanel.customLayouts", "no");
        if (customLayoutProperty.equals("yes")) {
            // Read the layouts for the simple and extended layouts.
        } else {
            // Set default layouts.
        }
*/        
	}

	public void saveState(IMemento memento) {
//      memento.createChild("entryOrderField", entryOrderFieldName);
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#getPageCount(java.lang.Object)
	 */
	public int getPageCount(Object selectedObject) {
		return 1;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public BookkeepingPage[] createPages(Object selectedObject, Session session, Composite parent) {
		this.session = session;
		
		/**
		 * topLevelControl is a control with grid layout, 
		 * onto which all sub-controls should be placed.
		 */
		Composite topLevelControl = new Composite(parent, SWT.NULL);
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		topLevelControl.setLayout(layout);
		
		viewer = new TreeViewer(topLevelControl, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalSpan = 2;
		viewer.getControl().setLayoutData(gridData);
		
		
		
//		drillDownAdapter = new DrillDownAdapter(viewer);
		ViewContentProvider contentProvider = new ViewContentProvider();
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		
		viewer.setInput(session);
		
		// Listen for changes to the category list.
		JMoneyPlugin.getDefault().addSessionChangeListener(listener);
//		viewer.expandAll();
		
		// Listen for changes in the selection and update the 
		// folder view.
		// TODO: figure out how to get the folder view dynamically.
		// The static getter is not such a clean interface.
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				// if the selection is empty clear the label
				if(event.getSelection().isEmpty()) {
					selectedAccount = null;
					nameField.setText("");
					nameField.setEnabled(false);
				} else if(event.getSelection() instanceof IStructuredSelection) {
					IStructuredSelection selection = (IStructuredSelection)event.getSelection();
					for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
						Object selectedObject = iterator.next();
						if (selectedObject instanceof IncomeExpenseAccount) {
							selectedAccount = (IncomeExpenseAccount)selectedObject;
							// TODO: We should really get the mutable account object
							// now.  This ensures we have an edit lock on the account
							// before the user edits the field.  The field should be
							// disabled if someone else has the account locked for edit.
							nameField.setText(selectedAccount.getName());
							nameField.setEnabled(true);
						} else {
							selectedAccount = null;
							nameField.setText("");
							nameField.setEnabled(false);
						}
						break;
					}
				}
			}
		});
		
		List sampleCheckButton2 = new List(topLevelControl, 0);
		sampleCheckButton2.add("t");
		sampleCheckButton2.add("h");
		sampleCheckButton2.add("i");
		sampleCheckButton2.add("s");
		sampleCheckButton2.add(" ");
		sampleCheckButton2.add("c");
		sampleCheckButton2.add("o");
		sampleCheckButton2.add("n");
		sampleCheckButton2.add("t");
		sampleCheckButton2.add("r");
		sampleCheckButton2.add("o");
		sampleCheckButton2.add("l");
		sampleCheckButton2.add(" ");
		sampleCheckButton2.add("f");
		sampleCheckButton2.add("o");
		sampleCheckButton2.add("r");
		sampleCheckButton2.add("c");
		sampleCheckButton2.add("e");
		sampleCheckButton2.add("s");
		sampleCheckButton2.add(" ");
		sampleCheckButton2.add("t");
		sampleCheckButton2.add("h");
		sampleCheckButton2.add("e");
		sampleCheckButton2.add(" ");
		sampleCheckButton2.add("h");
		sampleCheckButton2.add("e");
		sampleCheckButton2.add("i");
		sampleCheckButton2.add("g");
		sampleCheckButton2.add("h");
		sampleCheckButton2.add("t");
		sampleCheckButton2.add(".");

		nameLabel = new Label(topLevelControl, 0);
		nameLabel.setText(CategoriesPanelPlugin.getResourceString("CategoryPanel.name"));
		
		nameField = new Text(topLevelControl, 0);
		nameField.setText("");
		nameField.setEnabled(false);

		nameField.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == 13) {
					updateCategory();
				}
			}
		});
		nameField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				updateCategory();
			}
		});

		GridData gridData5 = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData5.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 2;
		nameField.setLayoutData(gridData5);

		Label wideLabel = new Label(topLevelControl, 0);
		wideLabel.setText("This control forces up the width of this column in the grid.  Without it, the Swing control would be very thin.");

		GridData gridData3 = new GridData();
		gridData3.horizontalSpan = 2;
		wideLabel.setLayoutData(gridData3);

		// Set up the context menus.
		makeActions();
		hookContextMenu();
		
		return new BookkeepingPage[] 
								   { new BookkeepingPage(topLevelControl, CategoriesPanelPlugin.getResourceString("NavigationTreeModel.categories")) };
	}

	private void updateCategory() {
		if (selectedAccount == null) {
			throw new RuntimeException("selected account null when it should not be null.");
		}
		
		try {
			MutableIncomeExpenseAccount mutableAccount = selectedAccount.createMutableAccount(session);
			mutableAccount.setName(nameField.getText());
			mutableAccount.commit();
		} catch (ObjectLockedForEditException e) {
			// The edit could not be made because someone else is editing
			// the properties of this category.
			// TODO: deal with this properly.  Should be control be
			// disabled, or should the user be told some other way
			// that the properties cannot be changed?
		}
	}
	
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				CategoryPage.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		FolderView.getDefault().getSite().registerContextMenu(menuMgr, viewer);
	}

private void fillContextMenu(IMenuManager manager) {
		manager.add(newAccountAction);
		manager.add(newSubAccountAction);
		manager.add(deleteAccountAction);
		manager.add(new Separator());
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
private void makeActions() {
	newAccountAction = new Action() {
		public void run() {
			Session session = JMoneyPlugin.getDefault().getSession();
	        MutableIncomeExpenseAccount mutableAccount = session.createNewIncomeExpenseAccount();
	        mutableAccount.setName(CategoriesPanelPlugin.getResourceString("CategoryPanel.newCategory"));
	        IncomeExpenseAccount account = mutableAccount.commit();
	        
	        // Having added the new account, set it as the selected
	        // account in the tree viewer.
	        viewer.setSelection(new StructuredSelection(account), true);
		}
	};
	newAccountAction.setText(CategoriesPanelPlugin.getResourceString("CategoryPanel.newCategory"));
	newAccountAction.setToolTipText("New category tooltip");
//	newAccountAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
//			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

	newSubAccountAction = new Action() {
		public void run() {
			Session session = JMoneyPlugin.getDefault().getSession();
			IncomeExpenseAccount account = null;
			IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
			for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
				Object selectedObject = iterator.next();
				account = (IncomeExpenseAccount)selectedObject;
				break;
			}
			if (account != null) {
				MutableIncomeExpenseAccount mutableAccount = account.createNewSubAccount(session);
				mutableAccount.setName(CategoriesPanelPlugin.getResourceString("CategoryPanel.newCategory"));
				IncomeExpenseAccount subAccount = mutableAccount.commit();
				
				// Having added the new account, set it as the selected
				// account in the tree viewer.
				viewer.setSelection(new StructuredSelection(subAccount), true);
			}
		}
	};
	newSubAccountAction.setText(CategoriesPanelPlugin.getResourceString("CategoryPanel.newSubcategory"));
	newSubAccountAction.setToolTipText("New category tooltip");
//	newSubAccountAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
//			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

	deleteAccountAction = new Action() {
		public void run() {
			Session session = JMoneyPlugin.getDefault().getSession();
			IncomeExpenseAccount account = null;
			IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
			for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
				Object selectedObject = iterator.next();
				account = (IncomeExpenseAccount)selectedObject;
				break;
			}
			if (account != null) {
				session.removeAccount(account);
			}
		}
	};
	deleteAccountAction.setText(CategoriesPanelPlugin.getResourceString("CategoryPanel.deleteCategory"));
	deleteAccountAction.setToolTipText("Delete category tooltip");
//	deleteAccountAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
//		getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
}


	class ViewContentProvider implements IStructuredContentProvider, 
	ITreeContentProvider {
		/**
		 * In fact the input does not change because we create our own node object
		 * that acts as the root node.  Certain nodes below the root may get
		 * their data from the model.  The accountsNode object does this.
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
//			The input never changes so we don't do anything here.
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}
		public Object getParent(Object child) {
			if (child instanceof Account) {
				Account parent = ((Account)child).getParent();
				if (parent == null) {
					return session;
				} else {
					return parent;
				}
			}
			return null;
		}
		
		public Object [] getChildren(Object parent) {
			// TODO: The nodes are not currently ordered, but they
			// should be.  

			Iterator iter;
			if (parent instanceof Session) {
				iter = ((Session)parent).getIncomeExpenseAccountIterator();
			} else {
				iter = ((Account)parent).getSubAccountIterator();
			}
			
			int count = 0;
			for ( ; iter.hasNext(); ) {
				iter.next();
				count++;
			}
			Object children[] = new Object[count];
			if (parent instanceof Session) {
				iter = ((Session)parent).getIncomeExpenseAccountIterator();
			} else {
				iter = ((Account)parent).getSubAccountIterator();
			}
			int i = 0;
			for ( ; iter.hasNext(); ) {
				children[i++] = iter.next();
			}
			return children;
		}
		
		public boolean hasChildren(Object parent) {
			if (parent instanceof Session) {
				return ((Session)parent).getIncomeExpenseAccountIterator().hasNext();
			} else if (parent instanceof Account) {
				return ((Account)parent).getSubAccountIterator().hasNext();
			}
			return false;
		}
		
	}
	
	class ViewLabelProvider extends LabelProvider {
		
		public String getText(Object obj) {
			if (obj instanceof Account) {
				return ((Account)obj).getName();
			} else {
				return "unknown object";
			}
		}
		public Image getImage(Object obj) {
			if (obj instanceof Account) {
				return Constants.CATEGORY_ICON;
			} else {
				throw new RuntimeException("");
			}
		}
	}
	
	class NameSorter extends ViewerSorter {
	}
	
	private SessionChangeListener listener =
		new SessionChangeAdapter() {
		public void sessionReplaced( SessionReplacedEvent event ) {
			// When the session is replaced, this view will be disposed
			// and possibly re-built, so do nothing here.
		}
		public void accountAdded(AccountAddedEvent event) {
			if (event.getNewAccount() instanceof IncomeExpenseAccount) {
				Account parent = event.getNewAccount().getParent();
				if (parent == null) {
					viewer.refresh(session, false);
				} else {
					viewer.refresh(parent, false);
				}
			}
		}
		public void accountDeleted(AccountDeletedEvent event) {
			if (event.getOldAccount() instanceof IncomeExpenseAccount) {
				Account parent = event.getOldAccount().getParent();
				if (parent == null) {
					viewer.refresh(session, false);
				} else {
					viewer.refresh(parent, false);
				}
			}
		}
		public void accountChange(PropertyChangeEvent event) {
			if (event.getSource() instanceof IncomeExpenseAccount
					&& event.getProperty().equals("name")) {
				IncomeExpenseAccount account = (IncomeExpenseAccount)event.getSource();
				Account parent = account.getParent();
				// We refresh the parent node because the name change
				// in this node may affect the order of the child nodes.
				if (parent == null) {
					viewer.refresh(session, true);
				} else {
					viewer.refresh(parent, true);
				}
			}
		}
	};
	
}
