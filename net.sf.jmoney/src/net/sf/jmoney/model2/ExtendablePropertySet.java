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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.swt.graphics.Image;

public class ExtendablePropertySet<E extends ExtendableObject> extends PropertySet<E> {

	ExtendablePropertySet<? super E> basePropertySet;

	/**
	 * true if further property sets must be derived from this property set,
	 * false if property sets cannot be derived from this property set.
	 */
	protected boolean derivable;

	/**
	 * Set of property sets that are derived from this property set
	 * (either directly or indirectly) and that are not
	 * themselves derivable.
	 */
	private Map<Class<? extends E>, ExtendablePropertySet<? extends E>> derivedPropertySets = new HashMap<Class<? extends E>, ExtendablePropertySet<? extends E>>();

	Map<String, ExtensionPropertySet> extensionPropertySets = null;  

	/**
	 * Localized text describing the type of object represented
	 * by this property set.
	 * <P>
	 * This field is defined only if this property set is not derivable.
	 */
	protected String objectDescription;  // defined only if not derivable

	/**
	 * This field is valid for extendable property sets only
	 */
	protected String iconFileName = null;

	/** cached value */
	private Image iconImage = null;

	/**
	 * These arrays are built on first use and then cached.
	 */
	private Vector<PropertyAccessor> properties2 = null;
	private Vector<ScalarPropertyAccessor> scalarProperties2 = null;
	private Vector<ListPropertyAccessor> listProperties2 = null;

	private Vector<PropertyAccessor> properties3 = null;
	private Vector<ScalarPropertyAccessor> scalarProperties3 = null;
	private Vector<ListPropertyAccessor> listProperties3 = null;

	/**
	 * This field is valid for non-derivable property sets only.
	 */
	private Vector<PageEntry> pageExtensions = null;

	/**
	 * Constructs a base property set object.
	 *  
	 * @param classOfObject
	 */
	protected ExtendablePropertySet(Class<E> classOfObject) {
		this.isExtension = false;
		this.classOfObject = classOfObject;
		this.basePropertySet = null;

		this.derivable = false;
		this.objectDescription = null;
		this.iconFileName = null;

		extensionPropertySets = new HashMap<String, ExtensionPropertySet>();
	}
	
	/**
	 * Constructs a derived property set object.
	 *  
	 * @param classOfObject
	 */
	protected ExtendablePropertySet(Class<E> classOfObject, ExtendablePropertySet<? super E> basePropertySet) {
		this.isExtension = false;
		this.classOfObject = classOfObject;
		this.basePropertySet = basePropertySet;

		this.derivable = false;
		this.objectDescription = null;
		this.iconFileName = null;

		extensionPropertySets = new HashMap<String, ExtensionPropertySet>();
	}

