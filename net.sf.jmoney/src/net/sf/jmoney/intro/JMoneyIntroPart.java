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

package net.sf.jmoney.intro;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.part.IntroPart;

/**
 * Provides a 'welcome' screen.  When users run JMoney for the
 * first time, this screen fills the JMoney window.
 * 
 * @author Nigel Westbury
 */
public class JMoneyIntroPart extends IntroPart {

	private Label introText;
	private Label l;
	private Button b;
	
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout());

		introText = new Label(parent, SWT.NONE);
		introText.setText(
				"Welcome to JMoney, the open source finance manager." +
				"  It will run on any computer that can run Java." +
				"  It can be extendable using the Eclipse plug-in architecture." +
				"  It is so easy and intuitive to use that no further introduction is needed." +
				"  So just close this window and get your accounts into order.");
		
		l = new Label(parent, SWT.NONE);
		l.setText("Info control (standby == unknown)");
		
		b = new Button(parent, SWT.TOGGLE);
		b.setText("Standby");
		b.addSelectionListener(new SelectionAdapter (){

			public void widgetSelected(SelectionEvent e) {
				getIntroSite().getWorkbenchWindow().getWorkbench().getIntroManager().setIntroStandby(JMoneyIntroPart.this, b.getSelection());
			}
		});	
		
		boolean standby = getIntroSite().getWorkbenchWindow().getWorkbench().getIntroManager().isIntroStandby(this);
		standbyStateChanged(standby);
		
		IAction testAction = new Action("Test", IAction.AS_CHECK_BOX) {
			public void run() {
				
			}
		};
		
		getIntroSite().getActionBars().getToolBarManager().add(testAction);
		getIntroSite().getActionBars().updateActionBars();
	}

	public void setFocus() {
	}

	public void init(IIntroSite site) throws PartInitException {
		setSite(site);		
	}

	public void standbyStateChanged(boolean standby) {
		l.setText("Info control (standby == " + standby + ")");
		b.setSelection(standby);
	}

}
