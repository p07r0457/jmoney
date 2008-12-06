package net.sf.jmoney.popup.actions;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.AbstractDataOperation;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionDelegate;

public class DeleteAccountAction extends ActionDelegate {

    private ISelection selection = null;

	/**
     * The <code>ActionDelegate</code> implementation of this
     * <code>IActionDelegate</code> method does nothing. Subclasses may
     * reimplement.
     * <p>
     * <b>Note:</b> This method is not called directly by the proxy action. Only
     * by the default implementation of <code>runWithEvent</code> of this
     * abstract class.
     */
    @Override
    public void run(IAction action) {
			Account account = null;
			
			if (this.selection == null) {
				return;
			}
			
			IStructuredSelection selection = (IStructuredSelection)this.selection;
			for (Object selectedObject: selection.toList()) {
				account = (Account)selectedObject;
				break;
			}
			if (account != null) {
				final Session session = account.getSession();
				final Account account2 = account;

				IOperationHistory history = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();

				IUndoableOperation operation = new AbstractDataOperation(session, "delete account") { //$NON-NLS-1$
					@Override
					public IStatus execute() throws ExecutionException {
						if (account2.getParent() != null) {
							if (account2.getParent() instanceof CapitalAccount) {
								((CapitalAccount)account2.getParent()).getSubAccountCollection().remove(account2);
							} else {
								((IncomeExpenseAccount)account2.getParent()).getSubAccountCollection().remove(account2);
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

    /**
     * The <code>ActionDelegate</code> implementation of this
     * <code>IActionDelegate</code> method does nothing. Subclasses may
     * reimplement.
     */
    @Override
    public void selectionChanged(IAction action, ISelection selection) {
    	this.selection = selection;
    }
}
