/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Johann Gyger <jgyger@users.sf.net>
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package net.sf.jmoney.fields;

import java.util.Calendar;
import java.util.Date;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertyAccessor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.vafada.swtcalendar.SWTCalendar;
import org.vafada.swtcalendar.SWTCalendarEvent;
import org.vafada.swtcalendar.SWTCalendarListener;

/**
 * Editor class for date properties. These dates are formatted according to the
 * selected format in the preferences.
 *
 * @author Johann Gyger
 */
public class DateEditor implements IPropertyControl {

    protected VerySimpleDateFormat fDateFormat;
    protected PropertyAccessor fPropertyAccessor;
    protected Composite fPropertyControl;
    protected ExtendableObject fExtendableObject;

	/**
	 * Image registry key for three dot image (value <code>"cell_editor_dots_button_image"</code>).
	 */
	public static final String CELL_EDITOR_IMG_DOTS_BUTTON = "cell_editor_dots_button_image";//$NON-NLS-1$

	/**
	 * The text box containing the date
	 */
	private Text textControl;

	/**
	 * The small button to the right of the date that brings up the date picker
	 */
	private Button button;

	static private Image threeDotsImage = null;

	SWTCalendar swtcal = null;
	
	/**
     * Create a new date editor.
     */
    public DateEditor(final Composite parent, PropertyAccessor propertyAccessor, VerySimpleDateFormat dateFormat) {
        fPropertyAccessor = propertyAccessor;
        fDateFormat = dateFormat;
        
		Font font = parent.getFont();

		fPropertyControl = new Composite(parent, SWT.NULL);
		fPropertyControl.setFont(font);
		fPropertyControl.setLayout(new DialogCellLayout());

		textControl = new Text(fPropertyControl, SWT.LEFT);
		textControl.setFont(fPropertyControl.getFont());
		
		textControl.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == '+' || e.character == '-') {
					
		            Calendar calendar = Calendar.getInstance();
		            calendar.setTime(
		            		fDateFormat.parse(
		            				textControl.getText()
		            		)
		            );
	            	
					if (e.character == '+') {
						calendar.add(Calendar.DAY_OF_MONTH, 1);
					} else {
		            	calendar.add(Calendar.DAY_OF_MONTH, -1);
					}
					
					textControl.setText(
							fDateFormat.format(
									calendar.getTime()
							)
					);

					e.doit = false;
				}
			}
		});
		
		button = new Button(fPropertyControl, SWT.DOWN);
		if (threeDotsImage == null) {
			ImageDescriptor descriptor = JMoneyPlugin.createImageDescriptor("icons/dots_button.gif");
			threeDotsImage = descriptor.createImage();
		}
		button.setImage(threeDotsImage);

		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				final Shell shell = new Shell(parent.getShell(), SWT.NONE);
		        shell.setLayout(new RowLayout());
    	        swtcal = new SWTCalendar(shell);
                
                // Set the currently set date into the calendar control
                // (If the parse method returned null then the text control did not
                // contain a valid date.  In this case no date is set into the
                // date picker).
                Date date = fDateFormat.parse(textControl.getText());
                if (date != null) {
        	        Calendar calendar = Calendar.getInstance();
        	        calendar.setTime(date);
        	        swtcal.setCalendar(calendar);
                }
                
                swtcal.addSWTCalendarListener(
                		new SWTCalendarListener() {
                			public void dateChanged(SWTCalendarEvent calendarEvent) {
                				Date date = calendarEvent.getCalendar().getTime();
                				textControl.setText(fDateFormat.format(date));
                			}
                		});

    	        shell.pack();
    	        
    	        // This is an attempt to locate the calendar control beneath
    	        // the date control.  This is not trivial because the calendar
    	        // control is a shell.  We cannot make it a control with the same
    	        // parent as the date control because then it would not overlap
    	        // the lower controls outside the parent control.
    	        // We go back through the ancestors to try to get the co-ordinates
    	        // of the date control relative to the shell.  This works reasonably
    	        // well except that on Windows we need a fudge factor of 50
    	        // (perhaps because of window trimmings).
    	        // There is probably a better way of doing this.
    	        
    	        Point size = fPropertyControl.getSize();
    	        int x = 0;
    	        int y = size.y + 2 + 50;
    	        for (Composite ancestor = fPropertyControl; ancestor != null; ancestor = ancestor.getParent()) {
    	        	Point location = ancestor.getLocation();
    	        	x += location.x;
    	        	y += location.y;
    	        }
    	        shell.setLocation(x, y);
    	        shell.open();
    	        
    	        shell.addShellListener(new ShellAdapter() {
    	        	public void shellDeactivated(ShellEvent e) {
    	        		shell.close();
    	        		swtcal = null;
    	        	}
    	        });
			}
		});
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#load(net.sf.jmoney.model2.ExtendableObject)
     */
    public void load(ExtendableObject object) {
    	this.fExtendableObject = object;
    	if (object == null) {
            textControl.setText("");
    	} else {
            Date d = (Date) object.getPropertyValue(fPropertyAccessor);
            this.textControl.setText(fDateFormat.format(d));
    	}
    	fPropertyControl.setEnabled(object != null);
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#save()
     */
    public void save() {
        String text = textControl.getText();
        fExtendableObject.setPropertyValue(fPropertyAccessor, fDateFormat.parse(text));
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#getControl()
     */
    public Control getControl() {
        return fPropertyControl;
    }

    /**
	 * Internal class for laying out the dialog.
	 */
	private class DialogCellLayout extends Layout {
		public void layout(Composite editor, boolean force) {
			Rectangle bounds = editor.getClientArea();
			Point size = textControl.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
			textControl.setBounds(0, 0, bounds.width-size.y, bounds.height);
			button.setBounds(bounds.width-size.y, 0, size.y, bounds.height);
		}
		public Point computeSize(Composite editor, int wHint, int hHint, boolean force) {
			if (JMoneyPlugin.DEBUG) System.out.println("wHint =" + wHint + ", " + hHint);
			if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT)
				return new Point(wHint, hHint);
			Point contentsSize = textControl.computeSize(SWT.DEFAULT, SWT.DEFAULT, force); 
			Point buttonSize =  button.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
			if (JMoneyPlugin.DEBUG) System.out.println("contents =" + contentsSize.x + ", " + contentsSize.y);
			if (JMoneyPlugin.DEBUG) System.out.println("contents =" + buttonSize.x + ", " + buttonSize.y);
			// Just return the button width to ensure the button is not clipped
			// if the label is long.  Date text needs 60
			// The label will just use whatever extra width there is
			Point result = new Point(60 + buttonSize.x,
//							        Math.max(contentsSize.y, buttonSize.y));
				contentsSize.y);
			return result;			
		}
	}
	
}