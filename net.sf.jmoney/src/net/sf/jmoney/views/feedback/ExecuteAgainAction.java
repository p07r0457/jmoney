package net.sf.jmoney.views.feedback;

import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.jface.action.Action;

class ExecuteAgainAction extends Action {
	private FeedbackView fView;
	
	public ExecuteAgainAction(FeedbackView view) {
		setToolTipText("Perform the Current Action Again"); 
		setImageDescriptor(JMoneyPlugin.imageDescriptorFromPlugin(JMoneyPlugin.PLUGIN_ID, "icons/elcl16/search_again.gif"));
		fView = view;	
	}

	@Override
	public void run() {
		/*
		 * This action should not be enabled if no results are being shown or if
		 * the action succeeded (albeit with warnings or info), or if the action
		 * cannot be re-executed.
		 */
		Feedback feedback = fView.getCurrentFeedback();
		feedback.executeAgain();
		fView.showResults(feedback);
	}
}
