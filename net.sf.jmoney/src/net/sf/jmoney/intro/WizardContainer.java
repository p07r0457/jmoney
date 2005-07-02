/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Nigel Westbury - modified for use in JMoney
 *******************************************************************************/
package net.sf.jmoney.intro;
import java.util.ArrayList;

import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer2;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.intro.IIntroPart;

/**
 * A composite to show a wizard to the end user. 
 * <p>
 * In typical usage, the client instantiates this class with 
 * a particular wizard. The dialog serves as the wizard container
 * and orchestrates the presentation of its pages.
 * <p>
 * The standard layout is roughly as follows: 
 * it has an area at the top containing both the
 * wizard's title, description, and image; the actual wizard page
 * appears in the middle; and at the bottom
 * of the page is message line and a button bar containing 
 * Help, Next, Back, Close, and Standby/Maximize buttons (or some subset).
 * </p>
 */
public class WizardContainer extends Composite implements IWizardContainer2 {
	/**
	 * Image registry key for error message image (value <code>"dialog_title_error_image"</code>).
	 */
	public static final String WIZ_IMG_ERROR = "dialog_title_error_image"; //$NON-NLS-1$
	
	// The wizard the dialog is currently showing.
	private IWizard wizard;
	// The introduction part in which this composite is contained
	IIntroPart introPart;
	// Wizards to dispose
	private ArrayList createdWizards = new ArrayList();
	// Current nested wizards
	private ArrayList nestedWizards = new ArrayList();
	// The currently displayed page.
	private IWizardPage currentPage = null;
	
	/**
	 * The dialog area; <code>null</code> until dialog is layed out.
	 */
	protected Composite dialogArea;

	/**
	 * The button bar; <code>null</code> until dialog is layed out.
	 */
	public Control buttonBar;
	
	// Navigation buttons
	private Button backButton;
	private Button nextButton;
	private Button closeButton;
	private Button standbyButton;
	private Button helpButton;

	private boolean isMovingToPreviousPage = false;
	private Composite pageContainer;
	private PageContainerFillLayout pageContainerLayout = new PageContainerFillLayout(0, 0, 300,
			225);
	private int pageWidth = SWT.DEFAULT;
	private int pageHeight = SWT.DEFAULT;

	/**
	 * Indicates whether the container is in standby or full
	 * screen mode.
	 */
	private boolean standby = false;
	
