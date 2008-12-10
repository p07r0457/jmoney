package net.sf.jmoney.navigator;

import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.PageEntry;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.views.NodeEditorInput;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.INavigatorContentService;

public class OpenAccountsAction extends BaseSelectionListenerAction {

	public OpenAccountsAction() {
		super(Messages.OpenAccountsAction_Text);
		setToolTipText(Messages.OpenAccountsAction_ToolTipText);
	}

	@Override
	public void run() {
		IStructuredSelection selection = super.getStructuredSelection();
		for (Object selectedObject: selection.toArray()) {
			if (selectedObject instanceof Account) {
				ExtendableObject extendableObject = (ExtendableObject)selectedObject;
				ExtendablePropertySet<?> propertySet = PropertySet.getPropertySet(extendableObject.getClass());
				Vector<PageEntry> pageFactories = propertySet.getPageFactories();


				Assert.isTrue(!pageFactories.isEmpty());



				final IWorkbenchPart dse = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage()
				.findView(JMoneyCommonNavigator.ID);
				if (dse != null) {
					CommonNavigator navigator = (CommonNavigator) dse;
					INavigatorContentService contentService = navigator.getNavigatorContentService();
					String description = contentService.createCommonDescriptionProvider().getDescription(selectedObject);
					ILabelProvider labelProvider = contentService.createCommonLabelProvider();
					String label = labelProvider.getText(selectedObject);
					Image image = labelProvider.getImage(selectedObject);
					navigator.getViewSite().getActionBars().getStatusLineManager().setMessage(image, description);


					// Create an editor for this node (or active if an editor
					// is already open).
					try {
						IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
						IEditorInput editorInput = new NodeEditorInput(selectedObject,
								label /*labelProvider.getText(selectedObject) */,
								image /*labelProvider.getImage(selectedObject) */,
								pageFactories,
								null);
						window.getActivePage().openEditor(editorInput,
						"net.sf.jmoney.genericEditor"); //$NON-NLS-1$
					} catch (PartInitException e) {
						JMoneyPlugin.log(e);
					}
				}				
			}
		}
	}
}
