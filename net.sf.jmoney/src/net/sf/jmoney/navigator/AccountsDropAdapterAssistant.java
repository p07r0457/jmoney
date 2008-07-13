package net.sf.jmoney.navigator;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.AbstractDataOperation;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.ObjectCollection;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.views.AccountsNode;
import net.sf.jmoney.views.CategoriesNode;
import net.sf.jmoney.views.TreeNode;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.part.PluginTransfer;

public class AccountsDropAdapterAssistant extends
		org.eclipse.ui.navigator.CommonDropAdapterAssistant {

	public AccountsDropAdapterAssistant() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public IStatus validateDrop(Object target, int operation,
			TransferData transferType) {
		if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType) || PluginTransfer.getInstance().isSupportedType(transferType)) {
			ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
			if (selection != null && !selection.isEmpty() && (selection instanceof IStructuredSelection)) {

				IStructuredSelection structuredSelection = (IStructuredSelection) selection;

				List draggedObjects = structuredSelection.toList();

				if (draggedObjects == null || canDropObjects(draggedObjects, target)) {
					return Status.OK_STATUS;
				} else {
					return Status.CANCEL_STATUS;
				}

//				event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
			}

		}
		return Status.CANCEL_STATUS;
	}

	@Override
	public IStatus handleDrop(CommonDropAdapter dropAdapter,
			DropTargetEvent dropTargetEvent, Object target) {
		if (LocalSelectionTransfer.getTransfer().isSupportedType(dropAdapter.getCurrentTransfer())) {

			final IStructuredSelection selection = (IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();

			boolean success = false;

			List draggedObjects = selection.toList();

			if (!canDropObjects(draggedObjects, target)) {
				dropTargetEvent.detail = DND.DROP_NONE;
				return Status.OK_STATUS;
			}

			Session session = JMoneyPlugin.getDefault().getSession();
			
			if (target == TreeNode.getTreeNode(AccountsNode.ID)) {
				success = moveCapitalAccounts(session.getAccountCollection(), draggedObjects);
			} else if (target == TreeNode.getTreeNode(CategoriesNode.ID)) {
				success = moveCategoryAccounts(session.getAccountCollection(), draggedObjects);
			} else if (target instanceof CapitalAccount) {
				success = moveCapitalAccounts(((CapitalAccount)target).getSubAccountCollection(), draggedObjects);
			} else if (target instanceof IncomeExpenseAccount) {
				success = moveCategoryAccounts(((IncomeExpenseAccount)target).getSubAccountCollection(), draggedObjects);
			}

			if (!success) {
				dropTargetEvent.detail = DND.DROP_NONE;
			}
		}

		return Status.OK_STATUS;
	}

	private boolean canDropObjects(List draggedObjects, Object targetObject) {
		for (Object object: draggedObjects) {
			if (!canDropObject(object, targetObject)) {
				return false;
			}
		}

		return true;
	}
	
	private boolean canDropObject(Object draggedObject, Object targetObject) {
		boolean success = false;

		if (targetObject == TreeNode.getTreeNode(AccountsNode.ID)) {
			if (draggedObject instanceof CapitalAccount) {
				success = true;
			}
		} else if (targetObject == TreeNode.getTreeNode(CategoriesNode.ID)) {
			if (draggedObject instanceof IncomeExpenseAccount) {
				success = true;
			}
		} else if (targetObject instanceof CapitalAccount) {
			if (draggedObject instanceof CapitalAccount && !isDescendentOf(targetObject, draggedObject)) {
				success = true;
			}
		} else if (targetObject instanceof IncomeExpenseAccount) {
			if (draggedObject instanceof IncomeExpenseAccount && !isDescendentOf(targetObject, draggedObject)) {
				success = true;
			}
		}

		return success;
	}

	/**
	 * 
	 * @param object1
	 * @param object2
	 * @return true if object1 is a descendant of object2, false otherwise
	 */
	private boolean isDescendentOf(Object object1, Object object2) {
		if (object1 instanceof Account) {
			Account ancestorOfObject1 = (Account)object1;
			do {
				if (ancestorOfObject1 == object2) {
					return true;
				}
				ancestorOfObject1 = ancestorOfObject1.getParent();
			} while (ancestorOfObject1 != null);
		}
		
		return false;
	}

	private boolean moveCapitalAccounts(ObjectCollection<? super CapitalAccount> accountCollection, List draggedObjects) {
		ArrayList<CapitalAccount> draggedAccounts = new ArrayList<CapitalAccount>();
		for (Object draggedObject: draggedObjects) {
			if (!(draggedObject instanceof CapitalAccount)) {
				return false;
			}
			draggedAccounts.add((CapitalAccount)draggedObject);
		}
		moveElements(accountCollection, draggedAccounts);
		return true;
	}

	private boolean moveCategoryAccounts(ObjectCollection<? super IncomeExpenseAccount> accountCollection, List draggedObjects) {
		ArrayList<IncomeExpenseAccount> draggedAccounts = new ArrayList<IncomeExpenseAccount>();
		for (Object draggedObject: draggedObjects) {
			if (!(draggedObject instanceof IncomeExpenseAccount)) {
				return false;
			}
			draggedAccounts.add((IncomeExpenseAccount)draggedObject);
		}
		moveElements(accountCollection, draggedAccounts);
		return true;
	}

	private <E extends ExtendableObject> void moveElements(final ObjectCollection<? super E> targetCollection, final Collection<E> objectsToMove) {
		IOperationHistory history = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();

		String description;
		if (objectsToMove.size() == 1) {
			Object[] messageArgs = new Object[] {
					objectsToMove.iterator().next().toString() 
			};
			description = new MessageFormat("Move {0}", java.util.Locale.US).format(messageArgs); 
		} else {
			Object[] messageArgs = new Object[] {
					Integer.toString(objectsToMove.size())
			};
			description = new MessageFormat("Move {0} Accounts", java.util.Locale.US).format(messageArgs); 
		}

		Session session = JMoneyPlugin.getDefault().getSession();
		
		IUndoableOperation operation = new AbstractDataOperation(session, description) {
			@Override
			public IStatus execute() throws ExecutionException {
				for (E objectToMove: objectsToMove) {
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

}
