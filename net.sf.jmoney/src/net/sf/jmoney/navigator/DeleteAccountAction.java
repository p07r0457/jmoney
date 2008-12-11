/**
 * 
 */
package net.sf.jmoney.navigator;

import net.sf.jmoney.model2.AbstractDataOperation;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

class DeleteAccountAction extends BaseSelectionListenerAction {
	private final Session session;

	DeleteAccountAction(Session session) {
		super(Messages.AccountsActionProvider_DeleteAccount);
		setToolTipText(Messages.AccountsActionProvider_DeleteAccount);
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		this.session = session;
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		if (selection.size() != 1) {
			return false;
		}
		Object selectedObject = selection.getFirstElement();
		return selectedObject instanceof Account;
	}

	@Override
	public void run() {
		// This action is enabled only when a single account is selected.
		final Account account = (Account)getStructuredSelection().getFirstElement();

		IOperationHistory history = PlatformUI.getWorkbench()
		.getOperationSupport().getOperationHistory();

		IUndoableOperation operation = new AbstractDataOperation(
				session, "delete account") { //$NON-NLS-1$
			@Override
			public IStatus execute() throws ExecutionException {
				if (account.getParent() != null) {
					if (account.getParent() instanceof CapitalAccount) {
						((CapitalAccount) account.getParent())
						.getSubAccountCollection().remove(
								account);
					} else {
						((IncomeExpenseAccount) account
								.getParent())
								.getSubAccountCollection().remove(
										account);
					}
				} else {
					session.deleteAccount(account);
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