/*
 * Created on Oct 1, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.jmoney.model2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * Keeps track of changes made to the model.
 * 
 * There are two purposes for keeping track of changes:
 * <UL>
 * <LI>Support for the undo/redo feature</LI>
 * <LI>Changes to any underlying database can be made more efficient
 * 		by combining changes.  For example, if an object is created
 * 		and then properties are set, and a SQL database is used for storage,
 * 		what would be an insert and multiple updates can be merged into
 * 		a single insert.</LI>
 * <UL>
 * 
 * This class solves the first of the two above problems only.
 * <P>
 * As changes are undone and redone, the id of each object may change.
 * For example, in the serializeddatastore plug-in, the id of each object
 * is a reference to the object itself, i.e. the java identity.  Unless
 * we keep a reference to these objects, which we don't, the identity of
 * objects will not be the same when the object is re-created.  (Even if
 * we kept a reference to an object, it is not possible to insert that
 * object back into the object store for various technical reasons).
 * If the datastore is a database, for example in the jdbcdatastore plug-in,
 * the id is automatically generated as a value of a unique column.
 * The database may decide to re-use the id of a delete row.
 * Therefore, this class never stores ids of objects that have been
 * deleted.  When an object is deleted, all old values that reference
 * the object are replaced with references to the delete entry.
 * This allows the data to be re-created correctly on both undo and
 * redo operations.
 * <P>
 * The data in this object is changed as the changes are undone and
 * redone.  This is because ids are updated to reflect the ids of each
 * object given to it by the datastore.
 * <P>
 * The changes are made to the database by this class as changes are
 * passed to this class.  Although it may be more efficient to execute
 * the changes in the database when they are committed, this would result
 * in changes not being reflected in query statements that are submitted
 * after the changes were made by the plug-in but before the plug-in
 * committed the changes.
 * 
 *  @author Nigel Westbury
 *
 */
public class ChangeManager {

	ISessionManagement sessionManager;
	
	/**
	 * Maps IExtendableObject to ChangeEntry
	 */
	Map objects = new HashMap();	
	
	/**
	 * Contains the set of property values for an object.
	 * 
	 * This may contain the old values of an object before
	 * a change or the new values after a change.  If an
	 * object is being inserted then a complete set of all
	 * new values is stored, including all the extension
	 * properties except where an extension property set
	 * contains entirely default values.  Likewise when
	 * an object is being deleted then a complete set of
	 * old values is stored.
	 */
	class PropertyValues {
		Object [] values;
		Map extensionValues;  // PropertySet to Object[]
	}
	
	class ChangeEntry {
		int op; // 0 = update, 1 = insert, 2 = delete
		int id;
		PropertyValues oldValues;
		PropertyValues newValues;
	}
	
	public ChangeManager(ISessionManagement sessionManager) {
		this.sessionManager = sessionManager;
	}
	
	public void setProperty(
		ExtendableObjectHelperImpl object,
		PropertyAccessor propertyAccessor,
		Object oldValue,
		Object newValue) {
		
		if (oldValue == newValue ||
			(oldValue != null && oldValue.equals(newValue)))
				return;
		
		IObjectKey key = object.getObjectKey();
		
		// Build two arrays of old and new values.
		// Ultimately we will have a layer between that does this
		// for us, also combining multiple updates to the same row
		// into a single update.  Until then, we need this code here.
		{
			PropertySet propertySet = PropertySet.getPropertySet(object.getClass());
			Vector constructorProperties = propertySet.getConstructorProperties();

			// TODO: improve performance here.
			int count = 0;
			for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); ) {
				PropertyAccessor propertyAccessor2 = (PropertyAccessor)iter.next();
				if (propertyAccessor2.isScalar()) {
					count++;
				}
			}
			
			PropertyValues oldValues = new PropertyValues();
			PropertyValues newValues = new PropertyValues();
			
			oldValues.values = new Object[count];
			newValues.values = new Object[count];
			
