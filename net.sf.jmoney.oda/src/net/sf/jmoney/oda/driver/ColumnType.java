/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2007 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.oda.driver;


/**
 * The native data types must match the values used in the dataTypeMapping
 * elements in plugin.xml.
 * 
 * @author Nigel Westbury
 *
 */
public enum ColumnType {

	stringType {
		int getNativeType() { return 1; }
		String getNativeTypeName() { return "String"; }
		int getDisplayLength() { return 10; }
	},
	integerType {
		int getNativeType() { return 2; }
		String getNativeTypeName() { return "Integer"; }
		int getDisplayLength() { return 5; }
	},
	longType {
		int getNativeType() { return 3; }
		String getNativeTypeName() { return "Long"; }
		int getDisplayLength() { return 10; }
	},
	doubleType {
		int getNativeType() { return 4; }
		String getNativeTypeName() { return "Double"; }
		int getDisplayLength() { return 10; }
	},
	booleanType {
		int getNativeType() { return 5; }
		String getNativeTypeName() { return "Boolean"; }
		int getDisplayLength() { return 10; }
	},
	dateType {
		int getNativeType() { return 6; }
		String getNativeTypeName() { return "Date"; }
		int getDisplayLength() { return 10; }
	};
	
	abstract String getNativeTypeName();
	abstract int getDisplayLength();
	int getPrecision() { return 10; };
	int getScale() { return 10; };
	abstract int getNativeType();
}
