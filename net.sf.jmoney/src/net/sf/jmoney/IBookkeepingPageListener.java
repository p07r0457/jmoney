/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney;

import net.sf.jmoney.model2.Session;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;

/**
 * @author Nigel
 *
 * All classes that implement extensions to the net.sf.jmoney.pages 
 * extension point must implement this interface.
 */
public interface IBookkeepingPageListener {
	/**
	 * This method is always called before any other methods are called.
	 *   
	 * @param parent The memento containing the previous state, if any,
	 * 		of pages created by this extension.
	 */
	void init(IMemento memento);

	void saveState(IMemento memento);	

	/**
	 * Given the object selected in the navigation view, return
	 * the number of TabItem objects to show in the folder view.
	 * 
	 * @param selectedObject The object selected in the navigation view.
	 * @return count
	 */
	int getPageCount(Object selectedObject);

	/**
	 * Given the object selected in the navigation view, initialize
	 * the list of TabItem objects to show in the folder view and
	 * return a list of them.
	 * 
	 * @param selectedObject The object selected in the navigation view.
	 * @param parent The parent composite control to which the control
	 * 			objects are to be added.
	 */
	BookkeepingPage[] createPages(Object selectedObject, Session session, Composite parent);
	
	public class BookkeepingPage {
		private Control control;
		private String text;

		public BookkeepingPage(Control control, String text) {
			this.control = control;
			this.text = text;
		}
		
		public Control getControl() {
			return control;
		}

		public String getText() {
			return text;
		}
	}
}
