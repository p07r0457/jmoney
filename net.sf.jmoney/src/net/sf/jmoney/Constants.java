/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

import java.util.ResourceBundle;

import org.eclipse.swt.graphics.Image;

/**
 * An interface that holds the global constants.
 */
public interface Constants {

	/**
	 * The name of the author.
	 */
	public static final String AUTHOR = "Johann Gyger";

	/**
	 * The email address of the author.
	 */
	public static final String EMAIL = "jgyger@users.sf.net";

	/**
	 * Copyright string.
	 */
	public static final String COPYRIGHT =
		"Copyright (C) 2000 - 2004 " + AUTHOR + " <" + EMAIL + ">";

	/**
	 * Corresponding int for CANCEL.
	 */
	public static final int CANCEL = 0;

	/**
	 * Corresponding int for OK.
	 */
	public static final int OK = 1;

	/**
	 * Corresponding int for HELP.
	 */
	public static final int HELP = 2;

	/**
	 * Corresponding int for NEW.
	 */
	public static final int NEW = 3;
/*
	public static final ImageIcon JMONEY_IMAGE =
		new ImageIcon(Constants.class.getResource("images/jmoney_logo.png"));
*/
	
	public static final Image ACCOUNTS_ICON =
		JMoneyPlugin.createImage("icons/accounts.gif");

	public static final Image ACCOUNT_ICON =
		JMoneyPlugin.createImage("icons/account.gif");

	public static final Image CATEGORY_ICON =
		JMoneyPlugin.createImage("icons/category.gif");
/*
	public static final ImageIcon ARROW_UP_ICON =
		new ImageIcon(Constants.class.getResource("images/ArrowUp.gif"));

	public static final ImageIcon ARROW_DOWN_ICON =
		new ImageIcon(Constants.class.getResource("images/ArrowDown.gif"));

	public static final ImageIcon PRINT_ICON =
		new ImageIcon(Constants.class.getResource("images/jlfgr/Print16.gif"));
		
	public static final ImageIcon IMPORT_ICON =
		new ImageIcon(Constants.class.getResource("images/jlfgr/Import16.gif"));
		
	public static final ImageIcon EXPORT_ICON =
		new ImageIcon(Constants.class.getResource("images/jlfgr/Export16.gif"));

	public static final ImageIcon UNDO_ICON =
		new ImageIcon(Constants.class.getResource("images/jlfgr/Undo16.gif"));

	public static final ImageIcon REDO_ICON =
		new ImageIcon(Constants.class.getResource("images/jlfgr/Redo16.gif"));

	public static final ImageIcon CUT_ICON =
		new ImageIcon(Constants.class.getResource("images/jlfgr/Cut16.gif"));

	public static final ImageIcon COPY_ICON =
		new ImageIcon(Constants.class.getResource("images/jlfgr/Copy16.gif"));

	public static final ImageIcon PASTE_ICON =
		new ImageIcon(Constants.class.getResource("images/jlfgr/Paste16.gif"));

	public static final ImageIcon FIND_ICON =
		new ImageIcon(Constants.class.getResource("images/jlfgr/Find16.gif"));

	public static final ImageIcon FIND_AGAIN_ICON =
		new ImageIcon(Constants.class.getResource("images/jlfgr/FindAgain16.gif"));
		
	public static final ImageIcon PREFERENCES_ICON =
		new ImageIcon(Constants.class.getResource("images/jlfgr/Preferences16.gif"));

	public static final ImageIcon ABOUT_ICON =
		new ImageIcon(Constants.class.getResource("images/jlfgr/About16.gif"));
*/
	/**
	 * The language resource bundle.
	 */
/*	
	public static ResourceBundle LANGUAGE =
		ResourceBundle.getBundle("net.sf.jmoney.resources.Language");
*/
	/**
	 * The general resource bundle.
	 */
	public static ResourceBundle GENERAL =
		ResourceBundle.getBundle("net.sf.jmoney.resources.General");

}
