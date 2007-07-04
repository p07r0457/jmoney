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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
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

    private PageWithChildPages introPage;
	private IWizardPage closedSourcePage;
	private IWizardPage quickenPage;
	private IWizardPage msmoneyPage;
	private IWizardPage gnuCashPage;
	private IWizardPage otherFileFormatPage;
	private IWizardPage databasePage;
	private IWizardPage noDataPage;
	private IWizardPage summaryPage;
	
	private WizardContainer wc = null;
	
	/**
	 * This class represents a wizard page with further pages
	 * that may or may not be shown depending on data entered
	 * in this page.
	 * <P>
	 * Derived classes must override getNextPage() and set
	 * childPages to the set of pages that are to be shown.
	 * Furthermore, all such child pages must override getNextPage
	 * and pass the request on to the getNextPage(IWizardPage childPage)
	 * method from this object.
	 * 
	 * @author Nigel Westbury
	 */
	abstract class PageWithChildPages extends WizardPage {
		/**
		 * List of pages that are to be displayed following this page.
		 * This list depends on this data entered by the user into this
		 * page and so childPages is valid only once the user has
		 * pressed the Next button to go past this page.
		 */
		protected Vector<IWizardPage> childPages;
		
		PageWithChildPages(String pageName) {
			super(pageName);
		}

		public IWizardPage getNextPage(IWizardPage childPage) {
			int index = childPages.indexOf(childPage);
			if (index == childPages.size() - 1 || index == -1)
				// last page or page not found
				return null;
			return childPages.get(index + 1);
		}
	}
	
	class ExistingDataInquiryPage extends PageWithChildPages {

		private Button quickenButton;
		private Button msmoneyButton;
		private Button gnuCashButton;
		private Button otherFileFormatButton;
		private Button databaseButton;
		
		ExistingDataInquiryPage(ISelection s) {
			super("page name");
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
		 */
		public void createControl(Composite parent) {
			Composite container = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();
			container.setLayout(layout);
			layout.numColumns = 1;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			layout.verticalSpacing = 9;  
			
			String htmlString = "";
			InputStream in = JMoneyIntroPart.class.getResourceAsStream("intro.html"); //$NON-NLS-1$
			BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
			try {
				String line = buffer.readLine();
				while (line != null) {
					htmlString = htmlString + line + '\n';
					line = buffer.readLine();
				}

				buffer.close();
				in.close();
			} catch (IOException e) {
				// TODO: log error
			}
			
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			
			try {
				Browser browser = new Browser(container, SWT.NONE);
				browser.setLayoutData(gd);
				browser.setText(htmlString);
			} catch (SWTError e) {
				// The operating system does not support a browser control.
				// Put the data into a wrapped label.
				// TODO: Do some basic formatting of the HTML.
				Label introText = new Label(container, SWT.WRAP);
				introText.setLayoutData(gd);
				introText.setText(htmlString);
			}
			
			quickenButton = new Button(container, SWT.CHECK);
			quickenButton.setText("Quicken");
			
			msmoneyButton = new Button(container, SWT.CHECK);
			msmoneyButton.setText("Microsoft Money");
			
			gnuCashButton = new Button(container, SWT.CHECK);
			gnuCashButton.setText("GnuCash");
			
			otherFileFormatButton = new Button(container, SWT.CHECK);
			otherFileFormatButton.setText(JMoneyPlugin.getResourceString("Intro.checkBoxOtherFormat")); //$NON-NLS-1$
			
			databaseButton = new Button(container, SWT.CHECK);
			databaseButton.setText(JMoneyPlugin.getResourceString("Intro.checkBoxDatabase")); //$NON-NLS-1$
			
			setControl(container);
		}
		
	    @Override	
		public IWizardPage getNextPage() {
			childPages = new Vector<IWizardPage>();
			
			if (quickenButton.getSelection() && msmoneyButton.getSelection()) {
				childPages.add(closedSourcePage);
			} else if (quickenButton.getSelection()) {
				childPages.add(quickenPage);
			} else if (msmoneyButton.getSelection()) {
				childPages.add(msmoneyPage);
			}
			
			if (gnuCashButton.getSelection()) {
				childPages.add(gnuCashPage);
			}

			if (otherFileFormatButton.getSelection()) {
				childPages.add(otherFileFormatPage);
			}

			if (databaseButton.getSelection()) {
				childPages.add(databasePage);
			}

			if (childPages.isEmpty()) {
				childPages.add(noDataPage);
			}
			
			return childPages.get(0);
		}
	}
	
	class TextOnlyPage extends WizardPage {
		private PageWithChildPages parentPage;
		private String resourceName;
		private Object [] messageArgs;
		
		TextOnlyPage(PageWithChildPages parentPage, String resourceName) {
			super("page 2 name");
			this.parentPage = parentPage;
			this.resourceName = resourceName;
			this.messageArgs = null;
		}
		
		TextOnlyPage(PageWithChildPages parentPage, String resourceName, Object [] messageArgs) {
			super("page 2 name");
			this.parentPage = parentPage;
			this.resourceName = resourceName;
			this.messageArgs = messageArgs;
		}
		
		public void createControl(Composite parent) {
			Composite container = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();
			container.setLayout(layout);
			layout.numColumns = 1;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			
			String htmlString = "";
			InputStream in = JMoneyIntroPart.class.getResourceAsStream(resourceName);
			BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
			try {
				String line = buffer.readLine();
				while (line != null) {
					htmlString = htmlString + line + '\n';
					line = buffer.readLine();
				}

				buffer.close();
				in.close();
			} catch (IOException e) {
				// TODO: log error
			}
			
			String html2 = new java.text.MessageFormat(
							htmlString, 
							java.util.Locale.US)
							.format(messageArgs);
			
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			
			try {
				Browser browser = new Browser(container, SWT.NONE);
				browser.setLayoutData(gd);
				browser.setText(html2);
			} catch (SWTError e) {
				// The operating system does not support a browser control.
				// Put the data into a wrapped label.
				// TODO: Do some basic formatting of the HTML.
				Label introText = new Label(container, SWT.WRAP);
				introText.setLayoutData(gd);
				introText.setText(html2);
			}
			
			setControl(container);			
		}
		
	    @Override	
		public IWizardPage getNextPage() {
			// TextOnly pages have no child pages.
			// Therefore pass the request on to the parent page which
			// will determine the next page.
			return parentPage.getNextPage(this);
		}
	}
	
	public class JMoneyIntroWizard extends Wizard
	{
	    private ISelection selection;

	    public JMoneyIntroWizard()
	    {
	        super();
	        setNeedsProgressMonitor(true);
	    }

	    @Override	
	    public void addPages()
	    {
			introPage = new ExistingDataInquiryPage(selection);
			closedSourcePage = new TextOnlyPage(introPage, "closedformat.html", //$NON-NLS-1$
					new Object[] {
						"Quicken and MS-Money",
						"are", "s", ""
			});
			
			quickenPage = new TextOnlyPage(introPage, "closedformat.html", //$NON-NLS-1$
			new Object[] {
					"Quicken",
					"is", "", "s"
			});

			msmoneyPage = new TextOnlyPage(introPage, "closedformat.html", //$NON-NLS-1$
			new Object[] {
					"MS-Money",
					"is", "", "s"
			});

			gnuCashPage = new TextOnlyPage(introPage, "gnucashformat.html"); //$NON-NLS-1$
			otherFileFormatPage = new TextOnlyPage(introPage, "otherformat.html");  //$NON-NLS-1$
			databasePage = new TextOnlyPage(introPage, "database.html");  //$NON-NLS-1$
			noDataPage = new TextOnlyPage(introPage, "nodata.html");  //$NON-NLS-1$
			summaryPage = new TextOnlyPage(introPage, "summary.html");  //$NON-NLS-1$

			addPage(introPage);
			addPage(closedSourcePage);
			addPage(quickenPage);
			addPage(msmoneyPage);
			addPage(gnuCashPage);
			addPage(otherFileFormatPage);
			addPage(databasePage);
			addPage(noDataPage);
	        addPage(summaryPage);
	    }

	    @Override	
	    public boolean performFinish() {
	    	// There is no special processing that we need to
	    	// do when the user presses the Finish button.
	        return true;
	    }
	}	
	
    @Override	
	public void createPartControl(Composite parent) {
		JMoneyIntroWizard wizard = new JMoneyIntroWizard();
		wc = new WizardContainer(parent, wizard, JMoneyIntroPart.this);		

		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		parent.setLayout(layout);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		wc.setLayoutData(gd);
		
		wc.createContents(parent);
		
//		getIntroSite().getActionBars().getToolBarManager().add(testAction);
//		getIntroSite().getActionBars().updateActionBars();
	}

    @Override	
	public void setFocus() {
	}

	public void init(IIntroSite site) throws PartInitException {
		setSite(site);		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.intro.IIntroPart#standbyStateChanged(boolean)
	 */
	public void standbyStateChanged(boolean standby) {
		// Pass the notification on to the wizard container so
		// it can change the text in the button.
		wc.standbyStateChanged(standby);
	}
}
