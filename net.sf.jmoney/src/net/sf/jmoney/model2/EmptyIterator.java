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

import java.util.*;

/**
 *
 * @author  Nigel
 */
public class EmptyIterator implements Iterator {
    
    /** Creates a new instance of EmptyIterator */
    public EmptyIterator() {
    }
    
    /**
     * Returns <tt>false</tt> to indicate that there are never any more elements.
     */
    public boolean hasNext() {
        return false;
    }

    /**
     * Always throws NoSuchElementException because there are never any elements
     * in the iteration.
     *
     * @exception NoSuchElementException iteration has no more elements.
     */
    public Object next() {
        throw new NoSuchElementException();
    }

    /**
     * This method is not supported because there are never any elements in this iterator.
     *
     * @exception UnsupportedOperationException if the <tt>remove</tt>
     *		  operation is not supported by this Iterator.
     *
     * @exception IllegalStateException if the <tt>next</tt> method has not
     *		  yet been called, or the <tt>remove</tt> method has already
     *		  been called after the last call to the <tt>next</tt>
     *		  method.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
