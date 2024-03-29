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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class ExtendablePropertySet<E extends ExtendableObject> extends PropertySet<E> {

	ExtendablePropertySet<? super E> basePropertySet;

	/**
	 * An interface that can be used to construct implementation objects,
	 * or null if this is an abstract property set.
	 */
	IExtendableObjectConstructors<E> constructors;
	
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

	private Collection<ExtendablePropertySet<? extends E>> directlyDerivedPropertySets = new Vector<ExtendablePropertySet<? extends E>>(); 
	
	Map<String, ExtensionPropertySet<?>> extensionPropertySets = null;  

	/**
	 * Localized text describing the type of object represented
	 * by this property set.  This property is never null.
	 */
	protected String objectDescription;

	/**
	 * This field is valid for extendable property sets only.
	 * <P>
	 * This field is never null. A null image descriptor may be passed during
	 * construction if this is not a base extendable object, but when a null
	 * image is passed the image will be derived from the base class.
	 */
	private ImageDescriptor iconImageDescriptor = null;

	/**
	 * This image is created when first needed and kept throughout the lifetime
	 * of the application.
	 */
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
	 * Constructs a property set object.
	 * 
	 * @param classOfObject
	 *            the class of the implementation object
	 * @param objectDescription
	 *            a localized description of this object class, suitable for use
	 *            in the UI
	 * @param basePropertySet
	 *            the property set from which this property set is derived, or
	 *            null if we are constructing a base property set
	 * @param constructors
	 *            an interface containing methods for constructing
	 *            implementation objects, or null if this is an abstract
	 *            property set
	 */
	protected ExtendablePropertySet(Class<E> classOfObject, String objectDescription, ExtendablePropertySet<? super E> basePropertySet, IExtendableObjectConstructors<E> constructors) {
		this.isExtension = false;
		this.classOfObject = classOfObject;
		this.basePropertySet = basePropertySet;
		this.constructors = constructors;
		
		this.derivable = (constructors == null);
		this.objectDescription = objectDescription;
		this.iconImageDescriptor = null;

		extensionPropertySets = new HashMap<String, ExtensionPropertySet<?>>();
	}

	@Override
	public void initProperties(String propertySetId) {
		super.initProperties(propertySetId);
		
		// Add to our map that maps ids to ExtendablePropertySet objects.
		allExtendablePropertySetsMap.put(propertySetId, this);

		/*
		 * Add to the map that maps the extendable classes to the extendable
		 * property sets. Both final and derived property sets are put in this
		 * map.
		 */
		if (classToPropertySetMap.containsKey(classOfObject)) {
			throw new MalformedPluginException("More than one property set uses " + classOfObject + " as the Java implementation class."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		classToPropertySetMap.put(classOfObject, this);

		if (basePropertySet != null && !basePropertySet.isDerivable()) {
			throw new MalformedPluginException(basePropertySet.getImplementationClass().getName() + " is a base property for " + propertySetId + ".  However, " + basePropertySet.getImplementationClass().getName() + " is not derivable (setDerivable() has not been called from the IPropertySetInfo implementation)."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		if (!derivable) {
			pageExtensions = new Vector<PageEntry>();

			// Add this property set to the list of derived property sets
			// for this and all the base classes.
			for (ExtendablePropertySet<? super E> base = this; base != null; base = base.getBasePropertySet()) {
				base.derivedPropertySets.put(classOfObject, this);
			}
		}

		if (basePropertySet != null) {
			basePropertySet.directlyDerivedPropertySets.add(this);
		}

		/*
		 * Set the icon associated with this property set. The icon will already
		 * have been set in any property set for which an icon is specifically
		 * set. However, icons also apply to derived property sets for which no
		 * icon has been set. So, if the icon is null, go up the list of base
		 * property sets until we find a non-null icon.
		 * 
		 * This must be done here and not in the constructor because the calling
		 * code must have a chance to set an icon before this code is executed.
		 */
		if (iconImageDescriptor == null) {
			for (ExtendablePropertySet base = getBasePropertySet(); base != null; base = base.getBasePropertySet()) {
				if (base.iconImageDescriptor != null) {
					iconImageDescriptor = base.iconImageDescriptor;
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
	 * Gets the set of all property sets that are directly derived from this
	 * property set. This set does not include property sets that are derived
	 * from property sets that are in turn derived from this property set. This
	 * set includes both property sets that are derivable and property sets that
	 * are final.
	 * 
	 * This method is useful when the caller needs to know the actual tree
	 * structure of the derived property sets or needs to know about the
	 * intermediate (non-final) property sets. Callers generally would call this
	 * method in a recursive manner.
	 * 
	 * If a caller just needs a list of the final property sets,
	 * getDerivedPropertySets() is simpler to use.
	 */
	public Collection<ExtendablePropertySet<? extends E>> getDirectlyDerivedPropertySets() {
		return directlyDerivedPropertySets;
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
	 * Given a property set id of the property set that is derived from this
	 * property set, return the property set.
	 * <P>
	 * This method will give the same result as the static method
	 * PropertySet.getExtendablePropertySet provided that the id is for a
	 * property set that is derived from this property set. If the id is for a
	 * property set that is not derived from this property set then null will be
	 * returned. The advantage of using this method is that the returned
	 * property set is parameterized with &lt;? extends E>.
	 * @throws PropertySetNotFoundException 
	 */
	public ExtendablePropertySet<? extends E> getDerivedPropertySet(String propertySetId) throws PropertySetNotFoundException {
		for (ExtendablePropertySet<? extends E> propertySet : derivedPropertySets.values()) {
			if (propertySet.getId().equals(propertySetId)) {
				return propertySet;
			}
		}
		throw new PropertySetNotFoundException(propertySetId);
	}
	
	/**
	 * Given a class of an object, returns the property
	 * set for that object.  The class passed to this method
	 * must be the class of an ExtendableObject that either is
	 * the implementation object for this property set or is
	 * extended from the implementation class.  The class must be
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
			throw new RuntimeException("getBasePropertySet called for an extension."); //$NON-NLS-1$
		}

		return basePropertySet;
	}

	/**
	 * @return localized text describing the type of object
	 * 			represented by this property set
	 */
	public String getObjectDescription() {
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
	 * Set an icon that is to be shown for objects of this class.
	 * If no icon is set then the icon for the base class will be
	 * used or, if there is no base class, no icon will be shown
	 * for objects of this class.
	 *   
	 * @param iconImageDescriptor
	 */
	public void setIcon(ImageDescriptor iconImageDescriptor) {
		this.iconImageDescriptor = iconImageDescriptor;
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
	// TODO remove this method and use getImageDescriptor only.
	public Image getIconImage() {
		if (iconImage == null) {
			if (iconImageDescriptor != null) {
				iconImage = iconImageDescriptor.createImage();
			} else {
				iconImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
			}
		}
		return iconImage;
	}

	public ImageDescriptor getIconImageDescriptor() {
		if (iconImageDescriptor == null) {
			/*
			 * No object from which this object directly or indirectly
			 * extends has an icon. Use a default one.
			 */
			iconImageDescriptor = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_ELEMENT);
		}
		return iconImageDescriptor;
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

	/**
	 * Returns the set of all scalar properties (i.e. list properties are
	 * excluded) of the given set of property sets, including both properties in
	 * the extendable object and properties in extension property sets.
	 * <P>
	 * Properties from base property sets and properties from derived property
	 * sets are not returned.
	 * 
	 * @return a collection of <code>PropertyAccessor</code> objects
	 */
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

	/**
	 * Returns the set of all list properties (i.e. scalar properties are
	 * excluded) of the given set of property sets, including both properties in
	 * the extendable object and properties in extension property sets.
	 * <P>
	 * Properties from base property sets and properties from derived property
	 * sets are not returned.
	 * 
	 * @return a collection of <code>PropertyAccessor</code> objects
	 */
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
	 * Returns the set of all properties of the given set of property sets,
	 * including properties in the extendable object, properties in extension
	 * property sets, and all properties in the base property sets including all
	 * extension properties to the base property sets.
	 * <P>
	 * This is the set of properties that can be set against an object that
	 * implements this property set.
	 * <P>
	 * Properties are returned with the properties from the base-most class
	 * first, then properties from the class immediately derived from the
	 * base-most class, and so on with the properties from this property set
	 * being last. This order gives the most intuitive order from the user's
	 * perspective. This order also ensures that a property in a base class has
	 * the same index in the returned order, regardless of the actual derived
	 * property set.
	 */
	public Collection<PropertyAccessor> getProperties3() {
		if (properties3 == null) {
			properties3 = new Vector<PropertyAccessor>();

			// Properties in this and all the base property sets
			ExtendablePropertySet<?> extendablePropertySet = this;
			do {
				int index= 0;
				for (PropertyAccessor propertyAccessor: extendablePropertySet.getProperties2()) {
					properties3.insertElementAt(propertyAccessor, index++);
				}
				extendablePropertySet = extendablePropertySet.getBasePropertySet();
			} while (extendablePropertySet != null);
		}

		return properties3;
	}

	/**
	 * Returns the same set of properties as the <code>getProperties3</code>
	 * method but the returned collection includes only the scalar properties
	 * (i.e. list properties are excluded).
	 */
	public Collection<ScalarPropertyAccessor> getScalarProperties3() {
		if (scalarProperties3 == null) {
			scalarProperties3 = new Vector<ScalarPropertyAccessor>();

			// Properties in this and all the base property sets
			ExtendablePropertySet<?> extendablePropertySet = this;
			do {
				int index= 0;
				for (ScalarPropertyAccessor propertyAccessor: extendablePropertySet.getScalarProperties2()) {
					scalarProperties3.insertElementAt(propertyAccessor, index++);
				}
				extendablePropertySet = extendablePropertySet.getBasePropertySet();
			} while (extendablePropertySet != null);
		}

		return scalarProperties3;
	}

	/**
	 * Returns the same set of properties as the <code>getProperties3</code>
	 * method but the returned collection includes only the list properties
	 * (i.e. scalar properties are excluded).
	 */
	public Collection<ListPropertyAccessor> getListProperties3() {
		if (listProperties3 == null) {
			listProperties3 = new Vector<ListPropertyAccessor>();

			// Properties in this and all the base property sets
			ExtendablePropertySet<?> extendablePropertySet = this;
			do {
				int index= 0;
				for (ListPropertyAccessor propertyAccessor: extendablePropertySet.getListProperties2()) {
					listProperties3.insertElementAt(propertyAccessor, index++);
				}
				extendablePropertySet = extendablePropertySet.getBasePropertySet();
			} while (extendablePropertySet != null);
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
	public Collection<ExtensionPropertySet<?>> getDirectExtensionPropertySets() {
		return extensionPropertySets.values();
	}
	
	/**
	 * Gets a list of all property sets that extend the given property set.
	 * Property sets that extend any base property sets are also included.
	 * 
	 * Note: This method does not return derived property sets.
	 * 
	 * @return the extension property sets that extend this property set
	 */
	public Collection<ExtensionPropertySet<?>> getExtensionPropertySets() {
		Collection<ExtensionPropertySet<?>> result = new ArrayList<ExtensionPropertySet<?>>();
	
		ExtendablePropertySet<?> propertySet = this;
		do {
			result.addAll(propertySet.extensionPropertySets.values());
			propertySet = propertySet.getBasePropertySet();
		} while (propertySet != null);
		
		return result;
	}
	
	/**
	 * This method should be used only by plug-ins that implement a datastore.
	 * 
	 * @param objectKey
	 *            key to the object, which cannot be null
	 * @param parentKey
	 *            key to the list that contains this object
	 * @param values
	 *            an implementation of the IValues interface which can provide
	 *            the values for all the scalar properties in the object
	 *            (ExtendableObjects, pojos, and intrinsic properties)
	 * @return A newly constructed ExtendableObject, constructed from the given
	 *         parameters
	 */
	public E constructImplementationObject(IObjectKey objectKey, ListKey<? super E> parentKey, IValues values) {
		return constructors.construct(objectKey, parentKey, values);
	}

	/**
	 * This method should be used only by plug-ins that implement
	 * a datastore.
	 * 
	 * @return A newly constructed object, constructed from the given
	 * 		parameters.  This object may be an ExtendableObject or
	 * 		may be an ExtensionObject.
	 */
	public E constructDefaultImplementationObject(IObjectKey objectKey, ListKey<? super E> parentKey) {
		return constructors.construct(objectKey, parentKey);
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
			PageEntry page = pageExtensions.get(i);
			if (newPage.getPosition() < page.getPosition()) {
				addIndex = i;
				break;
			}
		}
		pageExtensions.add(addIndex, newPage);
	}

	/**
	 * Utility method to find a property among all properties supported
	 * by objects of this class.
	 * 
	 * @param scalarPropertyId
	 * @return
	 */
	public ScalarPropertyAccessor getScalarProperty(String scalarPropertyId) {
		for (ScalarPropertyAccessor property: getScalarProperties3()) {
			if (property.getName().equals(scalarPropertyId)) {
				return property;
			}
		}
		return null;
	}

	/**
	 * Utility method to find a property among all properties supported
	 * by objects of this class.
	 * 
	 * @param listPropertyId
	 * @return
	 */
	public ListPropertyAccessor getListProperty(String listPropertyId) {
		for (ListPropertyAccessor property: getListProperties3()) {
			if (property.getName().equals(listPropertyId)) {
				return property;
			}
		}
		return null;
	}

	@Override
	protected E getImplementationObject(ExtendableObject extendableObject) {
		return classOfObject.cast(extendableObject);
	}
}
