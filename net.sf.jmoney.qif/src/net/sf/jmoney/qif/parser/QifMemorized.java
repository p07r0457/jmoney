/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (C) 2001-2008 Craig Cavanaugh, Nigel Westbury, and others
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
 *  Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package net.sf.jmoney.qif.parser;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class QifMemorized {

	public class QifAmortization {

	}
	private QifAmortization amortization;
	private String type;
	private String U;
	private String amount;
	private String payee;
	private String category;
	private String memo;
    private List<String> addressLines = new ArrayList<String>();
    private List<QifSplitTransaction> splits = new ArrayList<QifSplitTransaction>();
    
    // Amortization fields
	private String firstPaymentDate;
	private int totalYears;
	private int numberOfPaymentsMade;
	private int periodsPerYear;
	private String interestRate;
	private BigDecimal currentLoanBalance;
	private BigDecimal originalLoanAmount;

	public static QifMemorized parseMemorized(QifReader in, QifFile qifFile) throws IOException, InvalidQifFileException {
		QifMemorized transaction = new QifMemorized();

	    String splitCategory = null;
	    String splitMemo = null;
	    BigDecimal splitAmount = null;
	    String splitPercentage = null;

		String line = in.readLine();
		loop: while (line != null) {
			if (line.length() == 0) {
				throw new RuntimeException("unexpected blank line");
			}

			char key = line.charAt(0);
			String value = line.substring(1);

			// All memorized transactions begin with 'K'.

			switch (key) {
			case 'K':
				transaction.type = value;
				break;
			case 'U':
				transaction.U = value;
				break;
			case 'T':
				transaction.amount = value;
				break;
			case 'P':
				transaction.payee = value;
				break;
	    	case 'A':
	    		transaction.addressLines.add(value);
	    		break;
			case 'L':
				transaction.category = value;
				break;
			case 'M':
				transaction.memo = value;
				break;
	    	case 'S':
	    		if (splitCategory != null) {
	    			transaction.splits.add(new QifSplitTransaction(splitCategory, splitMemo, splitAmount, splitPercentage));
	    			splitCategory = null;
	    			splitMemo = null;
	    			splitAmount = null;
	    			splitPercentage = null;
	    		}
	    		splitCategory = value;
	    		break;
	    	case 'E':
	    		if (splitMemo != null) {
	    			transaction.splits.add(new QifSplitTransaction(splitCategory, splitMemo, splitAmount, splitPercentage));
	    			splitCategory = null;
	    			splitMemo = null;
	    			splitAmount = null;
	    			splitPercentage = null;
	    		}
	    		splitMemo = value;
	    		break;
	    	case '$':
	    		if (splitAmount != null) {
	    			transaction.splits.add(new QifSplitTransaction(splitCategory, splitMemo, splitAmount, splitPercentage));
	    			splitCategory = null;
	    			splitMemo = null;
	    			splitAmount = null;
	    			splitPercentage = null;
	    		}
	    		splitAmount = QifFile.parseMoney(value);
	    		break;
	    	case '%':
	    		if (splitPercentage != null) {
	    			transaction.splits.add(new QifSplitTransaction(splitCategory, splitMemo, splitAmount, splitPercentage));
	    			splitCategory = null;
	    			splitMemo = null;
	    			splitAmount = null;
	    			splitPercentage = null;
	    		}
	    		splitPercentage = value;
	    		break;
	    	case '1':
	    		transaction.firstPaymentDate = value;
	    		qifFile.processDate(value, in);
	    		break;
	    	case '2':
	    		transaction.totalYears = QifFile.parseInteger(value, line, in);
	    		break;
	    	case '3':
	    		transaction.numberOfPaymentsMade = QifFile.parseInteger(value, line, in);
	    		break;
	    	case '4':
	    		transaction.periodsPerYear = QifFile.parseInteger(value, line, in);
	    		break;
	    	case '5':
	    		transaction.interestRate = value;
	    		break;
	    	case '6':
	    		transaction.currentLoanBalance = QifFile.parseMoney(value);
	    		break;
	    	case '7':
	    		transaction.originalLoanAmount = QifFile.parseMoney(value);
	    		break;
			case '^':
				break loop;
			default:
				throw new InvalidQifFileException("Unknown field in 'Memorized' type: " + line, in);
			}
			line = in.readLine();
		}
		
		return transaction;
	}

	public String getType() {
		return type;
	}
	public String getU() {
		return U;
	}
	public String getAmount() {
		return amount;
	}
	public String getPayee() {
		return payee;
	}
	public String getCategory() {
		return category;
	}
	public String getMemo() {
		return memo;
	}
	public List<QifSplitTransaction> getSplits() {
		return Collections.unmodifiableList(splits);
	}
}
