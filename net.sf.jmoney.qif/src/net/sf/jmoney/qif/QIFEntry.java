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

package net.sf.jmoney.qif;

import net.sf.jmoney.model2.AbstractEntryExtension;

/**
 * @author Nigel
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class QIFEntry extends AbstractEntryExtension {
		
		protected char reconcilingState = ' ';

		/** Creates a new instance.
		 * A default constructor is mandatory for all extension objects.
		 */
		public QIFEntry() {
		}
		
		/**
		 * Returns the status.
		 */
		public char getReconcilingState() {
			return reconcilingState;
		}

		/**
		 * Sets the check. Either UNCLEARED, RECONCILING or CLEARED.
	         *
	         * At this point of time, any change to an extension causes a change notification
	         * on the base entry object itself.  TODO: Most consumers will be aware of only 
	         * one or a few extension types so it is not efficient to notify them when any
	         * other extension is changed.  Think about whether it is worth bringing notifications
	         * down to a finer granularity.
		 */
		public void setReconcilingState(char reconcilingState) {
	                char oldReconcilingState = this.reconcilingState;
			this.reconcilingState = reconcilingState;
			firePropertyChange("reconcilingState", oldReconcilingState, reconcilingState);
		}
	}
