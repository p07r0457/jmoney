package net.sf.jmoney.navigator;

import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.AbstractDataOperation;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CapitalAccountInfo;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.IncomeExpenseAccountInfo;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.views.AccountsNode;
import net.sf.jmoney.views.CategoriesNode;
import net.sf.jmoney.views.TreeNode;
import net.sf.jmoney.wizards.NewAccountWizard;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.DrillDownAdapter;

public class AccountsActionProvider extends CommonActionProvider {

	private OpenAccountsAction openAction = new OpenAccountsAction();

	private DrillDownAdapter drillDownAdapter;

	private Vector<Action> newAccountActions = new Vector<Action>();
	private Action newCategoryAction;
	private Action deleteAccountAction;

	private boolean fHasContributedToViewMenu = false;

	
	@Override
	public void init(ICommonActionExtensionSite site) {
		super.init(site);
		
		drillDownAdapter = new DrillDownAdapter((TreeViewer)site.getStructuredViewer());
		
		makeActions();
	}
	
	@Override
	public void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();

		openAction.selectionChanged(selection);
		if (openAction.isEnabled()) {
			manager.insertAfter(ICommonMenuConstants.GROUP_OPEN, openAction);
		}
		
		manager.add(new Separator());

		// TODO: move selection stuff into the actions.
		Object selectedObject = selection.getFirstElement();
		if (selectedObject != null) {  // Needed???
		
		if (selectedObject == TreeNode.getTreeNode(AccountsNode.ID) 
				|| selectedObject instanceof CapitalAccount) {
			for (Action newAccountAction: newAccountActions) {
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


//		ActionGroup ag = new UndoRedoActionGroup(
//				getActionSite().getViewSite(),
//				PlatformUI.getWorkbench().getOperationSupport().getUndoContext(),
//				true);
//		ag.fillContextMenu(manager);
		
		}


		drillDownAdapter.addNavigationActions(manager);

		// Other plug-ins can contribute there actions here
		manager.add(new Separator(ICommonMenuConstants.GROUP_ADDITIONS));
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
		if (selection.size() == 1 && selection.getFirstElement() instanceof Account) {
			openAction.selectionChanged(selection);
			actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openAction);
		}

		if (!fHasContributedToViewMenu ) {
			fillLocalPullDown(actionBars.getMenuManager());
			fillLocalToolBar(actionBars.getToolBarManager());
			fHasContributedToViewMenu = true;
		}

		/*
		 * This action provider is used for navigation trees
		 * in view parts only.  The CommonViewerSite object will
		 * therefore in fact implement ICommonViewerWorkbenchSite.
		 * ICommonViewerWorkbenchSite would not be implemented if
		 * the tree is in a dialog.
		 */
		ICommonViewerWorkbenchSite site = (ICommonViewerWorkbenchSite)getActionSite().getViewSite();
		
		ActionGroup undoRedoActionGroup = new UndoRedoActionGroup(
				site.getSite(), 
				site.getWorkbenchWindow().getWorkbench().getOperationSupport().getUndoContext(),
				true);
		undoRedoActionGroup.fillActionBars(actionBars);
	}

	private void fillLocalPullDown(IMenuManager manager) {
		for (Action newAccountAction: newAccountActions) {
			manager.add(newAccountAction);
		}
		manager.add(newCategoryAction);
		manager.add(new Separator());
		manager.add(deleteAccountAction);
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		drillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		/*
		 * For each class of object derived (directly or indirectly) from the
		 * capital account class, and that is not itself derivable, add a menu
		 * item to create a new account of that type.
		 */
		for (final ExtendablePropertySet<? extends CapitalAccount> derivedPropertySet: CapitalAccountInfo.getPropertySet().getDerivedPropertySets()) {

			Object [] messageArgs = new Object[] {
					derivedPropertySet.getObjectDescription()
			};

			String text = new java.text.MessageFormat(
							JMoneyPlugin.getResourceString("MainFrame.newAccount"), 
							java.util.Locale.US)
					.format(messageArgs);

			String tooltip = new java.text.MessageFormat(
							"Create a New {0}", 
							java.util.Locale.US)
					.format(messageArgs);
			
			Action newAccountAction = new BaseSelectionListenerAction(text) {
				@Override	
				public void run() {
					CapitalAccount account = null;
					IStructuredSelection selection = super.getStructuredSelection();
					for (Object selectedObject: selection.toList()) {
						if (selectedObject instanceof CapitalAccount) {
							account = (CapitalAccount)selectedObject;
							break;
						}
					}

					Session session = JMoneyPlugin.getDefault().getSession();
					
					NewAccountWizard wizard = new NewAccountWizard(session, account, derivedPropertySet);
					WizardDialog dialog = new WizardDialog(getActionSite().getViewSite().getShell(), wizard);
					dialog.setPageSize(600, 300);
					int result = dialog.open();
					if (result == WizardDialog.OK) {
						// Having added the new account, set it as the selected
						// account in the tree viewer.
						getActionSite().getStructuredViewer().setSelection(new StructuredSelection(wizard.getNewAccount()), true);
					}
				}
			};

			newAccountAction.setToolTipText(tooltip);
			newAccountAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
					getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

			newAccountActions.add(newAccountAction);
		}

		Object [] messageArgs = new Object[] {
				IncomeExpenseAccountInfo.getPropertySet().getObjectDescription()
		};

		String text = new java.text.MessageFormat(
						JMoneyPlugin.getResourceString("MainFrame.newAccount"), 
						java.util.Locale.US)
				.format(messageArgs);

		String tooltip = new java.text.MessageFormat(
						"Create a New {0}", 
						java.util.Locale.US)
				.format(messageArgs);

		newCategoryAction = new BaseSelectionListenerAction(text) {
			@Override	
			public void run() {
				IncomeExpenseAccount account = null;
				IStructuredSelection selection = super.getStructuredSelection();
				for (Object selectedObject: selection.toList()) {
					if (selectedObject instanceof IncomeExpenseAccount) {
						account = (IncomeExpenseAccount)selectedObject;
						break;
					}
				}

				Session session = JMoneyPlugin.getDefault().getSession();
				
				NewAccountWizard wizard = new NewAccountWizard(session, account);
				WizardDialog dialog = new WizardDialog(getActionSite().getViewSite().getShell(), wizard);
				dialog.setPageSize(600, 300);
				int result = dialog.open();
				if (result == WizardDialog.OK) {
					// Having added the new account, set it as the selected
					// account in the tree viewer.
					getActionSite().getStructuredViewer().setSelection(new StructuredSelection(wizard.getNewAccount()), true);
				}
			}
		};

		newCategoryAction.setToolTipText(tooltip);

		newCategoryAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

		deleteAccountAction = new BaseSelectionListenerAction(JMoneyPlugin.getResourceString("MainFrame.deleteAccount")) {
			@Override	
			public void run() {
				final Session session = JMoneyPlugin.getDefault().getSession();
				Account account = null;
				IStructuredSelection selection = super.getStructuredSelection();
				for (Object selectedObject: selection.toList()) {
					account = (Account)selectedObject;
					break;
				}
				if (account != null) {
					final Account account2 = account;

					IOperationHistory history = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();

					IUndoableOperation operation = new AbstractDataOperation(session, "delete account") {
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
		};
		deleteAccountAction.setToolTipText("Delete an account");
		deleteAccountAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
	}

}
