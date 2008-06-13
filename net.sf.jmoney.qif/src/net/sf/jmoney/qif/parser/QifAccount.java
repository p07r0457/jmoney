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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package net.sf.jmoney.qif.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class QifAccount {       
	public String name;
	public String type;
	public String description;
	public String notes;
	public String creditLimit;
	public String statementBalanceDate;
	public String statementBalance;

	public List<QifTransaction> transactions = new ArrayList<QifTransaction>();
	public List<QifInvstTransaction> invstTransactions = new ArrayList<QifInvstTransaction>();

	// Is there a code, or just set from first transaction?
	public long startBalance = 0;

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Name: " + name + "\n");
		buf.append("Type: " + type + "\n");
		buf.append("Descrition: " + description + "\n");                     
		return buf.toString();        
	}

	public static QifAccount parseAccount(QifReader in, QifFile qifFile) throws IOException, InvalidQifFileException {
		QifAccount acc = new QifAccount();

		String line = in.readLine();
		loop: while (line != null) {
			char key = line.charAt(0);
			String value = line.substring(1);

			switch (key) {
			case 'N':
				acc.name = value;
				break;
			case 'T':
				acc.type = value;
				break;
			case 'D':
				acc.description = value;
				break;
			case 'A':
				acc.notes = value;
				break;
			case 'L':
				acc.creditLimit = value;
				break;
			case '/':
				acc.statementBalanceDate = value;
				break;
			case '$':
				acc.statementBalance = value;
				break;
			case 'X':
				// must be GnuCashToQIF... not sure what it is??? ignore it.
				break;
			case 'B': 
				// This is the 'balance' in some files.  It is undocumented so ignore.
				break;
			case '^':
				break loop;
			default:
				throw new InvalidQifFileException("Unknown field in 'account' type: " + line, in);
			}
			line = in.readLine();
		}

		line = in.peekLine();
		if (line != null
    	 && QifFile.startsWith(line, "!Type:")
    	 && QifFile.getType(line, in).isAccountTransactions()) {
			// must be transactions that follow

			if (QifFile.startsWith(line, "!Type:Invst")) {
				line = in.readLine();    // Move onto line following !Type

				do {
					QifInvstTransaction transaction = QifInvstTransaction.parseTransaction(in, qifFile);
					if (transaction == null) {
						throw new InvalidQifFileException("", in);
					}
					acc.invstTransactions.add(transaction);
				} while (in.peekLine() != null && !in.peekLine().startsWith("!"));
			} else {
				line = in.readLine();    // Move onto line following !Type

				do {
					QifTransaction transaction = QifTransaction.parseTransaction(in, qifFile);
					if (transaction == null) {
						throw new InvalidQifFileException("", in);
					}
					acc.transactions.add(transaction);
				} while (in.peekLine() != null && !in.peekLine().startsWith("!"));
			}
		}
		return acc;
	}
}
