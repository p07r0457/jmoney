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

import java.lang.reflect.*; // for introspection
import java.util.*; // for collections

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

/**
 * This class maintains the set of propagators that have
 * been added by the plug-ins.  This class provides methods
 * that may be called when a property value is changed that
 * ensure that all the appropriate propagator methods are called
 * to propagate the new property value.
 *
 * @author  Nigel
 */
public class Propagator {
	
	/**
	 * List of propagator objects.
	 * This may be temporary depending on the final design.
	 */
	private static Vector propagators = new Vector();
	
	/**
	 * Map of maps.  First key is the PropertySet object for the source,
	 * and second key is the PropertySet object for the destination.
	 */
	private static Map propagatorMap = new Hashtable();
	
	/**
	 * The set of all properties that have been updated as a result of a single
	 * update to a single property that has been propagated through propagators
	 * to the other properties.
	 *
	 * Null indicates that we are not currently in the process of firing propagators.
	 */
	private static Set updatedProperties = null;
	
	/**
	 * Loads the propagators.
	 * All propagators are added to the net.sf.jmoney.propagators extension point.
	 */
	public static void init() {
		
		// Load the list of available propagators.
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint("net.sf.jmoney.propagators");
		IExtension[] extensions = extensionPoint.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements =
				extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals("propagator")) {
					try {
						Object listener = elements[j].createExecutableExtension("class");
						propagators.add(listener);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		// Find all propagators for which both plugins are active.
		
		for (Iterator iter = propagators.iterator(); iter.hasNext(); ) {
			Object propagatorObject = iter.next();
			
			// Find all propertyChange methods.
			
			Method [] methods = propagatorObject.getClass().getMethods();
			for (int i = 0; i < methods.length; i++) {
				if (methods[i].getName().equals("propertyChange")) {
					if (methods[i].getReturnType() != void.class) {
						throw new MalformedPluginException("propertyChange methods in propagators must return void");
					}
					Class [] parameters = methods[i].getParameterTypes();
					if (parameters.length != 3) {
						throw new MalformedPluginException("propertyChange methods in propagators must have three parameters");
					}
					if (parameters[0] != String.class) {
						throw new MalformedPluginException("The first parameter in propertyChange methods in propagators must be of type String");
					}
					// Check that parameter one implements Entry interface
					/* Not needed - Entry extensions may not extend AbstractEntryExtension
					 Class [] interfaces1 = parameters[1].getInterfaces();
					 boolean interfaceOk = false;
					 for (int j = 0; j < interfaces1.length; j++) {
					 if (interfaces1[j].getName().equals("Entry")) {
					 interfaceOk = true;
					 break;
					 }
					 }
					 if (!interfaceOk) {
					 throw new MalformedPluginException("The first parameter in PropertyChange methods in propagators must be of type String");
					 }
					 */
					// If property set B is derived from property set A
					// then one may want to propagate values between extensions
					// to A and extensions to B, or between extensions to A
					// and properties in B itself.
					
					// We do not allow propagation between properties
					// in extensions and properties in the extendable class
					// which that extension is extending.  The reason being that
					// a propagator is not necessary because the plug-in providing 
					// the extension must know of the extendable class so can
					// more efficiently listen for property changes.
					
					for (Iterator extendableIter = PropertySet.getPropertySetIterator(); extendableIter.hasNext(); ) {
						PropertySet extendablePropertySet = (PropertySet)extendableIter.next();
						if (!extendablePropertySet.isExtension()) {
							
							CheckExtensions(extendablePropertySet, extendablePropertySet, methods[i]);
							
							for (PropertySet basePropertySet = extendablePropertySet.getBasePropertySet(); basePropertySet != null; basePropertySet = basePropertySet.getBasePropertySet()) {
								CheckExtensions(basePropertySet, extendablePropertySet, methods[i]);
								CheckExtensions(extendablePropertySet, basePropertySet, methods[i]);
								CheckPropertySetAgainstExtensions(extendablePropertySet, basePropertySet, methods[i]);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * @param method
	 */    
	private static void CheckExtensions(PropertySet extendablePropertySet1, PropertySet extendablePropertySet2, Method method) {
		for (Iterator sourceIter = PropertySet.getExtensionPropertySetIterator(extendablePropertySet1); sourceIter.hasNext(); ) {
			PropertySet sourcePropertySet = (PropertySet)sourceIter.next();
			CheckPropertySetAgainstExtensions(sourcePropertySet, extendablePropertySet2, method);
		}
	}
	
	/**
	 * Check propertySet1 itself against all of the extensions for propertySet2.
	 */
	private static void CheckPropertySetAgainstExtensions(PropertySet propertySet1, PropertySet extendablePropertySet2, Method method) {
		Class [] parameters = method.getParameterTypes();
		
		PropertySet sourcePropertySet = propertySet1;
		if (sourcePropertySet.getInterfaceClass().equals(parameters[1])) {
			
			for (Iterator destinationIter = PropertySet.getExtensionPropertySetIterator(extendablePropertySet2); destinationIter.hasNext(); ) {
				PropertySet destinationPropertySet = (PropertySet)destinationIter.next();
				
				if (destinationPropertySet.getInterfaceClass().equals(parameters[2])) {
					
					// Method looks ok so add to map.
					
					Map secondaryMap = (Map)propagatorMap.get(sourcePropertySet);
					if (secondaryMap == null) {
						secondaryMap = new Hashtable();
						propagatorMap.put(sourcePropertySet, secondaryMap);
					}
					
					Method methodFromMap = (Method)secondaryMap.get(destinationPropertySet);
					if (methodFromMap != null) {
						// We have two propagators for the same pair of property sets.
						// For the time being, consider this an error.
						// TODO this is not a plug-in error.  The plug-ins are just
						// incompatible.  We need a way of resolving this.
						Class sourceExtensionClass = parameters[1];
						Class destinationExtensionClass = parameters[2];
						throw new MalformedPluginException("There is more than one propagator from " + sourceExtensionClass.getName() + " to " + destinationExtensionClass.getName() + ".");
					}
					
					// (More than one property set may share the same extension class.  When this happens,
					// there are multiple instantiations of the extension in the extendable object.
					// each instantiation has separate data, but the propagators work for all so this
					// effectively forces the data to be the same for each property set.  This design
					// is more because it fits into the code rather than being a good design
					// for the plugin implementor.  It is not really expected that this will
					// happen).
					
					secondaryMap.put(destinationPropertySet, method);
				}
			}
		}
	}
	
	public static synchronized void fireAdaptors(IExtendableObject source, PropertyAccessor propertyAccessor) {
		// We must be very careful when firing propagators.  Propagators usually work two-way,
		// with updates to one property being reflected in another property and updates
		// to the other property being reflected in the first property.  There may be
		// multiple properties from multiple extensions there are all interconnected
		// by various propagators in a web of unlimited complexity.
		
		// Propagators must be fired recursively, so if an propagator propagates a change
		// from property A to property B and another propagator propagates a change
		// from property B to property C then a change to property A must update
		// property C.  With propagators being provided from multiple sources, this
		// is likely to result in infinite recursion.
		
		// The solution is that we keep a set of all properties that have been updated
		// by propagators due to a single original change to a property.  As soon as
		// an attempt is made to update a property for the second time, we either
		// stop the recursion there if the new value is the same as the old value,
		// or we give an error if the new value is different from the old value.
		
		// This method is synchronized because otherwise two simultaneous updates
		// may interfere.
		
		boolean topLevelPropagatorFirer;
		
		if (updatedProperties == null) {
			updatedProperties = new HashSet();
			topLevelPropagatorFirer = true;
		} else {
			topLevelPropagatorFirer = false;
		}
		
		// 'updatedProperties' must be reset to null even in the case where
		// an exception is throw.  We therefore wrap all the following in a
		// 'try' - 'finally' block.
		try {
		
		PropertySet sourcePropertySetKey = propertyAccessor.getPropertySet();
		
		if (updatedProperties.contains(propertyAccessor)) {
			// Actually should not be a runtime exception
			throw new InconsistentCircularPropagatorsException(propertyAccessor, source.getPropertyValue(propertyAccessor));
		}
		
		// Add this property to the map of properties that have been changed.
		updatedProperties.add(propertyAccessor);
		
		Map secondaryMap = (Map)propagatorMap.get(sourcePropertySetKey);
		if (secondaryMap != null) {
			for (Iterator iter = secondaryMap.entrySet().iterator(); iter.hasNext(); ) {
				Map.Entry mapEntry = (Map.Entry)iter.next();
				PropertySet destinationPropertySetKey = (PropertySet)mapEntry.getKey();
				Method method = (Method)mapEntry.getValue();
				
				Object[] parameters = new Object[3];
				parameters[0] = propertyAccessor.getLocalName();
				parameters[1] = source.getExtension(sourcePropertySetKey);
				parameters[2] = source.getExtension(destinationPropertySetKey);
				try {
					// Kludge.  When properties are being loaded from file, the objects
					// are not the mutable versions.  Therefore when we obtain the
					// destination extension, it may be null.  We do nothing in this
					// case, as the datastore should have consistent values already.
					// TODO tidy this up when datastore are properly implemented.
					if (parameters[2] != null) {
						method.invoke(null, parameters);
					}
				} catch (IllegalAccessException e) {
					// Should not happen because only public methods are added to our list.
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					if (e.getCause() == null) {
						// TODO: Figure out what to do here
						e.printStackTrace();
					}
					if (e.getCause() instanceof InconsistentCircularPropagatorsException) {
						InconsistentCircularPropagatorsException exception = (InconsistentCircularPropagatorsException)e.getCause();
						throw new InconsistentCircularPropagatorsException(propertyAccessor, source.getPropertyValue(propertyAccessor), exception);  
					} else {
						// TODO: Figure out what to do here
						e.printStackTrace();
					}
				}
			}
		}
		
		} finally {
		if (topLevelPropagatorFirer) {
			updatedProperties = null;
		}
		}
	}
}