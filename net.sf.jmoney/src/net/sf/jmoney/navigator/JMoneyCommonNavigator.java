package net.sf.jmoney.navigator;

import net.sf.jmoney.resources.Messages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.navigator.CommonNavigator;

public class JMoneyCommonNavigator extends CommonNavigator {
	public static String ID = "net.sf.jmoney.navigationView"; //$NON-NLS-1$
	
	/**
	 * Create the contents of this view.  When there is input we let the base
	 * class do it all but when there is no input we display an appropriate
	 * message just to be a little more user friendly.
	 */
	@Override	
	public void createPartControl(final Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		StackLayout stackLayout = new StackLayout();
		composite.setLayout(stackLayout);
		
		/*
		 * Even though this will never become visible when there is no input, we
		 * must still create it because the CommonNavigator class (from which
		 * this class is derived) will assume it has been created.
		 */
		super.createPartControl(composite);
		
		if (getSite().getPage().getInput() == null) {
			// Create the control that will be visible if no session is open
			Label noSessionMessage = new Label(composite, SWT.WRAP);
			noSessionMessage.setText(Messages.JMoneyCommonNavigator_NoSession);
			noSessionMessage.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));

			stackLayout.topControl = noSessionMessage;
		} else {
			stackLayout.topControl = getCommonViewer().getControl();
		}
	}
}
