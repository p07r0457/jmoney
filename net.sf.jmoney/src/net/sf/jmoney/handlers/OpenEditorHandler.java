package net.sf.jmoney.handlers;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.views.NodeEditorInput;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * This is a generic handler that can be used to open an editor. The input to
 * the editor is the session, so this handler can be used whenever the editor is
 * editing the session as a whole and does not need anything more specific as
 * input.
 * 
 * To use this handler implementation, declare your handler as follows:
 * 
 * <pre>
 * &lt;handler
 *           commandId=&quot;net.sf.jmoney.commands.openShoebox&quot;&gt;
 *         &lt;enabledWhen&gt;
 *           &lt;with variable=&quot;activeContexts&quot;&gt;
 *              &lt;iterate operator=&quot;or&quot;&gt;
 *                 &lt;equals value=&quot;net.sf.jmoney.sessionOpen&quot;/&gt;
 *              &lt;/iterate&gt;
 *           &lt;/with&gt;
 *        &lt;/enabledWhen&gt;
 *         &lt;class
 *               class=&quot;net.sf.jmoney.handlers.OpenEditorHandler&quot;&gt;
 *    &lt;parameter name=&quot;editorId&quot; 
 *                           value=&quot;&lt;&lt;&lt;put your editor id here&gt;&gt;&gt;&quot;/&gt;
 *         &lt;/class&gt;
 *     &lt;/handler&gt;
 * </pre>
 */
public class OpenEditorHandler extends AbstractHandler implements IExecutableExtension {

	private String editorId;

	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		for (IConfigurationElement classElements : config.getChildren("class")) {
			for (IConfigurationElement parameterElements : classElements.getChildren("parameter")) {
				String parameterName = parameterElements.getAttribute("name");
				if (parameterName.equals("editorId")) {	
					editorId = parameterElements.getAttribute("value");
				}
			}
		}
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IEditorInput editorInput = new SessionEditorInput(JMoneyPlugin.getDefault().getSession(), null);
			window.getActivePage().openEditor(editorInput, editorId);
		} catch (PartInitException e) {
			JMoneyPlugin.log(e);
		}

		return null;
	}
}