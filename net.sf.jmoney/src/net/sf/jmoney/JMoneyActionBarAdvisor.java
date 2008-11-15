/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2005 Johann Gyger <jgyger@users.sourceforge.net>
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

package net.sf.jmoney;

import net.sf.jmoney.resources.Messages;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

/**
 * @author Johann Gyger
 */
public class JMoneyActionBarAdvisor extends ActionBarAdvisor {

    private final IWorkbenchWindow window;
    private IAction newAction;
    private IAction importAction;
    private IAction exportAction;
    private IAction quitAction;
    private IAction undoAction;
    private IAction redoAction;
    private IAction preferencesAction;
    private IAction introAction;
    private IAction aboutAction;

    public JMoneyActionBarAdvisor(IActionBarConfigurer configurer) {
        super(configurer);
        window = configurer.getWindowConfigurer().getWindow();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.application.ActionBarAdvisor#makeActions(org.eclipse.ui.IWorkbenchWindow)
     */
    @Override	
    protected void makeActions(final IWorkbenchWindow window) {
        newAction = ActionFactory.NEW_WIZARD_DROP_DOWN.create(window);
        register(newAction);

        importAction = ActionFactory.IMPORT.create(window);
        register(importAction);

        exportAction = ActionFactory.EXPORT.create(window);
        register(exportAction);

        quitAction = ActionFactory.QUIT.create(window);
        register(quitAction);

        preferencesAction = ActionFactory.PREFERENCES.create(window);
        register(preferencesAction);

        undoAction = ActionFactory.UNDO.create(window);
        register(undoAction);
        
        redoAction = ActionFactory.REDO.create(window);
        register(redoAction);
        
        if (window.getWorkbench().getIntroManager().hasIntro()) {
            introAction = ActionFactory.INTRO.create(window);
            register(introAction);
        }

        aboutAction = ActionFactory.ABOUT.create(window);
        register(aboutAction);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.application.ActionBarAdvisor#fillMenuBar(org.eclipse.jface.action.IMenuManager)
     */
    @Override	
    protected void fillMenuBar(IMenuManager menuBar) {
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());
        menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menuBar.add(createNavigateMenu());
        menuBar.add(createWindowMenu());
        menuBar.add(createHelpMenu());
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.application.ActionBarAdvisor#fillMenuBar(org.eclipse.jface.action.IMenuManager)
     */
    @Override	
    protected void fillCoolBar(ICoolBarManager coolBar) {
    	super.fillCoolBar(coolBar);
    	
    	IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
    	coolBar.add(new ToolBarContributionItem(toolbar, "main")); //$NON-NLS-1$
    	toolbar.add(newAction);
    	toolbar.add(new Separator("edit")); //$NON-NLS-1$
    	toolbar.add(new Separator("navigate")); //$NON-NLS-1$

    	// This is an example of how menus can be added programatically.
    	// This is probably better than in plugin.xml if we can figure out how to make the
    	// menus be visible only when an appropriate editor is active.
    	CommandContributionItemParameter params = new CommandContributionItemParameter(PlatformUI
				.getWorkbench(), null, "net.sf.jmoney.transactionDetails", //$NON-NLS-1$
				CommandContributionItem.STYLE_PUSH);
		params.tooltip = "%Commands.tooltip.viewTransactionDetails";  // TODO:
//  	params.icon = ???.getImageDescriptor();
		toolbar.add(new CommandContributionItem(params));
    	
    	toolbar.add(new Separator("openEditors")); //$NON-NLS-1$
    	toolbar.add(new Separator());
    	toolbar.add(importAction);
        toolbar.add(exportAction);
    }

    /**
     * Creates and returns the File menu.
     */
    private MenuManager createFileMenu() {
        MenuManager menu = new MenuManager(Messages.JMoneyActionBarAdvisor_File, IWorkbenchActionConstants.M_FILE);

        menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));

        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

        menu.add(new Separator());
        menu.add(importAction);
        menu.add(exportAction);

        menu.add(new Separator());
        menu.add(quitAction);
        menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));

        return menu;
    }   

    /**
     * Creates and returns the File menu.
     */
    private MenuManager createEditMenu() {
        MenuManager menu = new MenuManager(Messages.JMoneyActionBarAdvisor_Edit, IWorkbenchActionConstants.M_EDIT);

        menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
        menu.add(undoAction);
        menu.add(redoAction);
        menu.add(new Separator());
        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));

        return menu;
    }   

    /**
     * Creates and returns the Navigate menu.
     */
    private MenuManager createNavigateMenu() {
        MenuManager menu = new MenuManager("&Navigate", IWorkbenchActionConstants.M_NAVIGATE);
        menu.add(new Separator("openEditors"));
        menu.add(new Separator());
        menu.add(createReportsMenu());
        menu.add(createChartsMenu());
        menu.add(new Separator());
        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        return menu;
    }

    /**
     * Creates and returns the Reports menu.
     */
    private MenuManager createReportsMenu() {
        MenuManager menu = new MenuManager("&Reports", JMoneyPlugin.createImageDescriptor("icons/report.gif"), "reports");
        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        return menu;
    }

    /**
     * Creates and returns the Charts menu.
     */
    private MenuManager createChartsMenu() {
        MenuManager menu = new MenuManager("&Charts", JMoneyPlugin.createImageDescriptor("icons\\chart.gif"), "charts");
        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        return menu;
    }

    /**
     * Creates and returns the Window menu.
     */
    private MenuManager createWindowMenu() {
        MenuManager menu = new MenuManager(Messages.JMoneyActionBarAdvisor_Window, IWorkbenchActionConstants.M_WINDOW);
        {
            MenuManager showViewMenuMgr = new MenuManager(Messages.JMoneyActionBarAdvisor_ShowView, "showView"); //$NON-NLS-1$
            IContributionItem showViewMenu = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
            showViewMenuMgr.add(showViewMenu);
            menu.add(showViewMenuMgr);
        }
        menu.add(new Separator());
        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menu.add(new Separator());
        menu.add(preferencesAction);
        return menu;
    }

    /**
     * Creates and returns the Help menu.
     */
    private MenuManager createHelpMenu() {
        MenuManager menu = new MenuManager(Messages.JMoneyActionBarAdvisor_Help, IWorkbenchActionConstants.M_HELP);

        // Help
        if (introAction != null) {
            menu.add(introAction);
        }
        menu.add(new Separator());
        menu.add(new GroupMarker(IWorkbenchActionConstants.HELP_START));
        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menu.add(new Separator());
        menu.add(aboutAction);
        menu.add(new GroupMarker(IWorkbenchActionConstants.HELP_END));    

        return menu;
    }

}
