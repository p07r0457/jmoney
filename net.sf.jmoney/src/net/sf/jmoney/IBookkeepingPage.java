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

import net.sf.jmoney.views.NodeEditor;

import org.eclipse.ui.forms.editor.IFormPage;

/**
 * Interface that must be implemented by all classes that
 * implement an extension to the net.sf.jmoney.pages extension
 * point.
 *
 * @author Nigel Westbury
 */
public interface IBookkeepingPage {
	/**
	 * Create the form page.
	 * 
	 * @param selectedObject The object selected in the navigation view.
	 * @param parent The parent composite control to which the control
	 * 			objects are to be added.
	 */
	IFormPage createFormPage(NodeEditor editor);
	
}
