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

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JRCompiler;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.fill.JRCalculator;

/**
 * This class is a dummy implementation of Jasper Report's JRCompiler interface.
 * <P>
 * The JRCompiler interface is designed to provide an implementation that takes
 * a JasperDesign object, generate the source code for an appropriate
 * JRCalculator class, compile the source code and then provide a method to load
 * the compiled class.
 * <P>
 * This implementation is a much simpler implementation that returns a
 * calculator that has been pre-set into a static field.
 * 
 * @author Nigel Westbury
 */
public class MyDummyCompiler implements JRCompiler {

	public static JRCalculator calculator;

	public JasperReport compileReport(JasperDesign jasperDesign)
			throws JRException {
		throw new RuntimeException("should not be called");
	}

	public JRCalculator loadCalculator(JasperReport jasperReport)
			throws JRException {
		return calculator;
	}

}
