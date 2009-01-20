package net.sf.jmoney.handlers;

import java.util.Map;

import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
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
 *           commandId=&quot;net.sf.jmoney.commands.openXxx&quot;&gt;
 *         &lt;enabledWhen&gt;
 *           &lt;with variable=&quot;activeWorkbenchWindow&quot;&gt;
 *                 &lt;test property=&quot;net.sf.jmoney.core.isSessionOpen&quot;/&gt;
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
public class OpenEditorHandler extends AbstractHandler {

	private static final String PARAMETER_EDITOR_ID = "net.sf.jmoney.openEditor.editorId";

	//	private String editorId;

	//	public void setInitializationData(IConfigurationElement config,
	//			String propertyName, Object data) throws CoreException {
	//		for (IConfigurationElement classElements : config.getChildren("class")) { //$NON-NLS-1$
	//			for (IConfigurationElement parameterElements : classElements.getChildren("parameter")) { //$NON-NLS-1$
	//				String parameterName = parameterElements.getAttribute("name"); //$NON-NLS-1$
	//				if (parameterName.equals("editorId")) {	 //$NON-NLS-1$
	//					editorId = parameterElements.getAttribute("value"); //$NON-NLS-1$
	//				}
	//			}
	//		}
	//	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Map parameters = event.getParameters();
		final String editorId = (String) parameters.get(PARAMETER_EDITOR_ID);

		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IEditorInput editorInput = new SessionEditorInput();
			window.getActivePage().openEditor(editorInput, editorId);
		} catch (PartInitException e) {
			JMoneyPlugin.log(e);
		}

		return null;
	}
}
