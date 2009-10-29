/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004, 2009 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.ofx.model;

import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IValues;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.ofx.resources.Messages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * Provides the metadata for the extra properties added to each
 * entry by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class OfxEntryInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<OfxEntry> propertySet = PropertySet.addExtensionPropertySet(OfxEntry.class, EntryInfo.getPropertySet(), new IExtensionObjectConstructors<OfxEntry>() {

		public OfxEntry construct(ExtendableObject extendedObject) {
			return new OfxEntry(extendedObject);
		}

		public OfxEntry construct(ExtendableObject extendedObject, IValues values) {
			return new OfxEntry(
					extendedObject, 
					values.getScalarValue(getFitidAccessor())
			);
		}
	});
	
	private static ScalarPropertyAccessor<String> fitidAccessor = null;
	
	public PropertySet registerProperties() {
		class NonEditableTextControlFactory extends PropertyControlFactory<String> {
			
			public IPropertyControl createPropertyControl(Composite parent, final ScalarPropertyAccessor<String> propertyAccessor) {
				
				// Property is not editable
		        final Label control = new Label(parent, SWT.NONE);
		        return new IPropertyControl<ExtendableObject>() {

					public Control getControl() {
						return control;
					}

					public void load(ExtendableObject object) {
						String text = object.getPropertyValue(propertyAccessor);
						if (text == null) {
							control.setText("");
						} else {
							control.setText(text);
						}
					}

					public void save() {
						/*
						 * The property is not editable so there is nothing
						 * to do here.
						 */
					}
		        };
			}

			@Override
			public String formatValueForMessage(ExtendableObject extendableObject, ScalarPropertyAccessor<? extends String> propertyAccessor) {
				String value = extendableObject.getPropertyValue(propertyAccessor);
				return (value == null) ? "<blank>" : value;
			}

			@Override
			public String formatValueForTable(ExtendableObject extendableObject, ScalarPropertyAccessor<? extends String> propertyAccessor) {
				String value = extendableObject.getPropertyValue(propertyAccessor);
				return (value == null) ? "" : value;
			}

			public String getDefaultValue() {
				return null;
			}

			public boolean isEditable() {
				return true;
			}
		}

		fitidAccessor  = propertySet.addProperty("fitid", Messages.Entry_Fitid, String.class, 1, 80, new NonEditableTextControlFactory(), null);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<OfxEntry> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String> getFitidAccessor() {
		return fitidAccessor;
	}
}
