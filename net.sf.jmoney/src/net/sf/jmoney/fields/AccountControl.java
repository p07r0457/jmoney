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

package net.sf.jmoney.fields;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Session;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;


/**
 * A control for entering accounts.
 * 
 * This control contains both a text box and a list box that appears when the
 * text box gains focus.
 * 
 * @author Nigel Westbury
 */
public class AccountControl extends AccountComposite {

	Text textControl;
	
    /**
     * List of accounts put into account list.
     */
    private Vector<Account> allAccounts;
    
	/**
	 * Currently selected account, or null if no account selected
	 */
	private Account account;
	
	private Vector<SelectionListener> listeners = new Vector<SelectionListener>();
	
	/**
	 * @param parent
	 * @param style
	 */
	public AccountControl(final Composite parent, final Session session, final Class<? extends Account> accountClass) {
		super(parent, SWT.NONE);

		setLayout(new FillLayout(SWT.VERTICAL));
		
		textControl = new Text(this, SWT.LEFT);
		
		textControl.addFocusListener(new FocusListener() {

			Shell shell;
			boolean closingShell = false;
			
			public void focusGained(FocusEvent e) {
				if (closingShell) {
					return;
				}
				
				shell = new Shell(parent.getShell(), SWT.ON_TOP);
		        shell.setLayout(new RowLayout());

		        final List listControl = new List(shell, SWT.SINGLE | SWT.V_SCROLL);
		        RowData rd = new RowData();
		        rd.height = 100;
		        listControl.setLayoutData(rd);

		        allAccounts = new Vector<Account>();
		        addAccounts("", session.getAccountCollection(), listControl, accountClass);
		        
//		        shell.setSize(listControl.computeSize(SWT.DEFAULT, listControl.getItemHeight()*10));
		        
                // Set the currently set account into the list control.
    	        listControl.select(allAccounts.indexOf(account));
                
    	        listControl.addSelectionListener(
                		new SelectionAdapter() {
							public void widgetSelected(SelectionEvent e) {
								int selectionIndex = listControl.getSelectionIndex();
								account = (Account)allAccounts.get(selectionIndex);
								textControl.setText(account.getName());
								fireAccountChangeEvent();
							}
                		});

    			listControl.addKeyListener(new KeyAdapter() {
    				String pattern;
    				int lastTime = 0;
    				
    				public void keyPressed(KeyEvent e) {
    					if (Character.isLetterOrDigit(e.character)) {
    						if ((e.time - lastTime) < 1000) {
    							pattern += Character.toUpperCase(e.character);
    						} else {
    							pattern = String.valueOf(Character.toUpperCase(e.character));
    						}
    						lastTime = e.time;
    						
    						/*
    						 * 
    						 Starting at the currently selected account,
    						 search for an account starting with these characters.
    						 */
    						int startIndex = listControl.getSelectionIndex();
    						if (startIndex == -1) {
    							startIndex = 0;
    						}
    						
    						int match = -1;
    						int i = startIndex;
    						do {
    							if (((Account)allAccounts.get(i)).getName().toUpperCase().startsWith(pattern)) {
    								match = i;
    								break;
    							}
    							
    							i++;
    							if (i == allAccounts.size()) {
    								i = 0;
    							}
    						} while (i != startIndex);
    						
    						if (match != -1) {
    							account = (Account)allAccounts.get(match);
    							listControl.select(match);
    							listControl.setTopIndex(match);
    							textControl.setText(account.getName());
    						}
    						
    						e.doit = false;
    					}
    				}
    			});

    			shell.pack();
    	        
    	        // Position the calendar shell below the date control,
    	        // unless the date control is so near the bottom of the display that
    	        // the calendar control would go off the bottom of the display,
    	        // in which case position the calendar shell above the date control.
    	        Display display = getDisplay();
    	        Rectangle rect = display.map(parent, null, getBounds());
    	        int calendarShellHeight = shell.getSize().y;
    	        if (rect.y + rect.height + calendarShellHeight <= display.getBounds().height) {
        	        shell.setLocation(rect.x, rect.y + rect.height);
    	        } else {
        	        shell.setLocation(rect.x, rect.y - calendarShellHeight);
    	        }

    	        shell.open();
    	        
    	        shell.addShellListener(new ShellAdapter() {
    	        	public void shellDeactivated(ShellEvent e) {
    	        		closingShell = true;
    	        		shell.close();
    	        		closingShell = false;
    	        	}
    	        });
			}

			public void focusLost(FocusEvent e) {
//        		shell.close();
 //       		listControl = null;
			}
		});
	}

	private void fireAccountChangeEvent() {
		for (SelectionListener listener: listeners) {
			listener.widgetSelected(null);
		}
	}
	
	private void addAccounts(String prefix, Collection<? extends Account> accounts, List listControl, Class<? extends Account> accountClass) {
    	Vector<Account> matchingAccounts = new Vector<Account>();
        for (Account account: accounts) {
        	if (accountClass.isAssignableFrom(account.getClass())) {
        		matchingAccounts.add(account);
        	}
        }
		
		// Sort the accounts by name.
		Collections.sort(matchingAccounts, new Comparator<Account>() {
			public int compare(Account account1, Account account2) {
				return account1.getName().compareTo(account2.getName());
			}
		});
		
		for (Account matchingAccount: matchingAccounts) {
    		allAccounts.add(matchingAccount);
			listControl.add(prefix + matchingAccount.getName());
    		addAccounts(prefix + matchingAccount.getName() + ":", matchingAccount.getSubAccountCollection(), listControl, accountClass);
		}
        
    }

    /**
	 * @param object
	 */
	public void setAccount(Account account) {
		this.account = account;
		
		if (account == null) {
			textControl.setText("");
	} else {
        textControl.setText(account.getName());
	}
}

	/**
	 * @return the date, or null if a valid date is not set in
	 * 				the control
	 */
	public Account getAccount() {
		return account;
	}

	/**
	 * @param listener
	 */
	public void addSelectionListener(SelectionListener listener) {
		listeners.add(listener);
	}

	/**
	 * @param listener
	 */
	public void removeSelectionListener(SelectionListener listener) {
		listeners.remove(listener);
	}

	public Control getControl() {
		return this;
	}

	public void rememberChoice() {
		// We don't remember choices, so nothing to do
	}	

	public void init(IMemento memento) {
		// No state to restore
	}

	public void saveState(IMemento memento) {
		// No state to save
	}
}