	public void initProperties(String propertySetId) {
		super.initProperties(propertySetId);
		
		// Add to our map that maps ids to ExtendablePropertySet objects.
		allExtendablePropertySetsMap.put(propertySetId, this);

		/*
		 * Add to the map that maps the extendable classes to the extendable
		 * property sets. Only final property sets (ones which do not define an
		 * enumerated type to control derived classes) are put in the map. This
		 * is important because when we are looking for the property set for an
		 * instance of an object, we want to be sure we find only the final
		 * property set for that object.
		 */
		if (!derivable) {
			if (classToPropertySetMap.containsKey(classOfObject)) {
				throw new MalformedPluginException("More than one property set uses " + classOfObject + " as the Java implementation class.");
			}
			classToPropertySetMap.put(classOfObject, this);

			pageExtensions = new Vector<PageEntry>();
		}

		if (basePropertySet != null && !basePropertySet.isDerivable()) {
			throw new MalformedPluginException(basePropertySet.getImplementationClass().getName() + " is a base property for " + propertySetId + ".  However, " + basePropertySet.getImplementationClass().getName() + " is not derivable (setDerivable() has not been called from the IPropertySetInfo implementation).");
		}

		if (!derivable) {
			// Add this property set to the list of derived property sets
			// for this and all the base classes.
			for (ExtendablePropertySet<? super E> base = this; base != null; base = base.getBasePropertySet()) {
				base.derivedPropertySets.put(classOfObject, this);
			}
		} else {
			if (objectDescription != null) {
				throw new MalformedPluginException("IPropertyRegistrar.setObjectDescription is called from the IPropertyInfo implementation for " + propertySetId + ", but the property set is derivable.");
			}
		}
		
		if (!derivable) {
			// Build the list of properties that are passed to
			// the 'new object' constructor and another list that
			// are passed to the 're-instantiating' constructor.

			constructorProperties = new Vector<PropertyAccessor>();
			defaultConstructorProperties = new Vector<PropertyAccessor>();
		}

		// We need to be able to iterate through property sets
		// starting at the base property set and continuing through
		// derived property sets until we get to this property set.
		// To do this, we first build a list of the base property sets
		// and we can then iterate through these property sets in
		// reverse order.
		Vector<ExtendablePropertySet> basePropertySets = new Vector<ExtendablePropertySet>();
		for (ExtendablePropertySet base = getBasePropertySet(); base != null; base = base.getBasePropertySet()) {
			basePropertySets.add(base);
		}

		// Add the appropriate properties from the base classes to
		// the constructorParameters and defaultConstructorParameters arrays.
		int parameterIndex = 3;

		for (int i = basePropertySets.size()-1; i >= 0; i--) {
			ExtendablePropertySet<?> base = basePropertySets.get(i);

			for (PropertyAccessor propertyAccessor: base.properties) {
				if (!isDerivable()) {
					constructorProperties.add(propertyAccessor);
					if (propertyAccessor.isList()) {
						defaultConstructorProperties.add(propertyAccessor);
					}
				}

				parameterIndex++;
			}
		}

		// Process the properties in this property set.
		for (PropertyAccessor propertyAccessor: properties) {
			propertyAccessor.setIndexIntoConstructorParameters(parameterIndex++);
			if (!isDerivable()) {
				constructorProperties.add(propertyAccessor);
				if (propertyAccessor.isList()) {
					defaultConstructorProperties.add(propertyAccessor);
				}
			}
		}

		if (!derivable) {
			findConstructors(false);
		}

		/*
		 * Set the icon associated with this property set. The icon will already
		 * have been set in any property set for which an icon is specifically
		 * set. However, icons also apply to derived property sets for which no
		 * icon has been set. So, if the icon is null, go up the list of base
		 * property sets until we find a non-null icon.
		 * 
		 * This must be done here and not in the constructor because the calling
		 * code must have a change to set an icon before this code is executed.
		 */
		if (iconFileName == null) {
			for (ExtendablePropertySet base = getBasePropertySet(); base != null; base = base.getBasePropertySet()) {
				if (base.getIconFileName() != null) {
					iconFileName = base.getIconFileName();
					break;
				}
			}
		}
	}

	public void initPropertiesPass2() {
		int scalarIndex = 0;
		for (ExtendablePropertySet base = getBasePropertySet(); base != null; base = base.getBasePropertySet()) {
			scalarIndex += base.getScalarProperties2().size();
		}
		
		for (ScalarPropertyAccessor propertyAccessor: getScalarProperties2()) {
			propertyAccessor.setIndexIntoScalarProperties(scalarIndex++);
		}

		
	}
	
	/**
	 * 
	 * @return the set of all property sets
	 * 				that are derived from this property set and
	 * 				that are themselves not derivable
	 */
	public Collection<ExtendablePropertySet<? extends E>> getDerivedPropertySets() {
		return derivedPropertySets.values();
	}

	/**
	 * Given a class of an object, returns the property
	 * set for that object.  The class passed to this method
	 * must be the class of an ExtendableObject that either is
	 * the implementation object for this property set or is
	 * extended from the implemetation class.  The class must be
	 * a final class (i.e. a class of an actual object instance,
	 * not an abstract class).
	 * 
	 * If this property set is a final property set then this
	 * method will always return this object.
	 *  
	 * @return the final property set
	 */
	public ExtendablePropertySet<? extends E> getActualPropertySet(Class<? extends E> classOfObject) {
		return derivedPropertySets.get(classOfObject);
	}

	/**
	 * 
	 * @return If this property set is derived from another property
	 * 			set then the base property set is returned, otherwise
	 * 			null is returned.
	 */
	public ExtendablePropertySet<? super E> getBasePropertySet() {
		if (isExtension) {
			throw new RuntimeException("getBasePropertySet called for an extension.");
		}

		return basePropertySet;
	}

