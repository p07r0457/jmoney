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
 * Property set implementation class for the properties added
 * to each Entry object by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class QIFEntry extends AbstractEntryExtension {
	
	private char reconcilingState = ' ';
	
	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 */
	public QIFEntry() {
	}
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 * 
	 * @param reconcilingState
	 */
	public QIFEntry(char reconcilingState) {
		this.reconcilingState = reconcilingState;
	}
	
	/**
	 * Gets the status of the entry.
	 * 
	 * @return The status of the entry.
	 * <LI>
	 * <UL>' ' - the entry not not been reconciled</UL>
	 * <UL>'*' - the entry is being reconciled</UL>
	 * <UL>'C' - the entry has cleared and shows on the statement</UL>
	 * </LI>
	 */
	public char getReconcilingState() {
		return reconcilingState;
	}
	
	/**
	 * Sets the status of the entry.
	 * 
	 * @param reconcilingState the new status of the entry.
	 * <LI>
	 * <UL>' ' - the entry not not been reconciled</UL>
	 * <UL>'*' - the entry is being reconciled</UL>
	 * <UL>'C' - the entry has cleared and shows on the statement</UL>
	 * </LI>
	 */
	public void setReconcilingState(char reconcilingState) {
		char oldReconcilingState = this.reconcilingState;
		this.reconcilingState = reconcilingState;
		processPropertyChange(QIFEntryInfo.getReconcilingStateAccessor(), new Character(oldReconcilingState), new Character(reconcilingState));
	}

	static public Object [] getDefaultProperties() {
		return new Object [] { new Character(' ') };
	}
}
