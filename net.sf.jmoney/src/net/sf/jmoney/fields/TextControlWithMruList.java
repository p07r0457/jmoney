/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2006 Nigel Westbury <westbury@users.sourceforge.net>
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

import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;

public class TextControlWithMruList extends TextComposite {
    protected List textList;
    protected Text textbox;

    protected LinkedList<String> recentlyUsedList = new LinkedList<String>();
    
    public TextControlWithMruList(Composite parent) {
		super(parent, SWT.NONE);
		
		setLayout(new GridLayout(1, false));
		
        textList = new List(this, SWT.NONE);
        textbox = new Text(this, SWT.NONE);

        /*
         * The preferred height (default height) of an empty list
         * box is a height that is sufficient to show about
         * five items.  The preferred height of a list box that
         * contains items is a height that is sufficient to show
         * those items.  We want the list box heights to be the
         * same regardless of whether they start empty or with
         * items, so we set the height in a grid data item.
         */
        GridData gdList = new GridData(SWT.FILL, SWT.FILL, true, true);
        gdList.heightHint = 80;
        textList.setLayoutData(gdList);
        textbox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        textList.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				String [] selection = textList.getSelection();
				if (selection.length >= 1) {
					textbox.setText(selection[0]);
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}});
	}
    
	public String getText() {
		return textbox.getText();
	}

    public void setText(String text) {
		textbox.setText(text);
	}

	public void rememberChoice() {
    	String text = textbox.getText();
    	if (text.length() != 0) {
    		if (recentlyUsedList.size() != 0) {
    			int index = recentlyUsedList.indexOf(text);
    			if (index == -1) {
    				// Drop off head if list is already full
    	    		if (recentlyUsedList.size() >= 5) {
    	    			recentlyUsedList.removeFirst();
    	    			textList.remove(0);
    	    		}
    			} else {
    				recentlyUsedList.remove(text);
    				textList.remove(index);
    			}
    		}
    		recentlyUsedList.addLast(text);
    		textList.add(text);
    	}
    }

	public void init(IMemento memento) {
		if (memento != null) {
			IMemento [] mruTextMementos = memento.getChildren("mruText");
			for (int i = 0; i < mruTextMementos.length; i++) {
				String text = mruTextMementos[i].getString("text");
	    		recentlyUsedList.addLast(text);
	    		textList.add(text);
			}
		}
	}

	public void saveState(IMemento memento) {
		for (Iterator iter = recentlyUsedList.iterator(); iter.hasNext(); ) {
			String text = (String)iter.next();
			memento.createChild("mruText").putString("text", text);
		}
	}
}