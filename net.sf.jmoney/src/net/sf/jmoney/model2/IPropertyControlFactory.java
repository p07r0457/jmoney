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

package net.sf.jmoney.model2;

import org.eclipse.swt.widgets.Composite;

/**
 * @author Nigel
 *
 */
public interface IPropertyControlFactory {
	/**
	 * Create a control that edits the property.
	 * <P>
	 * The PropertyAccessor object is not known when the factory
	 * is created so we require that it is passed as a parameter
	 * when a control is created.

	 * @return An interface to the class that wraps the
	 * 			control.
	 */
	IPropertyControl createPropertyControl(Composite parent, PropertyAccessor propertyAccessor);
}
