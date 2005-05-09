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

package net.sf.jmoney.reconciliation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jmoney.model2.CurrencyAccount;

import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Provides an implementation of the net.sf.jmoney.reconciliation.bankstatements
 * extension point.  This extension supports the import of Financial Exchange
 * files (OFX and QFX files).
 * 
 * @author Nigel Westbury
 */
public class OfxImport implements IBankStatementSource {

	private static Pattern headerDatePattern = Pattern.compile("^(\\w*):(\\w*)$");
	private static Pattern elementPattern = Pattern.compile("^\\s*<([A-Z][\\w.]*)>(.*)$");
	private static Pattern elementEndPattern = Pattern.compile("^\\s*</([A-Z][\\w.]*)>$");

	private static Pattern datePattern = Pattern.compile("^(\\d\\d\\d\\d)(\\d\\d)(\\d\\d)\\d{6}");
	
	// These are member variables only so we don't have to pass
	// them down the stack.  The values are not held across non-private method
	// calls.
    private BufferedReader buffer;
    private Vector entries;
    
	public Collection importEntries(Shell shell, CurrencyAccount account) {
		
		// Prompt the user for the file.
        FileDialog dialog = new FileDialog(shell);
        dialog.setFilterExtensions(new String [] {"*.OFX", "*.QFX"});
        dialog.setFilterNames(new String [] {"Open Financial Exchange Files (*.OFX)", "Quicken Financial Exchange Files (*.QFX)"});
        String fileName = dialog.open();
        
        if (fileName == null) {
        	return null;
        }
        
        File sessionFile = new File(fileName);
        
        try {
        	buffer = new BufferedReader(new FileReader(sessionFile));
        } catch (FileNotFoundException e1) {
        	// TODO Auto-generated catch block
        	e1.printStackTrace();
        	return null;
        }
        
        entries = new Vector();
        
        try {
        	String line = buffer.readLine();
        	while (line != null) {
        		Matcher m = headerDatePattern.matcher(line);
        		if (!m.matches()) {
        			break;
        		}
            	line = buffer.readLine();
        	}
        	
        	if (line != null && line.length() != 0) {
        		// TODO: do this properly
        		throw new RuntimeException("unexpected data");
        	}

        	// Pass the blank line
        	line = buffer.readLine();
        	
    		Matcher m = elementPattern.matcher(line);
    		if (m.matches() && m.group(1).equals("OFX")) {
    			parseOFX();
    		}
        } catch (IOException e) {
        	return null;
        }
        
        return entries;
    }
	
	void parseOFX() throws IOException {
		String line = buffer.readLine();
		Matcher childMatch = elementPattern.matcher(line);
		while (childMatch.matches()) {
			if (childMatch.group(1).equals("BANKMSGSRSV1")) {
				parseBANKMSGSRSV1();
			} else {
				if (childMatch.group(2).length() == 0) {
					parseAndIgnoreElement(childMatch.group(1));
				}
			}
			
			line = buffer.readLine();
			childMatch = elementPattern.matcher(line);
		}
		
		// Check the correctly matching element end
		Matcher endMatch = elementEndPattern.matcher(line);
		if (!endMatch.matches() || !endMatch.group(1).equals("OFX")) {
			throw new RuntimeException("bad data");
		}
	}

	private void parseBANKMSGSRSV1() throws IOException {
		String line = buffer.readLine();
		Matcher childMatch = elementPattern.matcher(line);
		while (childMatch.matches()) {
			if (childMatch.group(1).equals("STMTTRNRS")) {
				parseSTMTTRNRS();
			} else {
				if (childMatch.group(2).length() == 0) {
					parseAndIgnoreElement(childMatch.group(1));
				}
			}
			
			line = buffer.readLine();
			childMatch = elementPattern.matcher(line);
		}
		
		// Check the correctly matching element end
		Matcher endMatch = elementEndPattern.matcher(line);
		if (!endMatch.matches() || !endMatch.group(1).equals("BANKMSGSRSV1")) {
			throw new RuntimeException("bad data");
		}
	}
	
	private void parseSTMTTRNRS() throws IOException {
		String line = buffer.readLine();
		Matcher childMatch = elementPattern.matcher(line);
		while (childMatch.matches()) {
			if (childMatch.group(1).equals("STMTRS")) {
				parseSTMTRS();
			} else {
				if (childMatch.group(2).length() == 0) {
					parseAndIgnoreElement(childMatch.group(1));
				}
			}
			
			line = buffer.readLine();
			childMatch = elementPattern.matcher(line);
		}
		
		// Check the correctly matching element end
		Matcher endMatch = elementEndPattern.matcher(line);
		if (!endMatch.matches() || !endMatch.group(1).equals("STMTTRNRS")) {
			throw new RuntimeException("bad data");
		}
	}
	