			int i = 0;
			for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); ) {
				PropertyAccessor propertyAccessor2 = (PropertyAccessor)iter.next();
				if (propertyAccessor2.isScalar()) {
					if (propertyAccessor2 == propertyAccessor) {
					oldValues.values[i] = oldValue;
					newValues.values[i] = newValue;
					} else {
					oldValues.values[i] = propertyAccessor2.getValue(object);
					newValues.values[i] = propertyAccessor2.getValue(object);
					}
					i++;
				}
			}
			key.updateProperties(propertySet, oldValues.values, newValues.values, null);
		}
		
		// Lookup object in our object map.
		ChangeEntry changeEntry = (ChangeEntry)objects.get(object);
		
		PropertySet propertySet = PropertySet.getPropertySet(object.getClass());
		Vector constructorProperties = propertySet.getConstructorProperties();

		// If not found, add it to map.
		if (changeEntry == null) {
			changeEntry = new ChangeEntry();
			changeEntry.oldValues = new PropertyValues();
			changeEntry.newValues = new PropertyValues();
			
			// TODO: improve performance here.
			int count = 0;
			for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); ) {
				PropertyAccessor propertyAccessor2 = (PropertyAccessor)iter.next();
				if (propertyAccessor2.isScalar()) {
					count++;
				}
			}
			
			changeEntry.oldValues.values = new Object[count];
			changeEntry.newValues.values = new Object[count];
			
			int i = 0;
			for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); ) {
				PropertyAccessor propertyAccessor2 = (PropertyAccessor)iter.next();
				if (propertyAccessor2.isScalar()) {
					if (propertyAccessor2 == propertyAccessor) {
					changeEntry.oldValues.values[i] = oldValue;
					changeEntry.newValues.values[i] = newValue;
					} else {
					changeEntry.oldValues.values[i] = propertyAccessor2.getValue(object);
					changeEntry.newValues.values[i] = propertyAccessor2.getValue(object);
					}
					i++;
				}
			}
			
			objects.put(object, changeEntry);
		}
		
		// Update the new property in this map.
		int i = 0;
		for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor2 = (PropertyAccessor)iter.next();
			if (propertyAccessor2.isScalar()) {
				if (propertyAccessor2 == propertyAccessor) {
					changeEntry.newValues.values[i] = newValue;
				}
				i++;
			}
		}

		// Fire an event for this change.
        sessionManager.getSession().objectChanged(
        		object,
        		propertyAccessor,
				oldValue,
				newValue);
	}
	
	public void createObject(
			PropertySet propertySet,
			ExtendableObjectHelperImpl object,
			Object newValue) {

		// Add object and its values to the map.
			
	}
		
	public void deleteObject(
			PropertySet propertySet,
			ExtendableObjectHelperImpl object) {

		// Lookup object in our object map.
		
		// If not found, add it to map.

		// When we delete an object, we know that nothing in the
		// datastore references it.  However, there may be old
		// values that referenced it.  It is important that these
		// old values are updated to reference this deleted object.
		// Otherwise, if the object is re-created with a different
		// id then those old values cannot be restored correctly.
		
		// Set map entry to indicate that object is deleted.
	}
		
	/**
	 * Submit a series of updates, which have been stored,
	 * to the database.
	 */
	public void applyChanges(String desc) {
		/* actually nothing to do here.
		for (Iterator objectIter = objects.entrySet().iterator(); objectIter.hasNext(); ) {
			Map.Entry entry = (Map.Entry)objectIter.next();
			ExtendableObjectHelperImpl extendableObject = (ExtendableObjectHelperImpl)entry.getKey();
			ChangeEntry changeEntry = (ChangeEntry)entry.getValue();
			
			// TODO: build extensionValues from changeEntry.newValues.extensionValues
			ExtensionProperties [] extensionValues = null;
			
			IObjectKey key = extendableObject.getObjectKey();
			key.updateProperties(changeEntry.oldValues.values, changeEntry.newValues.values, extensionValues);
		}
		*/
	}

	/**
	 * Submit a series of updates, which have been stored,
	 * to the database.  These updates carry out the reverse
	 * of the updates stored.
	 * 
	 * Also make the same reverse changes to the memory objects.
	 */
	public void undoChanges() {
	}

	/**
	 * Submit a series of updates, which have been stored,
	 * to the database.
	 *
	 * Also make the same changes to the memory objects.
	 */
	public void redoChanges() {
	}

}
