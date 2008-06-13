/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (C) 2001-2008 Craig Cavanaugh, Johann Gyger, and others
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
import java.util.List;


public class QifInvstTransaction {

	private String date;
	private String action;
	private BigDecimal amount;
	private String status = null;
	private String text;

	private String number;
	private String payee = null;
	private String memo = "";
	private String category = null;
	private String U;
	private String security;
	private String price;
	private String quantity;
	private BigDecimal commission;
	private String type;
	private String amountTrans;
	private List<QifSplitTransaction> splits = new ArrayList<QifSplitTransaction>();
	private List<String> address = new ArrayList<String>();

    private QifFile qifFile;
    
	public QifInvstTransaction(QifFile qifFile) {
		this.qifFile = qifFile;
	}

	public static QifInvstTransaction parseTransaction(QifReader in, QifFile qifFile) throws IOException, InvalidQifFileException {
		QifInvstTransaction tran = new QifInvstTransaction(qifFile);

		String splitCategory = null;
		String splitMemo = null;
		BigDecimal splitAmount = null;
		String splitPercentage = null;

		String line = in.readLine();
		loop: while (line != null) {
			char key = line.charAt(0);
			String value = line.substring(1);

			switch (key) {
			case 'D':
				tran.date = value;
				qifFile.processDate(value, in);
				break;
			case 'U':
				tran.U = value;
				break;
			case 'T':
				tran.amount = QifFile.parseMoney(value);
				break;
			case 'C':
				tran.status = value;
				break;
			case 'P':
				tran.payee = value;
				break;
			case 'L':
				tran.category = value;
				break;
			case 'N': // trans type for inv accounts
				tran.number = value;
				break;
			case 'M':
				tran.memo = value;
				break;
			case 'A':
				tran.address.add(value);
				break;
			case 'Y':
				tran.security = value;
				break;
			case 'I':
				tran.price = value;
				break;
			case 'Q':
				tran.quantity = value;
				break;
			case '$': // must check before split trans checks... Does Quicken allow for split investment transactions?
				if (tran.amountTrans == null) {
					tran.amountTrans = value;
				} else {
					// Assume this is in a split.
					if (splitAmount != null) {
						tran.splits.add(new QifSplitTransaction(splitCategory, splitMemo, splitAmount, splitPercentage));
						splitCategory = null;
						splitMemo = null;
						splitAmount = null;
						splitPercentage = null;
					}
					splitAmount = QifFile.parseMoney(value);
					break;
				}
				break;
			case '^':
				break loop;
			case 'S':
				if (splitCategory != null) {
					tran.splits.add(new QifSplitTransaction(splitCategory, splitMemo, splitAmount, splitPercentage));
					splitCategory = null;
					splitMemo = null;
					splitAmount = null;
					splitPercentage = null;
				}
				splitCategory = value;
				break;
			case 'E':
				if (splitMemo != null) {
					tran.splits.add(new QifSplitTransaction(splitCategory, splitMemo, splitAmount, splitPercentage));
					splitCategory = null;
					splitMemo = null;
					splitAmount = null;
					splitPercentage = null;
				}
				splitMemo = value;
				break;
			case '%':
				if (splitPercentage != null) {
					tran.splits.add(new QifSplitTransaction(splitCategory, splitMemo, splitAmount, splitPercentage));
					splitCategory = null;
					splitMemo = null;
					splitAmount = null;
					splitPercentage = null;
				}
				splitPercentage = value;
				break;
			default:
				throw new InvalidQifFileException("Unknown field in 'invst' type: " + line, in);
			}

			line = in.readLine();
		}

	    // TODO: Do a few more checks on splits.
	    // Perhaps the splits transaction constructor
	    // should do the checks?
	    if (splitCategory != null) {
	    	tran.splits.add(new QifSplitTransaction(splitCategory, splitMemo, splitAmount, splitPercentage));
	    }
        	
		return tran;
	}

	public QifDate getDate() {
		return qifFile.parseDate(date);
	}

	public String getSecurity() {
		return security;
	}

	public String getQuantity() {
		return quantity;
	}
}
