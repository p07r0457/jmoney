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

package net.sf.jmoney.model2;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.JComponent;

/**
 * This class is a wrapper class to an entry property editor class provided by an extension.
 * This class provides all the necessary introspection code.
 *
 * @author  Nigel
 */
public class EntryEditor {
    
    private JComponent editor;
    
    private PropertySet propertySet;
    
    private Method theLoadMethod;
    
    private Method theSaveMethod;
    
    /** Creates a new instance of EntryEditor */
    public EntryEditor(JComponent editor, PropertySet propertySet) {
        this.editor = editor;
        this.propertySet = propertySet;
        
        // Use introspection on the interface to find the load method.
        // The load method takes one parameter which an entry extension of the appropriate class.
    
        Class parameterTypes[] = { propertySet.getImplementationClass() };
        try {
            this.theLoadMethod = editor.getClass().getDeclaredMethod("load", parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new MalformedPluginException("Method 'load(" + propertySet.getImplementationClass().getName() + ")' in '" + editor.getClass().getName() + "' was not found.");
        }
        
        if (theLoadMethod.getReturnType() != void.class) {
            throw new MalformedPluginException("Method 'load(" + propertySet.getImplementationClass().getName() + ")' in '" + editor.getClass().getName() + "' must return void.");
        }
        
        // The save method takes no parameters and must return void.
    
        try {
            this.theSaveMethod = editor.getClass().getDeclaredMethod("save", (Class[])null);
        } catch (NoSuchMethodException e) {
            throw new MalformedPluginException("Method 'save()' in '" + editor.getClass().getName() + "' was not found.");
        }
        
        if (theSaveMethod.getReturnType() != void.class) {
            throw new MalformedPluginException("Method 'save()' in '" + editor.getClass().getName() + "' must return void.");
        }
    }
    
    /**
     * This method takes as input the base object for the entry but passes on the
     * appropriate extension object.
     *
     * @param entry is the entry which contains the data to be loaded into the control.
     *      If entry is null then the control should be put into a state appropriate
     *      for when no entry is selected.
     */
    public void load(Entry entry) {
	Object[] parameters = new Object[1];
        if (entry == null) {
            parameters[0] = null;
        } else {
            parameters[0] = entry.getExtension(propertySet);
        }
        
        try {
            theLoadMethod.invoke(editor, parameters);
        } catch (IllegalAccessException e) {
            throw new MalformedPluginException("load must be public");
        } catch (InvocationTargetException e) {
            throw new MalformedPluginException("load gives InvocationTargetException");
        }
    }
    
    public void save() {
        try {
            theSaveMethod.invoke(editor, (Object[])null);
        } catch (IllegalAccessException e) {
            throw new MalformedPluginException("load must be public");
        } catch (InvocationTargetException e) {
            throw new MalformedPluginException("load gives InvocationTargetException");
        }
    }
    
    public JComponent getEditor() {
        return editor;
    }
}
