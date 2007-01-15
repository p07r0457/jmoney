/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2007 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.oda.driver;

import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.PropertySetNotFoundException;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.ui.IMemento;

/**
 * This class implements a fetcher that fetches rows/objects
 * from a list of objects as represented by an element
 * in the query tree.
 * 
 * Objects of this class fetch objects from a list property, where the
 * parent of the list property is a list of parents (though
 * the list will be a list of 1 if the parent is the session
 * object).
 * 
 * @author Nigel Westbury
 *
 */
public class ListFetcher implements IFetcher {

	private ListPropertyAccessor<?> listProperty;
	private Vector<ScalarPropertyAccessor<?>> columnProperties = new Vector<ScalarPropertyAccessor<?>>();
	
	/*
	 * If the list is to be filtered to include only objects of
	 * a particular class, the property set for that class;
	 * otherwise null
	 */
	private ExtendablePropertySet<?> filter = null;
	
	private Iterator<? extends ExtendableObject> iterator;
	private ExtendableObject currentObject;
	
	private IFetcher parentObjects;
	
	/**
	 * 
	 * @param memento
	 * @throws OdaException
	 */
	public ListFetcher(IMemento memento) throws OdaException {
		String listPropertyId = memento.getString("listId");
		if (listPropertyId == null) {
			throw new OdaException("no listId");
		}

		// Set the parent list
		IMemento childMemento = memento.getChild("listProperty");
		if (childMemento != null) {
			parentObjects = new ListFetcher(childMemento);
		} else {
			childMemento = memento.getChild("parameter");
			if (childMemento != null) {
				parentObjects = new ParameterFetcher(childMemento);
			} else {
				parentObjects = new SessionFetcher();
			}
		}

		ExtendablePropertySet parentPropertySet = parentObjects.getPropertySet();
		listProperty = parentPropertySet.getListProperty(listPropertyId);
		if (listProperty == null) {
			throw new OdaException("No list property '" + listPropertyId + "' found in property set '" + parentPropertySet.getId() + "'.");
		}
		
		/*
		 * Set the filter, if any
		 */
		String filterPropertySetId = memento.getString("filter");
		if (filterPropertySetId != null) {
			try {
				filter = PropertySet.getExtendablePropertySet(filterPropertySetId);
			} catch (PropertySetNotFoundException e) {
				throw new OdaException("Property set " + filterPropertySetId + " (used as a filter) does not exist.");
			}
		} else {
			filter = listProperty.getElementPropertySet();
		}
		
		
/* Just include all properties for time being		
		for (IMemento childMemento: memento.getChildren("column")) {
			String id = childMemento.getString("name");
			ScalarPropertyAccessor property = parentPropertySet.getScalarProperty(id);
			columnProperties.add(property);
		}
*/
		for (ScalarPropertyAccessor property: filter.getScalarProperties3()) {
			columnProperties.add(property);
		}
	}
	
	public void reset() {
		parentObjects.reset();
		iterator = null;
	}

	public boolean next() {
		if (iterator == null) {
			// We are positioned before the first row
			boolean isAnother = parentObjects.next();
			if (!isAnother) {
				return false;
			}
			iterator = parentObjects.getCurrentObject().getListPropertyValue(listProperty).iterator();
		}
		
		/*
		 * The assumption is that the caller of this method
		 * will get all the objects at once on the SWT thread,
		 * thus solving the problem of the need for us to
		 * take a snapshot copy to protect us from concurrent
		 * modifications.
		 * 
		 * If that is not the case (perhaps a large report will
		 * fetch data as the user scrolls down) then we would need
		 * to take a copy.  We would also have to listen for changes
		 * so that we can update this copy (necessary because if an
		 * object is deleted from the model, we cannot get rely on
		 * getting properties from that object even if we kept a reference
		 * to the object).
		 */
		do {
			while (iterator.hasNext()) {
				currentObject = iterator.next();
				if (filter.getImplementationClass().isAssignableFrom(currentObject.getClass())) {
					return true;
				}
			}
			boolean isAnother = parentObjects.next();
			if (!isAnother) {
				break;
			}
			iterator = parentObjects.getCurrentObject().getListPropertyValue(listProperty).iterator();
		} while (true);
		return false;
	}
	
	public Object getValue(int columnIndex) throws OdaException {
		if (columnIndex < columnProperties.size()) {
			return currentObject.getPropertyValue(columnProperties.get(columnIndex));
		} else {
			return parentObjects.getValue(columnIndex - columnProperties.size());
		}
	}

	public ExtendableObject getCurrentObject() {
		return currentObject;
	}

	public void addSelectedProperties(Vector<ScalarPropertyAccessor> selectedProperties) {
		for (ScalarPropertyAccessor property: this.columnProperties) {
			selectedProperties.add(property);
		}
		
		parentObjects.addSelectedProperties(selectedProperties);
	}

	public ExtendablePropertySet getPropertySet() {
		return listProperty.getElementPropertySet();
	}

	public void addParameters(Vector<ParameterData> parameters) {
		// This object does not directly use any parameters,
		// but the fetcher used by this object might.
		parentObjects.addParameters(parameters);
	}
}