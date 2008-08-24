package net.sf.jmoney.navigator;

import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.views.TreeNode;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.navigator.IDescriptionProvider;

public class AccountsLabelProvider extends LabelProvider implements ILabelProvider, IDescriptionProvider {

	@Override	
	public Image getImage(Object obj) {
		if (obj instanceof TreeNode) {
			return ((TreeNode)obj).getImage();
		} else if (obj instanceof ExtendableObject) {
			ExtendableObject extendableObject = (ExtendableObject)obj;
			return PropertySet.getPropertySet(extendableObject.getClass()).getIcon();
		} else {
			throw new RuntimeException(Messages.AccountsLabelProvider_Image);
		}
	}

	@Override
	public String getText(Object element) {
		if (element instanceof TreeNode) {
			return ((TreeNode)element).getLabel();
		} else if (element instanceof ExtendableObject) {
			return ((ExtendableObject)element).toString();
		}
		return Messages.AccountsLabelProvider_DefaultText;
	}

	public String getDescription(Object element) {
		if (element instanceof TreeNode) {
			return Messages.AccountsLabelProvider_TreeNodeDescription;
		}
		if (element instanceof CapitalAccount) {
			return NLS.bind(Messages.AccountsLabelProvider_CapitalAccountDescription,((CapitalAccount)element).getName());
		}
		return Messages.AccountsLabelProvider_DefaultDescription;
	}

}