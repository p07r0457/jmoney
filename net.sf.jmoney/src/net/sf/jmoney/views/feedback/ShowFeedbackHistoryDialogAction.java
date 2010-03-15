package net.sf.jmoney.views.feedback;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;



/**
 * Invoke the resource creation wizard selection Wizard.
 * This action will retarget to the active view.
 */
class ShowFeedbackHistoryDialogAction extends Action {
	private FeedbackView fFeedbackView;


	/*
	 *	Create a new instance of this class
	 */
	public ShowFeedbackHistoryDialogAction(FeedbackView feedbackView) {
		super("History..."); 
		fFeedbackView= feedbackView;
	}
	 
	@Override
	public void run() {
		List<Feedback> input = fFeedbackView.getQueries();
		FeedbackHistorySelectionDialog dlg= new FeedbackHistorySelectionDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), input, fFeedbackView);
		
		Feedback current= fFeedbackView.getCurrentFeedback();
		if (current != null) {
			Object[] selected= new Object[1];
			selected[0]= current;
			dlg.setInitialSelections(selected);
		}
		if (dlg.open() == Window.OK) {
			Object[] result= dlg.getResult();
			if (result != null && result.length == 1) {
				Feedback feedback = (Feedback) result[0];
				if (dlg.isOpenInNewView()) {
					try {
						FeedbackView view = (FeedbackView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(FeedbackView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);
						view.showResults(feedback);
					} catch (PartInitException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					fFeedbackView.showResults(feedback);
				}
			}
		}

	}
}
