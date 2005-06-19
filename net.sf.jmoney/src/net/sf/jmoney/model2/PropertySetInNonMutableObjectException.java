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

/**
 * Exception raised if an attempt is made to alter the datastore while
 * the datastore is in read-only mode.
 *
 * In order to support transactions and high level change events in the
 * datastore, properties may not be set into the datastore objects unless
 * a specific request has first been made to update the datastore.
 * Any attempt to call a getter method when the datastore is in
 * read-only mode will result in this exception.
 *
 * @author  Nigel
 */
public class PropertySetInNonMutableObjectException extends RuntimeException {
    
	private static final long serialVersionUID = -1094792377244984225L;

	/** Creates a new instance of PropertySetInNonMutableObjectException */
    public PropertySetInNonMutableObjectException(String propertyName, Class propertyOwningClass) {
    }
    
}
