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

package net.sf.jmoney.reconciliation;

import javax.swing.JComboBox;

/**
 * Note that this class has neither get/set methods for the value being edited
 * and no support for property change listeners.  This is
 * because objects of this class are tied to an Entry object.  Changes to this
 * object are reflected by this object in the Entry class objects.  
 * Consumers who are interested in changes to the Entry class objects should
 * add themselves as listeners to the appropriate PropertyAccessor object.
 *
 * @author  Nigel
 */
public class StatusEditor extends JComboBox {
    
    private int value;
    
    private ReconciliationEntry entry = null;

    /** Creates new StatusEditor */
    public StatusEditor() {
    	super(
    			new String[] { 
    					ReconciliationPlugin.getResourceString("Entry.uncleared"),
						ReconciliationPlugin.getResourceString("Entry.reconciling"),
						ReconciliationPlugin.getResourceString("Entry.cleared"),
    			}
    	);
    }
    
    
    /**
     * This method is called by the super class when the user changes a selection
     * but not when the selection is changed by the code.  We override this
     * method so we can update the entry with the new selection whenever the user
     * changes the selection.
     */
    public void setSelectedIndex(int index) {
        super.setSelectedIndex(index);
        entry.setStatus(index);
    }
   
    /**
     * Load the control with the value from the given entry.
     */
    public void load(ReconciliationEntry entry) {
        try {
            this.entry = entry;
            if (entry != null) {
                super.setSelectedIndex(entry.getStatus());
            } else {
                // If no entry selected, set so nothing is selected.
                super.setSelectedIndex(-1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Save the value from the control back into the entry.
     *
     * Beans may update the entry on a regular basis, not just when
     * the framework calls the <code>save</code> method.  However, the only time
     * that beans must update the entry is when the framework calls this method.
     *
     * In this implementation we save the value back into the entry when the selection
     * is changed.  This causes the change to be seen in other views as soon as the
     * user changes the selection.
     *
     * The framework should never call this method when no entry is selected
     * so we can assume that <code>entry</code> is not null.
     */
    public void save() {
        int index = getSelectedIndex();
        entry.setStatus(index);
    }
}
