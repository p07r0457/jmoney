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

import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;

public class TextControlWithMruCombo extends TextComposite {

	protected Combo combo;
    protected LinkedList<String> recentlyUsedList = new LinkedList<String>();
    
    public TextControlWithMruCombo(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(new FillLayout());
    	combo = new Combo(this, SWT.NONE);
	}
    
    @Override	
	public void rememberChoice() {
    	String text = combo.getText();
    	if (text.length() != 0) {
    		if (recentlyUsedList.size() != 0) {
    			int index = recentlyUsedList.indexOf(text);
    			if (index == -1) {
    				// Drop off head if list is already full
    	    		if (recentlyUsedList.size() >= 10) {
    	    			recentlyUsedList.removeFirst();
    	    			combo.remove(0);
    	    		}
    			} else {
    				recentlyUsedList.remove(text);
    				combo.remove(index);
    			}
    		}
    		recentlyUsedList.addLast(text);
    		combo.add(text);
    	}
    }

    @Override	
	public String getText() {
		return combo.getText();
	}

    @Override	
	public void setText(String text) {
		combo.setText(text);
	}

    @Override	
	public void init(IMemento memento) {
		if (memento != null) {
			IMemento [] mruTextMementos = memento.getChildren("mruText");
			for (int i = 0; i < mruTextMementos.length; i++) {
				String text = mruTextMementos[i].getString("text");
	    		recentlyUsedList.addLast(text);
	    		combo.add(text);
			}
		}
	}

    @Override	
	public void saveState(IMemento memento) {
		for (String text: recentlyUsedList) {
			memento.createChild("mruText").putString("text", text);
		}
	}
}
