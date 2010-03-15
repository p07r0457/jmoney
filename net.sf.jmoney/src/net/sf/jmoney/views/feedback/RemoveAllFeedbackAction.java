package net.sf.jmoney.views.feedback;

import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.jface.action.Action;

class RemoveAllFeedbackAction extends Action {

	private FeedbackView fFeedbackView;

	public RemoveAllFeedbackAction(FeedbackView feedbackView) {
		super("Clear History"); 
		setImageDescriptor(JMoneyPlugin.imageDescriptorFromPlugin(JMoneyPlugin.PLUGIN_ID, "icons/elcl16/search_remall.gif"));
		fFeedbackView= feedbackView;
	}
	
	public void run() {
		fFeedbackView.removeAllFeedbackResults();
	}
}
