package net.sf.jmoney;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * This class implements the "isSessionOpen" property on a window.
 * It is used when handlers are defined in plugin.xml files that need
 * to know if a session is open when determining their enablement state.
 */
public class SessionTester extends PropertyTester {

	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		IWorkbenchWindow window = (IWorkbenchWindow)receiver;
		if ("isSessionOpen".equals(property)) {
			Object input = window.getActivePage().getInput();

			return expectedValue == null
			? (input != null)
					: (input != null) == ((Boolean)expectedValue).booleanValue();
		}
		Assert.isTrue(false);
		return false;
	}
}
