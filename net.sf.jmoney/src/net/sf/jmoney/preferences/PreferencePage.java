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

package net.sf.jmoney.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */


public class PreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {
	public static final String P_PATH = "pathPreference";

	public PreferencePage() {
		super(GRID);
		setPreferenceStore(JMoneyPlugin.getDefault().getPreferenceStore());
		setDescription("JMoney preferences");
		initializeDefaults();
		
		// The title of this page is picked up and used by the
		// preference dialog as the text in the preferences
		// navigation tree.
		setTitle("core JMoney preferences");
	}
/**
 * Sets the default values of the preferences.
 */
	private void initializeDefaults() {
		IPreferenceStore store = getPreferenceStore();
		store.setDefault("booleanPreference", true);
		store.setDefault("dateFormat", "yyyy-MM-dd");
		store.setDefault("stringPreference", "Default value");
	}
	
/**
 * Creates the field editors. Field editors are abstractions of
 * the common GUI blocks needed to manipulate various types
 * of preferences. Each field editor knows how to save and
 * restore itself.
 */

	public void createFieldEditors() {
		addField(new DirectoryFieldEditor(P_PATH, 
				"&Directory preference:", getFieldEditorParent()));
		addField(
			new BooleanFieldEditor(
					"booleanPreference",
				"&An example of a boolean preference",
				getFieldEditorParent()));

		
		String dateOptions[] = VerySimpleDateFormat.DATE_PATTERNS;
		String dateOptions2[][] = new String[dateOptions.length][];
		for (int i = 0; i < dateOptions.length; i++) {
			dateOptions2[i] = 
				new String[] { dateOptions[i], dateOptions[i] }; 
		}
		
		addField(new RadioGroupFieldEditor(
			"dateFormat",
			"Date Format",
			1,
			dateOptions2,
//			new String[][] { { "&Choice 1", "choice1" }, {
//				"C&hoice 2", "choice2" }
//		}, 
			getFieldEditorParent()));
		
		addField(
			new StringFieldEditor("stringPreference", "A &text preference:", getFieldEditorParent()));
	}
	
	public void init(IWorkbench workbench) {
	}
}