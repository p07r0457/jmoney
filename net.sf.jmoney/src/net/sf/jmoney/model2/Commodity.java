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
 *
 * @author  Nigel
 */
public interface Commodity extends IExtendableObject {
    /**
     * @return the name of the currency.
     */
    String getName();
    
    /**
     * Set the name of the currency.
     */
    void setName(String name);
    
    /**
     * Converts an amount of this commodity from string to integer
     * format.
     *
     * @return a long value representing the amount of this commodity
     * in units that are at least as small as the smallest possible
     * quantity of the commodity.
     */
    long parse(String amountString);
    
    /**
     * Converts an amount of this commodity from integer to string format.
     */
    String format(long amount);
    
	/**
	 * Although one normally uses the parse and format methods, this method
	 * is useful when dividing one commodity quantity by another.
	 * 
	 * @return the scale factor for this currency (10 to the number of decimals).
	 */
	short getScaleFactor();
}
