package net.sf.jmoney.navigator;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.AccountInfo;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CurrentSessionChangeListener;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.views.AccountsNode;
import net.sf.jmoney.views.CategoriesNode;
import net.sf.jmoney.views.TreeNode;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

public class AccountsContentProvider implements ITreeContentProvider {

	private MyCurrentSessionChangeListener listener;

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TreeNode) {
			return ((TreeNode)parentElement).getChildren();
		} else if (parentElement instanceof Account) {
			return ((Account)parentElement).getSubAccountCollection().toArray();
		}
		return new Object[0];
	}

	public Object getParent(Object element) {
		if (element instanceof TreeNode) {
			return ((TreeNode)element).getParent();
		} else if (element instanceof Account) {
			Account parentAccount = ((Account)element).getParent();
			if (parentAccount == null) {
				return TreeNode.getTreeNode(
						(element instanceof CapitalAccount) ? AccountsNode.ID : CategoriesNode.ID);
			} else {
				return parentAccount;
			}
		}
		return null;  // Should never happen
	}

	public boolean hasChildren(Object element) {
		if (element instanceof TreeNode) {
			return ((TreeNode)element).hasChildren();
		}
		if (element instanceof CapitalAccount) {
			return !((CapitalAccount)element).getSubAccountCollection().isEmpty();
		}
		if (element instanceof IncomeExpenseAccount) {
			return !((IncomeExpenseAccount)element).getSubAccountCollection().isEmpty();
		}
		return false;
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
//		Session oldSession = (Session)oldInput;
//		Session newSession = (Session)newInput;
//		
//		if (oldSession != null) {
//			oldSession.getDataManager().removeChangeListener(listener);
//		}
//
//		if (newSession != null) {
//			listener = new MyCurrentSessionChangeListener((TreeViewer)viewer);
//			newSession.getDataManager().addChangeListener(listener);
//		} else {
//			listener = null;
//		}

		JMoneyPlugin.getDefault().addSessionChangeListener(new MyCurrentSessionChangeListener((TreeViewer)viewer), viewer.getControl());
	}

	private class MyCurrentSessionChangeListener extends SessionChangeAdapter implements CurrentSessionChangeListener {
		private TreeViewer viewer;
		
		MyCurrentSessionChangeListener(TreeViewer viewer) {
			this.viewer = viewer;
		}
		
		@Override	
		public void objectInserted(ExtendableObject newObject) {
			if (newObject instanceof Account) {
				Object parentElement = getParent(newObject);
				viewer.insert(parentElement, newObject, 0);
			}
		}

		@Override	
		public void objectRemoved(final ExtendableObject deletedObject) {
			if (deletedObject instanceof Account) {
				/*
				 * This listener method is called before the object is deleted.
				 * This allows listener methods to access the deleted object and
				 * its position in the model.  However, this is too early to
				 * refresh views.  Therefore we must delay the refresh of the view
				 * until after the object is deleted.
				 */
				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {
						viewer.remove(deletedObject);
					}
				});
			}
		}

		@Override	
		public void objectChanged(ExtendableObject changedObject, ScalarPropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
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
			if (originalParent instanceof Session || newParent instanceof Session) {
				if (movedObject instanceof CapitalAccount) {
					viewer.refresh(TreeNode.getTreeNode(AccountsNode.ID), false);
				} else if (movedObject instanceof IncomeExpenseAccount) {
					viewer.refresh(TreeNode.getTreeNode(CategoriesNode.ID), false);
				}
			}
			if (originalParent instanceof Account) {
				viewer.refresh(originalParent, false);
			}
			if (newParent instanceof Account) {
				viewer.refresh(newParent, false);
			}
		}

		public void sessionReplaced(Session oldSession, Session newSession) {
			// Update the viewer (if new session is null then the
			// viewer will not be visible but it is good to release the
			// references to the account objects in the dead session).
			viewer.refresh(TreeNode.getTreeNode(AccountsNode.ID), false);
			viewer.refresh(TreeNode.getTreeNode(CategoriesNode.ID), false);
		}
	}

}