	/**
	 * A layout for a container which includes several pages, like
	 * a notebook, wizard, or preference dialog. The size computed by
	 * this layout is the maximum width and height of all pages currently
	 * inserted into the container.
	 */
	protected class PageContainerFillLayout extends Layout {
		/**
		 * The margin width; <code>5</code> pixels by default.
		 */
		public int marginWidth = 5;
		/**
		 * The margin height; <code>5</code> pixels by default.
		 */
		public int marginHeight = 5;
		/**
		 * The minimum width; <code>0</code> pixels by default.
		 */
		public int minimumWidth = 0;
		/**
		 * The minimum height; <code>0</code> pixels by default.
		 */
		public int minimumHeight = 0;
		/**
		 * Creates new layout object.
		 *
		 * @param mw the margin width
		 * @param mh the margin height
		 * @param minW the minimum width
		 * @param minH the minimum height
		 */
		public PageContainerFillLayout(int mw, int mh, int minW, int minH) {
			marginWidth = mw;
			marginHeight = mh;
			minimumWidth = minW;
			minimumHeight = minH;
		}
		/* (non-Javadoc)
		 * Method declared on Layout.
		 */
		public Point computeSize(Composite composite, int wHint, int hHint, boolean force) {
			if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT)
				return new Point(wHint, hHint);
			Point result = null;
			Control[] children = composite.getChildren();
			if (children.length > 0) {
				result = new Point(0, 0);
				for (int i = 0; i < children.length; i++) {
					Point cp = children[i].computeSize(wHint, hHint, force);
					result.x = Math.max(result.x, cp.x);
					result.y = Math.max(result.y, cp.y);
				}
				result.x = result.x + 2 * marginWidth;
				result.y = result.y + 2 * marginHeight;
			} else {
				Rectangle rect = composite.getClientArea();
				result = new Point(rect.width, rect.height);
			}
			result.x = Math.max(result.x, minimumWidth);
			result.y = Math.max(result.y, minimumHeight);
			if (wHint != SWT.DEFAULT)
				result.x = wHint;
			if (hHint != SWT.DEFAULT)
				result.y = hHint;
			return result;
		}
		/**
		 * Returns the client area for the given composite according to this layout.
		 *
		 * @param c the composite
		 * @return the client area rectangle
		 */
		public Rectangle getClientArea(Composite c) {
			Rectangle rect = c.getClientArea();
			rect.x = rect.x + marginWidth;
			rect.y = rect.y + marginHeight;
			rect.width = rect.width - 2 * marginWidth;
			rect.height = rect.height - 2 * marginHeight;
			return rect;
		}
		/* (non-Javadoc)
		 * Method declared on Layout.
		 */
		public void layout(Composite composite, boolean force) {
			Rectangle rect = getClientArea(composite);
			Control[] children = composite.getChildren();
			for (int i = 0; i < children.length; i++) {
				children[i].setBounds(rect);
			}
		}
		/**
		 * Lays outs the page according to this layout.
		 *
		 * @param w the control
		 */
		public void layoutPage(Control w) {
			w.setBounds(getClientArea(w.getParent()));
		}
		/**
		 * Sets the location of the page so that its origin is in the
		 * upper left corner.
		 *
		 * @param w the control
		 */
		public void setPageLocation(Control w) {
			w.setLocation(marginWidth, marginHeight);
		}
	}
	/**
	 * Creates a new wizard dialog for the given wizard. 
	 *
	 * @param parentShell the parent shell
	 * @param newWizard the wizard this dialog is working on
	 */
	public WizardContainer(Composite parent, IWizard newWizard, IIntroPart introPart) {
		super(parent, SWT.NONE);
		setWizard(newWizard);
		this.introPart = introPart;
	}

	
	/**
	 * The Back button has been pressed.
	 */
	protected void backPressed() {
		IWizardPage page = currentPage.getPreviousPage();
		if (page == null)
			// should never happen since we have already visited the page
			return;
		// set flag to indicate that we are moving back
		isMovingToPreviousPage = true;
		// show the page
		showPage(page);
	}
	
	/**
	 * Calculates the difference in size between the given
	 * page and the page container. A larger page results 
	 * in a positive delta.
	 *
	 * @param page the page
	 * @return the size difference encoded
	 *   as a <code>new Point(deltaWidth,deltaHeight)</code>
	 */
	private Point calculatePageSizeDelta(IWizardPage page) {
		Control pageControl = page.getControl();
		if (pageControl == null)
			// control not created yet
			return new Point(0, 0);
		Point contentSize = pageControl.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		Rectangle rect = pageContainerLayout.getClientArea(pageContainer);
		Point containerSize = new Point(rect.width, rect.height);
		return new Point(Math.max(0, contentSize.x - containerSize.x), Math.max(0, contentSize.y
				- containerSize.y));
	}
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#close()
	 */

	public boolean close() {
		// inform wizards
		for (int i = 0; i < createdWizards.size(); i++) {
			IWizard createdWizard = (IWizard) createdWizards.get(i);
			createdWizard.dispose();
			// Remove this dialog as a parent from the managed wizard.
			// Note that we do this after calling dispose as the wizard or
			// its pages may need access to the container during
			// dispose code
			createdWizard.setContainer(null);
		}

		// Close the container
		introPart
		.getIntroSite()
		.getWorkbenchWindow()
		.getWorkbench()
		.getIntroManager()
		.closeIntro(introPart);
		
		return true;
	}

	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		if (wizard.isHelpAvailable()) {
			helpButton = createButton(parent,
					IDialogConstants.HELP_LABEL, false);
			helpButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					helpPressed();
				}
			});
			
		}

		if (wizard.needsPreviousAndNextButtons())
			createPreviousAndNextButtons(parent);

		closeButton = createButton(parent,
				IDialogConstants.CLOSE_LABEL, true);
		closeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				closePressed();
			}
		});
		
		standbyButton = createButton(parent,
				JMoneyPlugin.getResourceString("Intro.standby"), //$NON-NLS-1$
				false);
		standbyButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				standbyPressed();
			}
		});
	}
	
	
	/**
	 * Creates and returns the contents of this dialog's button bar.
	 * <p>
	 * The <code>Dialog</code> implementation of this framework method lays
	 * out a button bar and calls the <code>createButtonsForButtonBar</code>
	 * framework method to populate it. Subclasses may override.
	 * </p>
	 * <p>
	 * The returned control's layout data must be an instance of
	 * <code>GridData</code>.
	 * </p>
	 * <p>
	 * The <code>WizardDialog</code> implementation of this framework method
	 * prevents the composite's columns from being made equal width in order
	 * to remove the margin between the Back and Next buttons.
	 * </p>
	 * 
	 * @param parent
	 *            the parent composite to contain the button bar
	 * @return the button bar control
	 */
	protected Control createButtonBar(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		// create a layout with spacing and margins appropriate for the font
		// size.
		GridLayout layout = new GridLayout();
		layout.numColumns = 0; // this is incremented by createButton
		layout.makeColumnsEqualWidth = false;  // necessary so back and next buttons touch

		composite.setLayout(layout);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END
				| GridData.VERTICAL_ALIGN_CENTER);
		composite.setLayoutData(data);
		composite.setFont(parent.getFont());
		// Add the buttons to the button bar.
		createButtonsForButtonBar(composite);
		return composite;
	}
	
	
	/**
	 * Creates a new button with the given id.
	 * <p>
	 * The <code>Dialog</code> implementation of this framework method creates
	 * a standard push button, registers it for selection events including
	 * button presses, and registers default buttons with its shell. The button
	 * id is stored as the button's client data. If the button id is
	 * <code>IDialogConstants.CANCEL_ID</code>, the new button will be
	 * accessible from <code>getCancelButton()</code>. If the button id is
	 * <code>IDialogConstants.OK_ID</code>, the new button will be accesible
	 * from <code>getOKButton()</code>. Note that the parent's layout is
	 * assumed to be a <code>GridLayout</code> and the number of columns in
	 * this layout is incremented. Subclasses may override.
	 * </p>
	 * 
	 * @param parent
	 *            the parent composite
	 * @param id
	 *            the id of the button (see <code>IDialogConstants.*_ID</code>
	 *            constants for standard dialog button ids)
	 * @param label
	 *            the label from the button
	 * @param defaultButton
	 *            <code>true</code> if the button is to be the default button,
	 *            and <code>false</code> otherwise
	 * 
	 * @return the new button
	 * 
	 * @see #getCancelButton
	 * @see #getOKButton()
	 */
	protected Button createButton(Composite parent, String label,
			boolean defaultButton) {
		// increment the number of columns in the button bar
		((GridLayout) parent.getLayout()).numColumns++;
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);
		button.setFont(JFaceResources.getDialogFont());
		if (defaultButton) {
			Shell shell = parent.getShell();
			if (shell != null) {
				shell.setDefaultButton(button);
			}
		}
		setButtonLayoutData(button);
		return button;
	}
	
	
	/**
	 * Set the layout data of the button to a GridData with appropriate heights
	 * and widths.
	 * 
	 * @param button
	 */
	protected void setButtonLayoutData(Button button) {
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		button.setLayoutData(data);
	}
	
	/**
	 * The <code>WizardDialog</code> implementation of this <code>Window</code>
	 * method calls call <code>IWizard.addPages</code> to allow the current
	 * wizard to add extra pages, then <code>super.createContents</code> to create
	 * the controls. It then calls <code>IWizard.createPageControls</code>
	 * to allow the wizard to pre-create their page controls prior to opening,
	 * so that the wizard opens to the correct size. And finally it
	 * shows the first page.
	 */
	public Control createContents(Composite parent) {
		// Allow the wizard to add pages to itself
		// Need to call this now so page count is correct
		// for determining if next/previous buttons are needed
		wizard.addPages();
		
		
		// This class is a composite class.
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		setLayout(layout);
		setLayoutData(new GridData(GridData.FILL_BOTH));

		// create the dialog area and button bar
		dialogArea = createDialogArea(this);
		buttonBar = createButtonBar(this);
		Control contents = this;
		
		// Show the first page
		showStartingPage();
		return contents;
	}

	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected Composite createDialogArea(Composite parent) {
		// create a composite with standard margins and spacing
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		
		// When maximized, the margins are 15.
		// These margins are set to zero if the user puts the container in standby mode.
		layout.marginWidth = 15;
		layout.marginHeight = 15;

		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// Build the Page container
		pageContainer = createPageContainer(composite);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = pageWidth;
		gd.heightHint = pageHeight;
		pageContainer.setLayoutData(gd);
		pageContainer.setFont(parent.getFont());

		// Build the separator line
		Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		return composite;
	}
	/**
	 * Creates the container that holds all pages.
	 * @param parent
	 * @return Composite
	 */
	private Composite createPageContainer(Composite parent) {
		Composite result = new Composite(parent, SWT.NULL);
		result.setLayout(pageContainerLayout);
		return result;
	}
	
	/**
	 * Creates the Previous and Next buttons for this wizard dialog.
	 * Creates standard (<code>SWT.PUSH</code>) buttons and registers for their
	 * selection events. Note that the number of columns in the button bar composite
	 * is incremented. These buttons are created specially to prevent any space 
	 * between them.
	 *
	 * @param parent the parent button bar
	 * @return a composite containing the new buttons
	 */
	private Composite createPreviousAndNextButtons(Composite parent) {
		// increment the number of columns in the button bar
		((GridLayout) parent.getLayout()).numColumns++;
		Composite composite = new Composite(parent, SWT.NONE);
		// create a layout with spacing and margins appropriate for the font size.
		GridLayout layout = new GridLayout();
		layout.numColumns = 0; // will be incremented by createButton
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_CENTER
				| GridData.VERTICAL_ALIGN_CENTER);
		composite.setLayoutData(data);
		composite.setFont(parent.getFont());

		backButton = createButton(composite,
				IDialogConstants.BACK_LABEL,
				false);
		backButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				backPressed();
			}
		});

		nextButton = createButton(composite, 
				IDialogConstants.NEXT_LABEL,
				false);
		nextButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				nextPressed();
			}
		});
		
		return composite;
	}
	/**
	 * Creates and return a new wizard closing dialog without openiong it.
	 * @return MessageDalog
	 *
	private MessageDialog createWizardClosingDialog() {
		MessageDialog result = new MessageDialog(getShell(), JFaceResources
				.getString("WizardClosingDialog.title"), //$NON-NLS-1$
				null, JFaceResources.getString("WizardClosingDialog.message"), //$NON-NLS-1$
				MessageDialog.QUESTION, new String[]{IDialogConstants.OK_LABEL}, 0);
		return result;
	}
	*/

	/**
	 * The Close button has been pressed.
	 */
	protected void closePressed() {
		// Wizards are added to the nested wizards list in setWizard.
		// This means that the current wizard is always the last wizard in the list.
		// Note that we first call the current wizard directly (to give it a chance to 
		// abort, do work, and save state) then call the remaining n-1 wizards in the 
		// list (to save state).
		if (wizard.performFinish()) {
			// Call perform finish on outer wizards in the nested chain
			// (to allow them to save state for example)
			for (int i = 0; i < nestedWizards.size() - 1; i++) {
				((IWizard) nestedWizards.get(i)).performFinish();
			}
			
			// Hard close the intro part 
			close();
		}
	}

	protected void standbyPressed() {
		// Flip the 'standby' state of the container
		introPart
		.getIntroSite()
		.getWorkbenchWindow()
		.getWorkbench()
		.getIntroManager()
		.setIntroStandby(introPart, !standby);
	}

	/**
	 * Called externally to notify this object that the standby
	 * mode has changed.
	 * 
	 * The text in the standby/maximize button is switched as
	 * appropriate.
	 * 
	 * @param standby
	 */
	public void standbyStateChanged(boolean standby) {
		this.standby = standby;
		
		standbyButton.setText(
				JMoneyPlugin.getResourceString(
						standby 
						? "Intro.maximize" //$NON-NLS-1$
						: "Intro.standby")); //$NON-NLS-1$

		// The margins are 0 in standby and 15 in full screen
		GridLayout layout = (GridLayout)dialogArea.getLayout();
		layout.marginWidth = standby ? 0 : 15;
		layout.marginHeight = standby ? 0 : 15;
		dialogArea.layout(false);
	}
	
	/* (non-Javadoc)
	 * Method declared on IWizardContainer.
	 */
	public IWizardPage getCurrentPage() {
		return currentPage;
	}
	/**
	 * Returns the progress monitor for this wizard dialog (if it has one).
	 *
	 * @return the progress monitor, or <code>null</code> if
	 *   this wizard dialog does not have one
	 */
