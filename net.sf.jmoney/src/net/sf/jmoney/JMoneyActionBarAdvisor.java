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

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

/**
 * @author Johann Gyger
 */
public class JMoneyActionBarAdvisor extends ActionBarAdvisor {

    private final IWorkbenchWindow window;
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
    protected void makeActions(final IWorkbenchWindow window) {
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
    protected void fillMenuBar(IMenuManager menuBar) {
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());
        menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menuBar.add(createWindowMenu());
        menuBar.add(createHelpMenu());
    }

    /**
     * Creates and returns the File menu.
     */
    private MenuManager createFileMenu() {
        MenuManager menu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);

        // File
        menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

        // TODO 2005-07-09/jgyger: Change to an import (export) wizard.
        menu.add(new Separator());
        menu.add(new MenuManager("Import", "import"));
        menu.add(new MenuManager("Export", "export"));

        menu.add(new Separator());
        menu.add(quitAction);
        menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));

        return menu;
    }   

    /**
     * Creates and returns the File menu.
     */
    private MenuManager createEditMenu() {
        MenuManager menu = new MenuManager("&Edit", IWorkbenchActionConstants.M_EDIT);

        menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
        menu.add(undoAction);
        menu.add(redoAction);
        menu.add(new Separator());
        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));

        return menu;
    }   

    /**
     * Creates and returns the Help menu.
     */
    private MenuManager createWindowMenu() {
        MenuManager menu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
        {
            MenuManager showViewMenuMgr = new MenuManager("Show View", "showView");
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
        MenuManager menu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);

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