	/**
	 * @return localized text describing the type of object
	 * 			represented by this property set
	 */
	public String getObjectDescription() {
		if (isDerivable()) {
			throw new RuntimeException("internal error");
		}
		return objectDescription;
	}

	/**
	 * @return True if this property set can only be used by
	 * 			deriving another property set from it, false
	 * 			if property sets cannot be derived from this
	 * 			property set.
	 */
	public boolean isDerivable() {
		return derivable;
	}

	/**
	 * This method should be called to indicate that the property set is
	 * an abstract property set and that other property sets should be
	 * derived from this property set.
	 */
	public void setDerivable() {
		derivable = true;
	}

	public void setDescription(String description) {
		this.objectDescription = description;
	}

	public void setIcon(String iconFileName) {
		this.iconFileName = iconFileName;
	}

	/** used internally */
	String getIconFileName() {
		return iconFileName;
	}

	/**
	 * This method creates the image on first call.  It is very
	 * important that the image is not created when the this PropertySet
	 * object is initialized.  The reason is that this PropertySet is
	 * initialized by a different thread than the UI thread.  Images
	 * must be created by UI thread.
	 * <P>
	 * This method is valid for extendable property sets only.
	 * 
	 * @return the icon associated with objects that implement
	 * 			this property set.
	 */
	public Image getIcon() {
		if (iconImage == null)
			iconImage = JMoneyPlugin.createImage(iconFileName);
		return iconImage;
	}

	/**
	 * Gets the accessor for a property given the local name of the property.
	 *
	 * This method searches only this property set and any base
	 * property sets.  No extensions are searched.
	 * <P>
	 * This method is used when a column name is persisted in, say, a file
	 * and we are keen to keep the data in the file as simple and short as
	 * possible.  We therefore allow local names only to be specified.
	 * Local names may not be unique when extensions are included, so we
	 * must require fully qualified names for extensions.
	 * 
	 * @param name The local property name
	 */
	public PropertyAccessor getPropertyAccessorGivenLocalNameAndExcludingExtensions(String localPropertyName) throws PropertyNotFoundException {
		ExtendablePropertySet thisPropertySet = this;
		do {
			try {
				return thisPropertySet.getProperty(localPropertyName);
			} catch (PropertyNotFoundException e) {
			}
			thisPropertySet = thisPropertySet.getBasePropertySet();
		} while (thisPropertySet != null);

		throw new PropertyNotFoundException(propertySetId, localPropertyName);
	}


	/**
	 * Returns the set of all properties of the given set of property sets,
	 * including both properties in the extendable object and properties in
	 * extension property sets.
	 * <P>
	 * Properties from base property sets and properties from derived property
	 * sets are not returned.
	 * 
	 * @return a collection of <code>PropertyAccessor</code> objects
	 */
	private Collection<PropertyAccessor> getProperties2() {
		if (properties2 == null) {
			properties2 = new Vector<PropertyAccessor>();

			// Properties in this extendable object
			for (PropertyAccessor propertyAccessor: properties) {
				properties2.add(propertyAccessor);
			}

			// Properties in the extensions
			for (PropertySet<?> extensionPropertySet: extensionPropertySets.values()) {
				for (PropertyAccessor propertyAccessor: extensionPropertySet.properties) {
					properties2.add(propertyAccessor);
				}
			}
		}

		return properties2;
	}

	public Collection<ScalarPropertyAccessor> getScalarProperties2() {
		if (scalarProperties2 == null) {
			scalarProperties2 = new Vector<ScalarPropertyAccessor>();

			// Properties in this extendable object
			for (ScalarPropertyAccessor propertyAccessor: getScalarProperties1()) {
				scalarProperties2.add(propertyAccessor);
			}

			// Properties in the extensions
			for (PropertySet<?> extensionPropertySet: extensionPropertySets.values()) {
				for (ScalarPropertyAccessor propertyAccessor: extensionPropertySet.getScalarProperties1()) {
					scalarProperties2.add(propertyAccessor);
				}
			}
		}

		return scalarProperties2;
	}

	public Collection<ListPropertyAccessor> getListProperties2() {
		if (listProperties2 == null) {
			listProperties2 = new Vector<ListPropertyAccessor>();

			// Properties in this extendable object
			for (ListPropertyAccessor propertyAccessor: getListProperties1()) {
				listProperties2.add(propertyAccessor);
			}

			// Properties in the extensions
			for (PropertySet<?> extensionPropertySet: extensionPropertySets.values()) {
				for (ListPropertyAccessor propertyAccessor: extensionPropertySet.getListProperties1()) {
					listProperties2.add(propertyAccessor);
				}
			}
		}

		return listProperties2;
	}

