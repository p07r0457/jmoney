package net.sf.jmoney.entrytable;


import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.SessionChangeListener;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;

public class OtherEntriesControl extends Composite {

	private Block<Entry> rootBlock;
	private RowSelectionTracker selectionTracker;
	private FocusCellTracker focusCellTracker;
	
	/**
	 * The composite containing whatever is to the left of
	 * the drop-down button.  This has a StackLayout.
	 */
	private Composite childComposite;

	private StackLayout stackLayout;
	
	/**
	 * The label that is shown if this is a split entry.
	 * This label is shown instead of all the fields for
	 * properties that come from the other entry.
	 */
	private Label splitLabel;

	/**
	 * The composite that contains the fields for
	 * properties that come from the other entry.
	 * This composite is shown only if the entry is not split.
	 */
	private SplitEntryRowControl otherEntryControl;

	/**
	 * The small drop-down button to the right that shows
	 * the split entry data.
	 */
	private Button downArrowButton;

	private EntryData entryData;

	private SessionChangeListener splitEntryListener = new SessionChangeAdapter() {

		public void objectChanged(ExtendableObject changedObject, ScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
			// TODO Auto-generated method stub
			
		}

		public void objectInserted(ExtendableObject newObject) {
			if (newObject instanceof Entry
					&& ((Entry)newObject).getTransaction() == entryData.getEntry().getTransaction()
					&& entryData.getEntry().getTransaction().getEntryCollection().size() == 3) {
				// Is now split but was not split before
				stackLayout.topControl = splitLabel;
				childComposite.layout(false);
			}
		}

		public void objectRemoved(ExtendableObject deletedObject) {
			if (deletedObject instanceof Entry
					&& ((Entry)deletedObject).getTransaction() == entryData.getEntry().getTransaction()
					&& deletedObject != entryData.getEntry()
					&& entryData.getEntry().getTransaction().getEntryCollection().size() == 2) {
				// Is now not split but was split before
				otherEntryControl.setContent(entryData.getOtherEntry());
				stackLayout.topControl = otherEntryControl;
				childComposite.layout(false);
			}
		}
	};
	
	static private Image downArrowImage = null;

	public OtherEntriesControl(Composite parent, Block<Entry> rootBlock, RowSelectionTracker selectionTracker, FocusCellTracker focusCellTracker) {
		super(parent, SWT.NONE);
		this.rootBlock = rootBlock;
		this.selectionTracker = selectionTracker;
		this.focusCellTracker = focusCellTracker;
		
//		setLayout(new DropdownButtonLayout());
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		setLayout(layout);
		
		Control childArea = createChildComposite();
		Control dropDownButton = createDownArrowButton();

		dropDownButton.setLayoutData(new GridData(10, 10));
	}

	private Control createDownArrowButton() {
		downArrowButton = new Button(this, SWT.NO_TRIM);
		if (downArrowImage == null) {
			ImageDescriptor descriptor = JMoneyPlugin.createImageDescriptor("icons/comboArrow.gif");
			downArrowImage = descriptor.createImage();
		}
		downArrowButton.setImage(downArrowImage);

		downArrowButton.addSelectionListener(new SelectionAdapter() {
		    public void widgetSelected(SelectionEvent event) {
				final OtherEntriesShell shell = new OtherEntriesShell(getShell(), SWT.ON_TOP, entryData, rootBlock);
    	        
    	        /*
				 * Position the split-entries shell below this control, unless
				 * this control is so near the bottom of the display that the
				 * shell would go off the bottom of the display, in
				 * which case position the split-entries shell above this
				 * control.
				 * 
				 * In either case, the shell should overlap this control, so if it
				 * is going downwards, align the top with the top of this control.
				 * 
				 * Note also that we put the shell one pixel to the left.  This is because
				 * a single pixel margin is always added to BlockLayout so that the
				 * selection line can be drawn.  We want the controls in the shell to
				 * exactly line up with the table header.
				 */
    	        Display display = getDisplay();
    	        Rectangle rect = display.map(OtherEntriesControl.this.getParent(), null, getBounds());
    	        shell.open(rect);
			}
		});
		
		return downArrowButton;
	}

	private Control createChildComposite() {
		childComposite = new Composite(this, SWT.NONE);
		
		stackLayout = new StackLayout();
		childComposite.setLayout(stackLayout);
		
		splitLabel = new Label(childComposite, SWT.NONE);
		splitLabel.setText("--split entry--");

		otherEntryControl = new SplitEntryRowControl(childComposite, SWT.NONE, rootBlock, selectionTracker, focusCellTracker);
		
		return childComposite;
	}
	
	public void load(final EntryData entryData) {
		// TODO: this should be done in a 'row release' method??
		if (this.entryData != null) {
			this.entryData.getEntry().getObjectKey().getSessionManager().removeChangeListener(splitEntryListener);
		}
		
		this.entryData = entryData;
		
		if (entryData.getSplitEntries().size() == 1) {
			otherEntryControl.setContent(entryData.getOtherEntry());
			stackLayout.topControl = otherEntryControl;
		} else {
			stackLayout.topControl = splitLabel;
		}
		childComposite.layout(false);

		// Listen for changes so this control is kept up to date.
		entryData.getEntry().getObjectKey().getSessionManager().addChangeListener(splitEntryListener);
	}

	public void save() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Internal class for laying out this control.  There are two child
	 * controls - the composite with the data for the other entries and
	 * a drop-down button.
	 */
	private class DropdownButtonLayout extends Layout {
		public void layout(Composite editor, boolean force) {
			Rectangle bounds = editor.getClientArea();
			Point buttonSize =  downArrowButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
			childComposite.setBounds(0, 0, bounds.width-buttonSize.x, bounds.height);
			downArrowButton.setBounds(bounds.width-buttonSize.x, 0, buttonSize.x, buttonSize.y);
		}

		public Point computeSize(Composite editor, int wHint, int hHint, boolean force) {
			/*
			 * The button is always its preferred width.  Therefore we simply pass on to the contents,
			 * after adjusting for the button width. 
			 */
			Point buttonSize =  downArrowButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
			int contentsWidthHint = (wHint == SWT.DEFAULT) ? SWT.DEFAULT : wHint - buttonSize.x; 
			Point contentsSize = childComposite.computeSize(contentsWidthHint, hHint, force);
			return new Point(contentsSize.x + buttonSize.x, contentsSize.y);
		}
	}

}
