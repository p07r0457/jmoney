/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2005 Nigel Westbury <westbury@users.sourceforge.net>
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
*/
package net.sf.jmoney.reports;

import net.sf.jasperreports.engine.fill.JRFillField;
import net.sf.jasperreports.engine.fill.JRFillVariable;

/**
 * 
 * @author Nigel Westbury
 */
abstract class EvaluationType {
	static EvaluationType normal = new EvaluationType() {
    	Object getValue(JRFillField field) { return field.getValue(); }
    	Object getValue(JRFillVariable variable)  { return variable.getValue(); }
	};
	static EvaluationType old = new EvaluationType() {
		Object getValue(JRFillField field) { return field.getOldValue(); }
		Object getValue(JRFillVariable variable)  { return variable.getOldValue(); }
	};
	static EvaluationType estimated = new EvaluationType() {
		Object getValue(JRFillField field) { return field.getValue(); }
		Object getValue(JRFillVariable variable)  { return variable.getEstimatedValue(); }
	};
	
	abstract Object getValue(JRFillField field);
	abstract Object getValue(JRFillVariable variable);
}

/* A slightly better version for when we move to Java 5 source compatability.    
enum EvaluationType {
	normal {
    	Object getValue(JRFillField field) { return field.getValue(); }
    	Object getValue(JRFillVariable variable)  { return variable.getValue(); }
	},
	old {
		Object getValue(JRFillField field) { return field.getOldValue(); }
		Object getValue(JRFillVariable variable)  { return variable.getOldValue(); }
	}, 
	estimated {
		Object getValue(JRFillField field) { return field.getValue(); }
		Object getValue(JRFillVariable variable)  { return variable.getEstimatedValue(); }
	};
	
	abstract Object getValue(JRFillField field);
	abstract Object getValue(JRFillVariable variable);
}
*/        
