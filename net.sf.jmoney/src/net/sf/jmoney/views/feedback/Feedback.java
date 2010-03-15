package net.sf.jmoney.views.feedback;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;

public class Feedback {
	private String description;
	private IStatus rootStatus;

	public Feedback(String description, IStatus rootStatus) {
		this.description = description;
		this.rootStatus = rootStatus;
	}

	/**
	 * Returns the full description of the result messages.
	 */
	String getFullDescription() {
		return description;
	}

	/**
	 * Returns a short description of the action to which this feedback applies.
	 * Cuts off after 30 characters and adds ... The description set by the
	 * client where {0} will be replaced by the match count.
	 * 
	 * @return the short description
	 */
	String getShortDescription() {
		String text= getFullDescription();
		int separatorPos= text.indexOf(" - "); //$NON-NLS-1$
		if (separatorPos < 1)
			return text.substring(0, Math.min(50, text.length())) + "..."; // use first 50 characters //$NON-NLS-1$
		if (separatorPos < 30)
			return text;	// don't cut
		if (text.charAt(0) == '"')
			return text.substring(0, Math.min(30, text.length())) + "...\" - " + text.substring(Math.min(separatorPos + 3, text.length())); //$NON-NLS-1$
		return text.substring(0, Math.min(30, text.length())) + "... - " + text.substring(Math.min(separatorPos + 3, text.length())); //$NON-NLS-1$
	}

	public IStatus getRootStatus() {
		return rootStatus;
	}
	
	/** 
	 * Image used when feedback results are displayed in a list.
	 *  
	 * @return the image descriptor
	 */
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getLabel() {
		return getShortDescription();
	}

	public String getTooltip() {
		return getFullDescription();
	}

	/**
	 * This method re-executes whatever was run to create these errors
	 * in the first place.
	 * <P>
	 * This method will never throw an exception.  Instead an IStatus object
	 * is set to indicate any errors.
	 */
	public void executeAgain() {
		// TODO Auto-generated method stub
		
	}
}

