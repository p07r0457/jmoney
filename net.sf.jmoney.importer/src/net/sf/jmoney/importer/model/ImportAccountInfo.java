/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2010 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.importer.model;

import java.util.ArrayList;
import java.util.List;

import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CapitalAccountInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IListGetter;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IValues;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.ObjectCollection;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Provides the metadata for the extra properties added to each
 * currency account by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class ImportAccountInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<ImportAccount> propertySet = PropertySet.addExtensionPropertySet(ImportAccount.class, CapitalAccountInfo.getPropertySet(), new IExtensionObjectConstructors<ImportAccount>() {

		public ImportAccount construct(ExtendableObject extendedObject) {
			return new ImportAccount(extendedObject);
		}

		public ImportAccount construct(ExtendableObject extendedObject, IValues values) {
			return new ImportAccount(
					extendedObject, 
					values.getScalarValue(getImportDataExtensionIdAccessor()),
					values.getListManager(extendedObject.getObjectKey(), getAssociationsAccessor()) 
			);
		}
	});
	
	private static ScalarPropertyAccessor<String> importDataExtensionIdAccessor = null;
	private static ListPropertyAccessor<AccountAssociation> associationsAccessor = null;
	
	public PropertySet<ImportAccount> registerProperties() {

		IPropertyControlFactory<String> importDataControlFactory = new PropertyControlFactory<String>() {
			@Override
			public IPropertyControl<CapitalAccount> createPropertyControl(Composite parent,
					ScalarPropertyAccessor<String> propertyAccessor) {
				
				final List<String> ids = new ArrayList<String>();
				final Combo control = new Combo(parent, SWT.READ_ONLY);
				
				ids.add(null);
				control.add("none");
				
				IExtensionRegistry registry = Platform.getExtensionRegistry();
				for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.importer.importdata")) { //$NON-NLS-1$
					if (element.getName().equals("import-format")) { //$NON-NLS-1$
						String label = element.getAttribute("label"); //$NON-NLS-1$
						String id = element.getAttribute("id"); //$NON-NLS-1$
						ids.add(id);
						control.add(label);
					}
				}

				control.setVisibleItemCount(15);
				
				return new IPropertyControl<CapitalAccount>() {

					private CapitalAccount account;

					@Override
					public Control getControl() {
						return control;
					}

					@Override
					public void load(CapitalAccount account) {
						this.account = account;
						
						String importFormatId = account.getPropertyValue(getImportDataExtensionIdAccessor());
						int index = ids.indexOf(importFormatId);
						control.select(index);
					}

					@Override
					public void save() {
						String importFormatId = ids.get(control.getSelectionIndex());
						account.setPropertyValue(getImportDataExtensionIdAccessor(), importFormatId);
					}};
			}

			@Override
			public String getDefaultValue() {
				// By default, no extension is set
				return null;
			}

			@Override
			public boolean isEditable() {
				return true;
			}
		};

		IListGetter<ImportAccount, AccountAssociation> associationListGetter = new IListGetter<ImportAccount, AccountAssociation>() {
			public ObjectCollection<AccountAssociation> getList(ImportAccount parentObject) {
				return parentObject.getAssociationCollection();
			}
		};
	
		importDataExtensionIdAccessor = propertySet.addProperty("importDataExtensionId", "Table Structure", String.class, 1, 5, importDataControlFactory, null);
		associationsAccessor = propertySet.addPropertyList("associations", "Associations", AccountAssociationInfo.getPropertySet(), associationListGetter);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<ImportAccount> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String> getImportDataExtensionIdAccessor() {
		return importDataExtensionIdAccessor;
	}	

	/**
	 * @return
	 */
	public static ListPropertyAccessor<AccountAssociation> getAssociationsAccessor() {
		return associationsAccessor;
	}	
}
