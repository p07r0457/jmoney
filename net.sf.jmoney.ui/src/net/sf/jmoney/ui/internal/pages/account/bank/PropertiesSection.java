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
package net.sf.jmoney.ui.internal.pages.account.bank;

import net.sf.jmoney.model2.BankAccount;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class PropertiesSection extends SectionPart {

	protected PropertiesPage fPage;
    protected Text fName; 
    protected Text fCurrency; 
    protected Text fBank; 
    protected Text fAccountNumber; 
    protected Text fStartBalance; 
    protected Text fMinBalance; 
    protected Text fComment; 

	public PropertiesSection(PropertiesPage page, Composite parent) {
        super(parent, page.getManagedForm().getToolkit(), Section.DESCRIPTION | Section.TITLE_BAR);
        fPage = page;
        getSection().setText("Account Properties");
        getSection().setDescription("Specify general properties of this capital account.");
        createClient(page.getManagedForm().getToolkit());
    }

	protected void createClient(FormToolkit toolkit) {
        Composite container = toolkit.createComposite(getSection());

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        container.setLayout(layout);

        toolkit.createLabel(container, "Name:");
        fName = toolkit.createText(container, "");
        fName.setTextLimit(50);
        toolkit.createLabel(container, "Currency:");
        fCurrency = toolkit.createText(container, "");
        toolkit.createLabel(container, "Bank:");
        fBank = toolkit.createText(container, "");
        toolkit.createLabel(container, "Account Number:");
        fAccountNumber = toolkit.createText(container, "");
        toolkit.createLabel(container, "Start Balance:");
        fStartBalance = toolkit.createText(container, "");
        toolkit.createLabel(container, "Minimal Balance:");
        fMinBalance = toolkit.createText(container, "");
        toolkit.createLabel(container, "Comment:");
        fComment = toolkit.createText(container, "");

        getSection().setClient(container);
        toolkit.paintBordersFor(container);
        refresh();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.forms.IFormPart#refresh()
     */
    public void refresh() {    	// When this page was added to the 'pages' extension point,    	// the extendable-object-id was set to net.sf.jmoney.bankAccount.    	// This indicates that this page should only appear when    	// the object selected in the navigation tree is an    	// object of that type.  We can therefore safely cast    	// the selected object to BankAccount.        BankAccount account = (BankAccount)fPage.fEditor.getSelectedObject();

        setText(fName, account.getName());
        setText(fCurrency, account.getCurrencyCode());
        setText(fStartBalance, "" + account.getStartBalance());        setText(fComment, account.getComment());        BankAccount bankAccount = (BankAccount)account;	setText(fBank, bankAccount.getBank());
        setText(fAccountNumber, bankAccount.getAccountNumber());
        setText(fMinBalance, bankAccount.getMinBalance());
        super.refresh();
    }

    protected void setText(Text text, Object value) {
        text.setText(value == null? "": value.toString());
    }

}