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

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertyRegistrar;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;

import org.eclipse.swt.widgets.Composite;

/**
 * Provides the metadata for the extra properties added to each
 * entry by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class ReconciliationEntryInfo implements IPropertySetInfo {

	private static PropertySet<ReconciliationEntry> propertySet = null;
	private static ScalarPropertyAccessor<Integer> statusAccessor = null;
	private static ScalarPropertyAccessor<BankStatement> statementAccessor = null;
	private static ScalarPropertyAccessor<String> uniqueIdAccessor = null;
	
	public Class getImplementationClass() {
		return ReconciliationEntry.class;
	}
	
	public void registerProperties(IPropertyRegistrar propertyRegistrar) {
		propertySet = propertyRegistrar.addPropertySet(ReconciliationEntry.class);

		class NonEditableTextControlFactory extends PropertyControlFactory<String> {
			public IPropertyControl createPropertyControl(Composite parent, ScalarPropertyAccessor<String> propertyAccessor, Session session) {
				// Property is not editable
				return null;
			}

			public String formatValueForMessage(ExtendableObject extendableObject, ScalarPropertyAccessor<? extends String> propertyAccessor) {
				String value = extendableObject.getPropertyValue(propertyAccessor);
				return (value == null) ? "<blank>" : value;
			}

			public String formatValueForTable(ExtendableObject extendableObject, ScalarPropertyAccessor<? extends String> propertyAccessor) {
				String value = extendableObject.getPropertyValue(propertyAccessor);
				return (value == null) ? "" : value;
			}

			public boolean isEditable() {
				return false;
			}
		};

		IPropertyControlFactory<BankStatement> statementControlFactory = new PropertyControlFactory<BankStatement>() {
			public IPropertyControl createPropertyControl(Composite parent, ScalarPropertyAccessor<BankStatement> propertyAccessor, Session session) {
				// Property is not editable
				return null;
			}

			public String formatValueForMessage(ExtendableObject extendableObject, ScalarPropertyAccessor<? extends BankStatement> propertyAccessor) {
				BankStatement statement = extendableObject.getPropertyValue(propertyAccessor);
				if (statement == null) {
					return "unreconciled";
				} else {
					return statement.toString();
				}
			}

			public String formatValueForTable(ExtendableObject extendableObject, ScalarPropertyAccessor<? extends BankStatement> propertyAccessor) {
				BankStatement statement = extendableObject.getPropertyValue(propertyAccessor);
				if (statement == null) {
					return "unreconciled";
				} else {
					return statement.toString();
				}
			}

			public boolean isEditable() {
				return false;
			}
		};
		
		// TODO: correct localized text:
		statusAccessor    = propertyRegistrar.addProperty("status", ReconciliationPlugin.getResourceString("Entry.statusShort"), Integer.class, 1, 20, new StatusControlFactory(), null);
		statementAccessor = propertyRegistrar.addProperty("statement", ReconciliationPlugin.getResourceString("Entry.statementShort"), BankStatement.class, 1, 20, statementControlFactory, null);
		uniqueIdAccessor  = propertyRegistrar.addProperty("uniqueId", ReconciliationPlugin.getResourceString("Entry.uniqueIdShort"), String.class, 1, 20, new NonEditableTextControlFactory(), null);
	}

	/**
	 * @return
	 */
	public static PropertySet<ReconciliationEntry> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Integer> getStatusAccessor() {
		return statusAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<BankStatement> getStatementAccessor() {
		return statementAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String> getUniqueIdAccessor() {
		return uniqueIdAccessor;
	}
}
