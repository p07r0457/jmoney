package net.sf.jmoney.navigator;

import net.sf.jmoney.views.NodeEditorInput;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.navigator.ILinkHelper;

public class LinkHelper implements ILinkHelper {

	public void activateEditor(IWorkbenchPage page,	IStructuredSelection selection) {
	    if (selection == null || selection.isEmpty()) {
            return;
        }
        Object element = selection.getFirstElement();

        IEditorInput procInput = new NodeEditorInput(element, null, null, null, null);
        IEditorPart editor = page.findEditor(procInput);
        if (editor != null) {
        	page.bringToTop(editor);
        }
	}

	public IStructuredSelection findSelection(IEditorInput input) {
        if (input instanceof NodeEditorInput) {
            Object element = ((NodeEditorInput)input).getNode();
            return new StructuredSelection(element);
        }
        return StructuredSelection.EMPTY;
 	}

}
