/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2004 Johann Gyger <jgyger@users.sf.net>
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
package net.sf.jmoney.views;

import net.sf.jmoney.IBookkeepingPageListener;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.IBookkeepingPageListener.BookkeepingPage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class OldStyleWrapperFormPart extends AbstractFormPart {

	protected SectionlessPage fPage;
    protected IBookkeepingPageListener pageListener;
    protected Composite parent; 

    public OldStyleWrapperFormPart(SectionlessPage page, Composite parent, IBookkeepingPageListener pageListener) {
        fPage = page;
        this.parent = parent;
        this.pageListener = pageListener;

        createClient(page.getManagedForm().getToolkit());
    }

	protected void createClient(FormToolkit toolkit) {
        BookkeepingPage [] pages = 
        	pageListener.createPages(
        			fPage.getSelectedObject(), JMoneyPlugin.getDefault().getSession(),
					parent);
        if (pages.length != 1) {
        	throw new RuntimeException("should not happen");
        }

        // TODO: Do this.........
//      toolkit.paintBordersFor(pages[0].getControl());

        
        
        
        
        // Modified by Faucheux
        // The one and only visible control in the parent
        // should fill the entire space.
        final Control control = pages[0].getControl();
        control.setBackground(control.getDisplay().getSystemColor(
                SWT.COLOR_DARK_BLUE));
        control.pack(true);

        // Force the parent (DARK_BLUE) to be as big as its container
        control.addControlListener(new ControlListener() {
            public void controlMoved(ControlEvent e) {  }
            public void controlResized(ControlEvent e) {
                Composite parent = (Composite) e.getSource();
                System.out.println("Redraw dark blue " + parent);
                System.out.println("  was " + parent.getSize());
//              parent.setSize(parent.getParent().getSize());
                parent.setSize(parent.getParent().getSize().x * 95 / 100, parent.getParent().getSize().y * 90 / 100);
                System.out.println("  is  " + parent.getSize());
            }
            
        });

        parent.setBackground(control.getDisplay().getSystemColor(
                SWT.COLOR_CYAN));
        parent.pack(true);
        parent.addControlListener(new ControlListener() {
            public void controlMoved(ControlEvent e) { /* nothing */ }
            public void controlResized(ControlEvent e) {
                Composite parent = (Composite) e.getSource();
                System.out.println("Redraw cyan " + parent);
                System.out.println("  was " + parent.getSize());
//                parent.setSize(parent.getParent().getSize());
                
//                control.setSize(parent.getSize().x * 95 / 100, parent.getSize().x * 90 / 100);
                control.setSize(parent.getSize());
                System.out.println("  is  " + parent.getSize());
            }
        });
        
        refresh();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.forms.IFormPart#refresh()
     */
    public void refresh() {
/* Not sure what we do here???????
    	
    	
        CurrencyAccount account = fPage.fEditor.fAccount;



        setText(fName, account.getName());

        setText(fCurrency, account.getCurrencyCode());

        setText(fStartBalance, "" + account.getStartBalance());

        setText(fComment, account.getComment());
if (account instanceof BankAccount) {
	BankAccount bankAccount = (BankAccount)account;

	setText(fBank, bankAccount.getBank());

        setText(fAccountNumber, bankAccount.getAccountNumber());

        setText(fMinBalance, bankAccount.getMinBalance());
}
*/

        super.refresh();
    }

/*
    protected void setText(Text text, Object value) {

        text.setText(value == null? "": value.toString());

    }
*/
}