/*	
	protected IProgressMonitor getProgressMonitor() {
		return progressMonitorPart;
	}
*/	
	/**
	 * Returns the wizard this dialog is currently displaying.
	 *
	 * @return the current wizard
	 */
	protected IWizard getWizard() {
		return wizard;
	}

	/**
	 * The Help button has been pressed.
	 */
	protected void helpPressed() {
		if (currentPage != null) {
			currentPage.performHelp();
		}
	}
	/**
	 * The Next button has been pressed.
	 */
	protected void nextPressed() {
		IWizardPage page = currentPage.getNextPage();
		if (page == null) {
			// something must have happend getting the next page
			return;
		}
		// show the next page
		showPage(page);
	}

	/**
	 * Restores the enabled/disabled state of the given control.
	 *
	 * @param w the control
	 * @param h the map (key type: <code>String</code>, element type:
	 *   <code>Boolean</code>)
	 * @param key the key
	 * @see #saveEnableStateAndSet
	 *
	private void restoreEnableState(Control w, Map h, String key) {
		if (w != null) {
			Boolean b = (Boolean) h.get(key);
			if (b != null)
				w.setEnabled(b.booleanValue());
		}
	}
    */

	/**
	 * Restores the enabled/disabled state of the wizard dialog's
	 * buttons and the tree of controls for the currently showing page.
	 *
	 * @param state a map containing the saved state as returned by 
	 *   <code>saveUIState</code>
	 * @see #saveUIState
	 *
	private void restoreUIState(Map state) {
		restoreEnableState(backButton, state, "back"); //$NON-NLS-1$
		restoreEnableState(nextButton, state, "next"); //$NON-NLS-1$
		restoreEnableState(closeButton, state, "close"); //$NON-NLS-1$
		restoreEnableState(standbyButton, state, "standby"); //$NON-NLS-1$
		restoreEnableState(helpButton, state, "help"); //$NON-NLS-1$
		Object pageValue = state.get("page"); //$NON-NLS-1$
		if(pageValue != null)//page may never have been created
			((ControlEnableState) pageValue).restore();
	}
    */

	/* (non-Javadoc)
	 * Method declared on IRunnableContext.
	 */
	public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) {
		throw new RuntimeException("not implemented");
	}
	
	/**
	 * Saves the enabled/disabled state of the given control in the
	 * given map, which must be modifiable.
	 *
	 * @param w the control, or <code>null</code> if none
	 * @param h the map (key type: <code>String</code>, element type:
	 *   <code>Boolean</code>)
	 * @param key the key
	 * @param enabled <code>true</code> to enable the control, 
	 *   and <code>false</code> to disable it
	 * @see #restoreEnableState(Control, Map, String)
	 *
	private void saveEnableStateAndSet(Control w, Map h, String key, boolean enabled) {
		if (w != null) {
			h.put(key, new Boolean(w.getEnabled()));
			w.setEnabled(enabled);
		}
	}
    */

	/**
	 * Captures and returns the enabled/disabled state of the wizard dialog's
	 * buttons and the tree of controls for the currently showing page. All
	 * these controls are disabled in the process, with the possible excepton of
	 * the Cancel button.
	 *
	 * @param keepCancelEnabled <code>true</code> if the Cancel button should
	 *   remain enabled, and <code>false</code> if it should be disabled
	 * @return a map containing the saved state suitable for restoring later
	 *   with <code>restoreUIState</code>
	 * @see #restoreUIState
	 *
	private Map saveUIState(boolean keepCancelEnabled) {
		Map savedState = new HashMap(10);
		saveEnableStateAndSet(backButton, savedState, "back", false); //$NON-NLS-1$
		saveEnableStateAndSet(nextButton, savedState, "next", false); //$NON-NLS-1$
		saveEnableStateAndSet(closeButton, savedState, "close", false); //$NON-NLS-1$
		saveEnableStateAndSet(standbyButton, savedState, "standby", false); //$NON-NLS-1$
		saveEnableStateAndSet(helpButton, savedState, "help", false); //$NON-NLS-1$
		if(currentPage != null)
			savedState.put("page", ControlEnableState.disable(currentPage.getControl())); //$NON-NLS-1$
		return savedState;
	}
    */
	/**
	 * Sets the given cursor for all shells currently active
	 * for this window's display.
	 *
	 * @param c the cursor
	 *
	private void setDisplayCursor(Cursor c) {
		Shell[] shells = getShell().getDisplay().getShells();
		for (int i = 0; i < shells.length; i++)
			shells[i].setCursor(c);
	}
    */

	/**
	 * Sets the minimum page size used for the pages.
	 *
	 * @param minWidth the minimum page width
	 * @param minHeight the minimum page height
	 * @see #setMinimumPageSize(Point)
	 */
	public void setMinimumPageSize(int minWidth, int minHeight) {
		Assert.isTrue(minWidth >= 0 && minHeight >= 0);
		pageContainerLayout.minimumWidth = minWidth;
		pageContainerLayout.minimumHeight = minHeight;
	}
	/**
	 * Sets the minimum page size used for the pages.
	 *
	 * @param size the page size encoded as
	 *   <code>new Point(width,height)</code>
	 * @see #setMinimumPageSize(int,int)
	 */
	public void setMinimumPageSize(Point size) {
		setMinimumPageSize(size.x, size.y);
	}
	/**
	 * Sets the size of all pages.
	 * The given size takes precedence over computed sizes.
	 *
	 * @param width the page width
	 * @param height the page height
	 * @see #setPageSize(Point)
	 */
	public void setPageSize(int width, int height) {
		pageWidth = width;
		pageHeight = height;
	}
	/**
	 * Sets the size of all pages.
	 * The given size takes precedence over computed sizes.
	 *
	 * @param size the page size encoded as
	 *   <code>new Point(width,height)</code>
	 * @see #setPageSize(int,int)
	 */
	public void setPageSize(Point size) {
		setPageSize(size.x, size.y);
	}
	/**
	 * Sets the wizard this dialog is currently displaying.
	 *
	 * @param newWizard the wizard
	 */
	protected void setWizard(IWizard newWizard) {
		wizard = newWizard;
		wizard.setContainer(this);
		if (!createdWizards.contains(wizard)) {
			createdWizards.add(wizard);
			// New wizard so just add it to the end of our nested list
			nestedWizards.add(wizard);
		} else {
			// We have already seen this wizard, if it is the previous wizard
			// on the nested list then we assume we have gone back and remove 
			// the last wizard from the list
			int size = nestedWizards.size();
			if (size >= 2 && nestedWizards.get(size - 2) == wizard)
				nestedWizards.remove(size - 1);
			else
				// Assume we are going forward to revisit a wizard
				nestedWizards.add(wizard);
		}
	}
	/* (non-Javadoc)
	 * Method declared on IWizardContainer.
	 */
	public void showPage(IWizardPage page) {
		if (page == null || page == currentPage) {
			return;
		}
		if (!isMovingToPreviousPage)
			// remember my previous page.
			page.setPreviousPage(currentPage);
		else
			isMovingToPreviousPage = false;
/* 		
		// Update for the new page ina busy cursor if possible
		// getContents returns the top level control for the window,
		// the shell?
		if (getContents() == null)
			updateForPage(page);
		else {
*/		
		{
			final IWizardPage finalPage = page;
			BusyIndicator.showWhile(getDisplay(), new Runnable() {
				public void run() {
					updateForPage(finalPage);
				}
			});
		}
	}
	/**
	 * Update the receiver for the new page.
	 * @param page
	 */
	private void updateForPage(IWizardPage page) {
		// ensure this page belongs to the current wizard
		if (wizard != page.getWizard())
			setWizard(page.getWizard());
		// ensure that page control has been created
		// (this allows lazy page control creation)
		if (page.getControl() == null) {
			page.createControl(pageContainer);
			// the page is responsible for ensuring the created control is accessable
			// via getControl.
			Assert.isNotNull(page.getControl());
			// ensure the dialog is large enough for this page
			updateSize(page);
		}
		// make the new page visible
		IWizardPage oldPage = currentPage;
		currentPage = page;
		currentPage.setVisible(true);
		if(oldPage != null) {
			oldPage.setVisible(false);
		}
		// update the dialog controls
		update();
	}
	/**	
	 * Shows the starting page of the wizard.
	 */
	private void showStartingPage() {
		currentPage = wizard.getStartingPage();
		if (currentPage == null) {
			// something must have happend getting the page
			return;
		}
		// ensure the page control has been created
		if (currentPage.getControl() == null) {
			currentPage.createControl(pageContainer);
			// the page is responsible for ensuring the created control is accessable
			// via getControl.
			Assert.isNotNull(currentPage.getControl());
			// we do not need to update the size since the call
			// to initialize bounds has not been made yet.
		}
		// make the new page visible
		currentPage.setVisible(true);
		// update the dialog controls
		update();
	}
	/**
	 * Updates this dialog's controls to reflect the current page.
	 */
	public void update() {
		// Update the window title
		updateWindowTitle();
		// Update the title bar
		updateTitleBar();
		// Update the buttons
		updateButtons();
	}
	/* (non-Javadoc)
	 * Method declared on IWizardContainer.
	 */
	public void updateButtons() {
		boolean canFlipToNextPage = false;
		boolean canFinish = wizard.canFinish();
		if (backButton != null)
			backButton.setEnabled(currentPage.getPreviousPage() != null);
		if (nextButton != null) {
			canFlipToNextPage = currentPage.canFlipToNextPage();
			nextButton.setEnabled(canFlipToNextPage);
		}
		closeButton.setEnabled(canFinish);
		// close is default unless it is diabled and next is enabled
		if (canFlipToNextPage && !canFinish)
			getShell().setDefaultButton(nextButton);
		else
			getShell().setDefaultButton(closeButton);
	}

	/* (non-Javadoc)
	 * Method declared on IWizardContainer.
	 */
	public void updateMessage() {
		// This container does not display messages
	}
	
	/**
	 * Computes the correct dialog size for the current page and resizes 
	 * its shell if nessessary. Also causes the container to refresh its
	 * layout.
	 * 
	 * @param page the wizard page to use to resize the dialog
	 * @since 2.0
	 */
	protected void updateSize(IWizardPage page) {
		if (page == null || page.getControl() == null)
			return;
		updateSizeForPage(page);
		pageContainerLayout.layoutPage(page.getControl());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizardContainer2#updateSize()
	 */
	public void updateSize() {
		updateSize(currentPage);
	}
	/**
	 * Computes the correct dialog size for the given page and resizes 
	 * its shell if nessessary.
	 *
	 * @param page the wizard page
	 */
	private void updateSizeForPage(IWizardPage page) {
		// ensure the page container is large enough
		Point delta = calculatePageSizeDelta(page);
		if (delta.x > 0 || delta.y > 0) {
			// increase the size of the shell 
			Shell shell = getShell();
			Point shellSize = shell.getSize();
			
			this.setSize(shellSize.x + delta.x, shellSize.y + delta.y);
		}
	}
	/**
	 * Computes the correct dialog size for the given wizard and resizes 
	 * its shell if nessessary.
	 *
	 * @param sizingWizard the wizard
	 *
	private void updateSizeForWizard(IWizard sizingWizard) {
		Point delta = new Point(0, 0);
		IWizardPage[] pages = sizingWizard.getPages();
		for (int i = 0; i < pages.length; i++) {
			// ensure the page container is large enough
			Point pageDelta = calculatePageSizeDelta(pages[i]);
			delta.x = Math.max(delta.x, pageDelta.x);
			delta.y = Math.max(delta.y, pageDelta.y);
		}
		if (delta.x > 0 || delta.y > 0) {
			// increase the size of the shell 
			Shell shell = getShell();
			Point shellSize = shell.getSize();
			this.setSize(shellSize.x + delta.x, shellSize.y + delta.y);
		}
	}
    */

	/* (non-Javadoc)
	 * Method declared on IWizardContainer.
	 */
	public void updateTitleBar() {
		// This wizard container has no title bar so there
		// is nothing to do here.
	}

	/* (non-Javadoc)
	 * Method declared on IWizardContainer.
	 */
	public void updateWindowTitle() {
		// This wizard container has no window title so there
		// is nothing to do here.
	}
	
}