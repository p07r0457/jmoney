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

import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.SessionInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.datatools.connectivity.oda.OdaException;

/**
 * This class implements a fetcher that fetches the session
 * object.  There is only a single session object, and it is
 * the root of the data model, thus this object represents
 * a rowset that always contains a single row.  The first call to next() will position the cursor
 * on the session object, the second call will return false.
 * 
 * @author Nigel Westbury
 *
 */
public class SessionFetcher implements IFetcher {

	private Vector<ScalarPropertyAccessor<?>> columnProperties = new Vector<ScalarPropertyAccessor<?>>();
	
	private boolean onSessionObject;
	
	public void reset() {
		onSessionObject = false;
	}

	public boolean next() {
		if (!onSessionObject) {
			onSessionObject = true;
			return true;
		} else {
			return false;
		}
	}
	
	public Object getValue(int columnIndex) throws OdaException {
		if (columnIndex < columnProperties.size()) {
			return JMoneyPlugin.getDefault().getSession().getPropertyValue(columnProperties.get(columnIndex));
		} else {
			throw new OdaException("Column index is too big");
		}
	}

	public ExtendableObject getCurrentObject() {
		return JMoneyPlugin.getDefault().getSession();
	}

	public void addSelectedProperties(Vector<ScalarPropertyAccessor> selectedProperties) {
		for (ScalarPropertyAccessor property: this.columnProperties) {
			selectedProperties.add(property);
		}
	}

	public ExtendablePropertySet getPropertySet() {
		return SessionInfo.getPropertySet();
	}

	public void addParameters(Vector<ParameterData> parameters) {
		// This object does not use any parameters
	}
}