	private void parseSTMTRS() throws IOException {
		String line = buffer.readLine();
		Matcher childMatch = elementPattern.matcher(line);
		while (childMatch.matches()) {
			if (childMatch.group(1).equals("BANKTRANLIST")) {
				parseBANKTRANLIST();
			} else {
				if (childMatch.group(2).length() == 0) {
					parseAndIgnoreElement(childMatch.group(1));
				}
			}
			
			line = buffer.readLine();
			childMatch = elementPattern.matcher(line);
		}
		
		// Check the correctly matching element end
		Matcher endMatch = elementEndPattern.matcher(line);
		if (!endMatch.matches() || !endMatch.group(1).equals("STMTRS")) {
			throw new RuntimeException("bad data");
		}
	}
	
	private void parseBANKTRANLIST() throws IOException {
		String line = buffer.readLine();
		Matcher childMatch = elementPattern.matcher(line);
		while (childMatch.matches()) {
			if (childMatch.group(1).equals("STMTTRN")) {
				parseSTMTTRN();
			} else {
				if (childMatch.group(2).length() == 0) {
					parseAndIgnoreElement(childMatch.group(1));
				}
			}
			
			line = buffer.readLine();
			childMatch = elementPattern.matcher(line);
		}
		
		// Check the correctly matching element end
		Matcher endMatch = elementEndPattern.matcher(line);
		if (!endMatch.matches() || !endMatch.group(1).equals("BANKTRANLIST")) {
			throw new RuntimeException("bad data");
		}
	}			

	private void parseSTMTTRN() throws IOException {
		EntryData entryData = new EntryData();
		
		String name = null;
		String memo = null;
		
		String line = buffer.readLine();
		Matcher childMatch = elementPattern.matcher(line);
		while (childMatch.matches()) {
			System.out.println("X" + childMatch.group(0) + "X");
			System.out.println("X" + childMatch.group(1) + "X");
			System.out.println("X" + childMatch.group(2) + "X");
			String data = childMatch.group(2); 
		
			if (childMatch.group(1).equals("DTPOSTED")) {
				// For some extraordinary reason, the date pattern does not match.
/*				
				System.out.println("data=" + childMatch.group(2) + "Y");
				Matcher dateMatch = datePattern.matcher(data);
				System.out.println("data=" + childMatch.group(2) + "Z");
				if (!dateMatch.matches()) {
					throw new RuntimeException("bad date");
				}
				
				int year  = Integer.parseInt(childMatch.group(1));
				int month = Integer.parseInt(childMatch.group(2));
				int day   = Integer.parseInt(childMatch.group(3));
*/
				// So let's just extract another way
				int year = Integer.parseInt(data.substring(0, 4));
				int month = Integer.parseInt(data.substring(4, 6));
				int day = Integer.parseInt(data.substring(6, 8));
				
				entryData.setClearedDate(new Date(year-1900, month-1, day));
			} else if (childMatch.group(1).equals("TRNAMT")) {
				long amount = (long)(Double.parseDouble(data) * 100);
				entryData.setAmount(amount);
			} else if (childMatch.group(1).equals("FITID")) {
				entryData.setUniqueId(data);
			} else if (childMatch.group(1).equals("NAME")) {
				name = data;
			} else if (childMatch.group(1).equals("MEMO")) {
				memo = data;
			} else if (childMatch.group(1).equals("CHECKNUM")) {
				entryData.setCheck(data);
			}

			line = buffer.readLine();
			childMatch = elementPattern.matcher(line);
		}
		
		// It seems that QFX format has a <NAME> and a <MEMO> line,
		// whereas OFX has only a name.
		// It is a mess because sometimes the payee name is in the
		// <NAME> field and sometimes it is in the <MEMO> field.
		// (At least with the data from Bank of America)
		// Just combine the two.
		if (name == null && memo != null) {
			entryData.setMemo(memo);
		} else if (name != null && memo == null) {
				entryData.setMemo(name);
		} else if (name != null && memo != null) {
					entryData.setMemo(name + " " + memo);
		}
		
		entries.add(entryData);
		
		// Check the correctly matching element end
		Matcher endMatch = elementEndPattern.matcher(line);
		if (!endMatch.matches() || !endMatch.group(1).equals("STMTTRN")) {
			throw new RuntimeException("bad data");
		}
	}			

	/**
	 * Parse an element, ignoring it and all child elements
	 * and leaf nodes.
	 * 
	 * @param elementName
	 */
	private void parseAndIgnoreElement(String elementName) throws IOException {
		String line = buffer.readLine();
		Matcher childMatch = elementPattern.matcher(line);
		while (childMatch.matches()) {
				if (childMatch.group(2).length() == 0) {
					parseAndIgnoreElement(childMatch.group(1));
				}
			
			line = buffer.readLine();
			childMatch = elementPattern.matcher(line);
		}
		
		// Check the correctly matching element end
		Matcher endMatch = elementEndPattern.matcher(line);
		if (!endMatch.matches() || !endMatch.group(1).equals(elementName)) {
			throw new RuntimeException("bad data");
		}
	}
}