	/**
	 * Returns an iterator which iterates over all properties
	 * of the given set of property sets, including all
	 * extension properties, and all base properties including
	 * all extension properties to the base property sets.
	 * <P>
	 * This is the set of properties that can be set against
	 * an object that implements this property set.
	 * <P>
	 * Properties are returned with the properties from the
	 * base-most class first, then properties from the class
	 * immediately derived from the base-most class, and so
	 * on with the properties from this property set being
	 * last.  This order gives the most intuitive order from
	 * the user's perspective.  This order also ensures that
	 * a property in a base class has the same index in the returned order,
	 * regardless of the actual derived property set.
	 * 
	 * @ return An iterator which iterates over a set of
	 * 		<code>PropertyAccessor</code> objects.
	 */
	public Collection<PropertyAccessor> getProperties3() {
		if (properties3 == null) {
			Vector<PropertyAccessor> v = new Vector<PropertyAccessor>();

			// Properties in this and all the base property sets
			ExtendablePropertySet<?> extendablePropertySet = this;
			do {
				int index= 0;
				for (PropertyAccessor propertyAccessor: extendablePropertySet.getProperties2()) {
					v.insertElementAt(propertyAccessor, index++);
				}
				extendablePropertySet = extendablePropertySet.getBasePropertySet();
			} while (extendablePropertySet != null);

			properties3 = v;
		}

		return properties3;
	}

	public Collection<ScalarPropertyAccessor> getScalarProperties3() {
		if (scalarProperties3 == null) {
			Vector<ScalarPropertyAccessor> v = new Vector<ScalarPropertyAccessor>();

			// Properties in this and all the base property sets
			ExtendablePropertySet<?> extendablePropertySet = this;
			do {
				int index= 0;
				for (ScalarPropertyAccessor propertyAccessor: extendablePropertySet.getScalarProperties2()) {
					v.insertElementAt(propertyAccessor, index++);
				}
				extendablePropertySet = extendablePropertySet.getBasePropertySet();
			} while (extendablePropertySet != null);

			scalarProperties3 = v;
		}

		return scalarProperties3;
	}

	public Collection<ListPropertyAccessor> getListProperties3() {
		if (listProperties3 == null) {
			Vector<ListPropertyAccessor> v = new Vector<ListPropertyAccessor>();

			// Properties in this and all the base property sets
			ExtendablePropertySet<?> extendablePropertySet = this;
			do {
				int index= 0;
				for (ListPropertyAccessor propertyAccessor: extendablePropertySet.getListProperties2()) {
					v.insertElementAt(propertyAccessor, index++);
				}
				extendablePropertySet = extendablePropertySet.getBasePropertySet();
			} while (extendablePropertySet != null);

			listProperties3 = v;
		}

		return listProperties3;
	}

	/**
	 * Gets a list of all property sets that extend the given property
	 * set.  This method is used by the Propagator class only.
	 * <P>
	 * Note:
	 * This method does not return derived property sets.
	 * This method does not return property sets that extend any 
	 * property sets from which this property set is derived.
	 *  
	 * @return the extension property sets that extend this property
	 * 			set
	 */
	public Collection<ExtensionPropertySet> getExtensionPropertySets() {
		return extensionPropertySets.values();
	}
	
	
	/**
	 * Returns the set of tabbed pages that are to be shown in the
	 * editor associated with extendable objects of this property set.
	 * <P>
	 * This method is valid only for non-derivable extendable property sets.
	 * 
	 * @return a set of objects of type PageEntry
	 */
	public Vector<PageEntry> getPageFactories() {
		return pageExtensions;		
	}


	/**
	 * @param pageEntry
	 */
	public void addPage(PageEntry newPage) {
		int addIndex = pageExtensions.size();
		for (int i = 0; i < pageExtensions.size(); i++) {
			PageEntry page = (PageEntry) pageExtensions.get(i);
			if (newPage.getPosition() < page.getPosition()) {
				addIndex = i;
				break;
			}
		}
		pageExtensions.add(addIndex, newPage);
	}



}
