package net.sf.jmoney.navigator;

import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.views.TreeNode;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
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
			throw new RuntimeException("");
		}
	}

	@Override
	public String getText(Object element) {
		if (element instanceof TreeNode) {
			return ((TreeNode)element).getLabel();
		} else if (element instanceof ExtendableObject) {
			return ((ExtendableObject)element).toString();
		}
		return "should never happen";
	}

	public String getDescription(Object element) {
		if (element instanceof TreeNode) {
			return "Root of All Capital Accounts";
		}
		if (element instanceof CapitalAccount) {
			return "Capital Account: " + ((CapitalAccount)element).getName();
		}
		return "should never happen";
	}

}
