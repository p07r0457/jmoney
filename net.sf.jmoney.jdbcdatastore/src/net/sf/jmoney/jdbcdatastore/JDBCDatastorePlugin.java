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

package net.sf.jmoney.jdbcdatastore;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IListManager;
import net.sf.jmoney.model2.IObjectKey;
import net.sf.jmoney.model2.MalformedPluginException;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertyNotFoundException;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.PropertySetNotFoundException;
import net.sf.jmoney.model2.Session;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.*;
import org.osgi.framework.BundleContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * The main plugin class to be used in the desktop.
 *
 * @author Nigel Westbury
 */
public class JDBCDatastorePlugin extends AbstractUIPlugin {
	//The shared instance.
	private static JDBCDatastorePlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	
	private class ParentList {
		ParentList(PropertySet parentPropertySet, PropertyAccessor listProperty) {
			this.parentPropertySet = parentPropertySet;
			this.columnName = listProperty.getName().replace('.', '_');
		}
		
		PropertySet parentPropertySet;
		String columnName;
	}
	
	/**
	 * A map of PropertySet objects to non-null Vector objects.
	 * Each Vector object is a list of ParentList objects.
	 * An entry exists in this map for all property sets that are
	 * not extension property sets.  For each property set, the
	 * vector contains a list of the list properties in all property
	 * sets that contain a list of objects of the property set.
	 */
	private static Map tablesMap = null;
	
	/**
	 * The constructor.
	 */
	public JDBCDatastorePlugin() {
		super();
		plugin = this;
		try {
			resourceBundle = ResourceBundle.getBundle("net.sf.jmoney.jdbcdatastore.Language");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}
	
	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}
	
	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}
	
	/**
	 * Returns the shared instance.
	 */
	public static JDBCDatastorePlugin getDefault() {
		return plugin;
	}
	
	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = JDBCDatastorePlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}
	
	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	
    // Preferences
    
	/** 
	 * Initializes a preference store with default preference values 
	 * for this plug-in.
	 */
	protected void initializeDefaultPreferences(IPreferenceStore store) {
    	store.setDefault("promptEachTime", true);
		store.setDefault("driverOption", "Other");
		store.setDefault("driver", "org.hsqldb.jdbcDriver");
		store.setDefault("subProtocol", "hsqldb");
		store.setDefault("subProtocolData", "hsql://localhost/accounts");
		store.setDefault("user", "sa");
		store.setDefault("password", "");
	}
	
	/**
	 * @param window
	 * @return
	 */
	public SessionManager readSession(IWorkbenchWindow window) {
		SessionManager result = null;
		
    	// The following lines cannot return a null value because if
    	// no value is set then the default value set in
    	// the above initializeDefaultPreferences method will be returned.
		String driver = getPreferenceStore().getString("driver");
		String subprotocol = getPreferenceStore().getString("subProtocol");
		String subprotocolData = getPreferenceStore().getString("subProtocolData");
		
		String url = "jdbc:" + subprotocol + ":" + subprotocolData;
		
		String user = getPreferenceStore().getString("user");
		String password = getPreferenceStore().getString("password");;
		
		if (getPreferenceStore().getBoolean("promptEachTime")) {
			// TODO: Put up a dialog box so the user can change
			// the connection options for this connection only.
		}
		
		try {
			Class.forName(driver).newInstance();
		} catch (InstantiationException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IllegalAccessException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (ClassNotFoundException e2) {
			String title = JDBCDatastorePlugin.getResourceString("errorTitle");
			String message = JDBCDatastorePlugin.getResourceString("driverNotFound");
			MessageDialog waitDialog =
				new MessageDialog(
						window.getShell(), 
						title, 
						null, // accept the default window icon
						message, 
						MessageDialog.ERROR, 
						new String[] { IDialogConstants.OK_LABEL }, 0);
			waitDialog.open();
			return null;
		}
		
		try {
			Connection con = DriverManager.getConnection(url, user, password);	      
			
			result = initGeneralized(con);
			
		} catch (SQLException e3) {
			if (e3.getSQLState().equals("08000")) {
				// A connection error which means the database server is probably not running.
				String title = JDBCDatastorePlugin.getResourceString("errorTitle");
				String message = JDBCDatastorePlugin.getResourceString("connectionFailed");
				MessageDialog waitDialog =
					new MessageDialog(
							window.getShell(), 
							title, 
							null, // accept the default window icon
							message, 
							MessageDialog.ERROR, 
							new String[] { IDialogConstants.OK_LABEL }, 0);
				waitDialog.open();
			} else {
				e3.printStackTrace();
				throw new RuntimeException(e3.getMessage());
			}
		}
		
		return result;
	}
	
	
	SessionManager initGeneralized(Connection con) throws SQLException {
		PropertySet commodityPropertySet = JMoneyPlugin.getCommodityPropertySet();
		PropertySet currencyPropertySet = JMoneyPlugin.getCurrencyPropertySet();
		PropertySet accountPropertySet = JMoneyPlugin.getAccountPropertySet();
		PropertySet capitalPropertySet = JMoneyPlugin.getCapitalAccountPropertySet();
		PropertySet incomeExpensePropertySet = JMoneyPlugin.getIncomeExpenseAccountPropertySet();
		
		SessionKey sessionKey = new SessionKey();
		
		ResultSet rs;	
		
		DatabaseMetaData dmd = con.getMetaData();
		System.out.println("Connected to: " + dmd.getURL() + "\n" +
				"Driver: " + dmd.getDriverName() + "\n" +
				"Version: " + dmd.getDriverVersion());
		
		SessionManager sessionManager = new SessionManager(con, sessionKey);

		sessionManager.addCachedPropertySet(commodityPropertySet);
		sessionManager.addCachedPropertySet(accountPropertySet);
		
		Map commodityMap = sessionManager.getMapOfCachedObjects(commodityPropertySet);
		Map accountsMap = sessionManager.getMapOfCachedObjects(accountPropertySet);

		// Find all properties in any property set that are
		// a list of objects with the type as this property set.
		// A column must exist in this table for each such property
		// that exists in another property set.
		tablesMap = new Hashtable();
		for (Iterator iter = PropertySet.getPropertySetIterator(); iter.hasNext(); ) {
			PropertySet propertySet = (PropertySet)iter.next();
			if (!propertySet.isExtension()) {
				Vector list = new Vector();  // List of PropertyAccessors
				for (Iterator iter2 = PropertySet.getPropertySetIterator(); iter2.hasNext(); ) {
					PropertySet propertySet2 = (PropertySet)iter2.next();
					if (!propertySet2.isExtension()) {
						for (Iterator iter3 = propertySet2.getPropertyIterator2(); iter3.hasNext(); ) {
							PropertyAccessor propertyAccessor = (PropertyAccessor)iter3.next();
							if (propertyAccessor.isList()
							 && propertySet.getImplementationClass() == propertyAccessor.getValueClass()) {
								// Add to the list of possible parents.
								list.add(new ParentList(propertySet2, propertyAccessor));
							}
						}
					}
				}
				tablesMap.put(propertySet, list);
			}
		}
		
		// Create a statement for our use.
		Statement stmt = con.createStatement();

		// Check that all the required tables and columns exist.
		// Any missing tables or columns are created at this time.
		checkDatabase(con, stmt);
		
		PropertyAccessor commodityListProperty;
		PropertyAccessor accountListProperty;
		PropertyAccessor transactionListProperty;
		try {
			commodityListProperty = PropertySet.getPropertyAccessor("net.sf.jmoney.session.commodity");
			accountListProperty = PropertySet.getPropertyAccessor("net.sf.jmoney.session.account");
			transactionListProperty = PropertySet.getPropertyAccessor("net.sf.jmoney.session.transaction");
		} catch (PropertyNotFoundException e) {
			throw new RuntimeException("internal error");
		}
		ListManagerCached commodityListManager = new ListManagerCached(sessionManager, commodityListProperty);
		ListManagerCached accountListManager = new ListManagerCached(sessionManager, accountListProperty);
		
		// Fetch the currencies
		rs = stmt.executeQuery("SELECT * FROM net_sf_jmoney_currency JOIN net_sf_jmoney_commodity ON net_sf_jmoney_currency._ID = net_sf_jmoney_commodity._ID");

		while (rs.next()) {
			ExtendableObject commodityObject = materializeObjectCached(rs, currencyPropertySet, sessionKey, sessionManager);
			
			commodityListManager.add(commodityObject);
			
			int currencyId = rs.getInt(1);  // _ID
			commodityMap.put(new Integer(currencyId), commodityObject);
		}
		rs.close();
		
		// Fetch the accounts
		
		// Fetch the capital accounts
		rs = stmt.executeQuery("SELECT * FROM net_sf_jmoney_capitalAccount JOIN net_sf_jmoney_account ON net_sf_jmoney_capitalAccount._ID = net_sf_jmoney_account._ID");
		while (rs.next()) {
			ExtendableObject accountObject = materializeObjectCached(rs, capitalPropertySet, sessionKey, sessionManager);
				
			accountListManager.add(accountObject);
			
			int accountId = rs.getInt(1);  // _ID
			accountsMap.put(new Integer(accountId), accountObject);
		}
		rs.close();

		// Fetch the income and expense accounts
		rs = stmt.executeQuery("SELECT * FROM net_sf_jmoney_categoryAccount JOIN net_sf_jmoney_account ON net_sf_jmoney_categoryAccount._ID = net_sf_jmoney_account._ID");
		while (rs.next()) {
			ExtendableObject accountObject = materializeObjectCached(rs, incomeExpensePropertySet, sessionKey, sessionManager);
				
			accountListManager.add(accountObject);
			
			int accountId = rs.getInt(1);  // _ID
			accountsMap.put(new Integer(accountId), accountObject);
		}
		rs.close();

		stmt.close();
		
		// Create the transaction list manager.
		// Note that we do not read all the transactions.
		// We create an object that implements the IListManager
		// interface (an extension of the Collection interface)
		// and that returns transactions when requested.
		
		IListManager transactionListManager = new ListManagerUncached(sessionKey, sessionManager, transactionListProperty);
		
		// Create the session object.
		
		Session newSession = new Session(
				sessionKey,
				null,
				null,
				commodityListManager,
				accountListManager,
				transactionListManager,
				null
		);
		sessionKey.setObject(newSession, sessionManager);
		
		return sessionManager;
	}

	private class ColumnInfo {
		String columnName;
		String columnDefinition;
		PropertySet foreignKeyPropertySet = null;
	}
	
	/**
	 * Build a list of columns that we must have in the table that
	 * holds the data for a particular property set.
	 * <P>
	 * The list will depend on the set of installed plug-ins.
	 * <P>
	 * The "_ID" column is required in all tables as a primary
	 * key and is not returned by this method.
	 * <P>
	 * This method may not be called with an extension property set.
	 * 
	 * @return A Vector containing objects of class 
	 * 		<code>ColumnInfo</code>.
	 */
	private Vector buildColumnList(PropertySet propertySet) {
		Vector result = new Vector();
		
		// The parent column requirements depend on which other
		// property sets have lists.  A parent column exists in the
		// table for property set A for each other property set that
		// contains of list of objects of type A.
		//
		// If there is a single place where the property set is listed
		// and that place is in the session object (or an extension thereof)
		// then no parent column is necessary because there is only one
		// session object.
		
		// If there is a single place where the property set is listed
		// and that place is not in the session object (or an extension thereof)
		// then a parent column is created with the name being the same as the
		// fully qualified name of the property that lists these objects.
		// The column will not allow null values.
		
		// If there are multiple places where a property set is listed
		// then a column is created for each place (but if one of the places
		// is the session object that no column is created for that place).
		// The columns will allow null values.  At most one of the columns
		// may be non-null.  If all the columns are null then the parent is
		// the session object.  The names of the columns are the fully qualified
		// names of the properties that list these objects.
		
		String tableName = propertySet.getId().replace('.', '_');
		
		Vector list = (Vector)tablesMap.get(propertySet);
		
		// Find all properties in any property set that are
		// a list of objects with the type as this property set.
		// A column must exist in this table for each such property
		// that exists in another property set.
		for (Iterator iter2 = list.iterator(); iter2.hasNext(); ) {
			ParentList parentList = (ParentList)iter2.next();

			ColumnInfo info = new ColumnInfo();
			info.columnName = parentList.columnName;
			
			info.columnDefinition = "INT NULL";
			result.add(info);
		}
		
		// The columns for each property in this property set
		// (including the extension property sets).
		for (Iterator iter = propertySet.getPropertyIterator2(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
			if (propertyAccessor.isScalar()) {
				ColumnInfo info = new ColumnInfo();
				
				if (propertyAccessor.getPropertySet().isExtension()) {
					info.columnName = propertyAccessor.getName().replace('.', '_');
				} else {
					info.columnName = propertyAccessor.getLocalName();
				}
				
				boolean nullable = true;
				Class valueClass = propertyAccessor.getValueClass();
				if (valueClass == Integer.class) {
					info.columnDefinition = "INT";
					nullable = true;
				} else if (valueClass == int.class) {
					info.columnDefinition = "INT";
					nullable = false;
				} if (valueClass == Long.class) {
					info.columnDefinition = "BIGINT";
					nullable = true;
				} else if (valueClass == long.class) {
					info.columnDefinition = "BIGINT";
					nullable = false;
				} else if (valueClass == Character.class) {
					info.columnDefinition = "CHAR";
					nullable = true;
				} else if (valueClass == char.class) {
					info.columnDefinition = "CHAR";
					nullable = false;
				} else if (valueClass == boolean.class) {
					info.columnDefinition = "BIT";
					nullable = false;
				} else if (valueClass == String.class) {
					info.columnDefinition = "VARCHAR";
					nullable = true;
				} else if (valueClass == Boolean.class) {
					info.columnDefinition = "BIT";
					nullable = true;
				} else if (valueClass == Date.class) {
					info.columnDefinition = "DATE";
					nullable = true;
				} else if (ExtendableObject.class.isAssignableFrom(valueClass)) {
					info.columnDefinition = "INT";
					nullable = true;
					
					// This call does not work.  The method works only when the class
					// is a class of an actual object and only non-derivable property
					// sets are returned.
					// info.foreignKeyPropertySet = PropertySet.getPropertySet(valueClass);
					
					// This works.
					// The return type from a getter for a property that is a reference
					// to an extendable object must be the getter interface.
					info.foreignKeyPropertySet = null;
					for (Iterator iter2 = PropertySet.getPropertySetIterator(); iter2.hasNext(); ) {
						PropertySet propertySet2 = (PropertySet)iter2.next();
						if (propertySet2.getImplementationClass() == valueClass) {
							info.foreignKeyPropertySet = propertySet2;
							break;
						}
					}
				} else { 
					// All other types are stored as a string by 
					// using the String constructor and
					// the toString method for conversion.
					info.columnDefinition = "VARCHAR";
					nullable = true;
				}
				
				// If the property is an extension property then we set
				// a default value.  This saves us from having to set default
				// value in every insert statement and is a better solution
				// if other applications (outside JMoney) access the database.
					
				if (propertyAccessor.getPropertySet().isExtension()) {
					// TODO: fix the following line.
					// It currently assumes only one property per extension
					// property set.
					Object defaultValue = propertyAccessor.getPropertySet().getDefaultPropertyValues2()[0];
					info.columnDefinition +=
						" DEFAULT " + valueToSQLText(defaultValue);
				}
				
				if (nullable) {
					info.columnDefinition += " NULL";
				} else {
					info.columnDefinition += " NOT NULL";
				}
				result.add(info);
			}
		}
		
		// If the property set is a derivable property set
		// and is the base-most property set
		// then we must have a column called _PROPERTY_SET.
		// This column contains the id of the actual 
		// (non-derivable) property set of this object.
		// This column is required because otherwise we would not
		// know which further tables need to be joined to get the
		// complete set of properties with which we can construct
		// the object.
		if (propertySet.getBasePropertySet() == null
		 && propertySet.isDerivable()) {
			ColumnInfo info = new ColumnInfo();
			info.columnName = "_PROPERTY_SET";
			info.columnDefinition = "VARCHAR NOT NULL";
			result.add(info);
		}
		
		return result;
	}
	
	private static String[] tableOnlyType = new String[] { "TABLE" };

	private void traceResultSetLabels(ResultSet rs) {
		try {
			String x = "";		
			ResultSetMetaData rsmd = rs.getMetaData();
			int cols = rsmd.getColumnCount();
			for (int i = 1; i <= cols; i++) {
				x += rsmd.getColumnLabel(i) + ", ";
			}
			System.out.println(x);
			
		} catch (Exception SQLException) {
			throw new RuntimeException("database error");
		}
		System.out.println("");
	}

	private void traceResultSet(ResultSet rs) {
		try {
			String x = "";		
			ResultSetMetaData rsmd = rs.getMetaData();
			int cols = rsmd.getColumnCount();
			for (int i = 1; i <= cols; i++) {
				x += rsmd.getColumnLabel(i) + ", ";
			}
			System.out.println(x);
			
			while (rs.next()) {
				x = "";
				for (int i = 1; i <= cols; i++) {
					x += rs.getString(i) + ", ";
				}
				System.out.println(x);
			}
		} catch (Exception SQLException) {
			throw new RuntimeException("database error");
		}
		System.out.println("");
	}

	/**
	 * Check the tables and columns in the database.
	 * If a required table does not exist it will be created.
	 * If a table exists but it does not contain all the required
	 * columns, then the required columns will be added to the table.
	 * <P>
	 * There may be additional tables and there may be
	 * additional columns in the required tables.  These are
	 * ignored and the data in them are left alone.
	 */
	private void checkDatabase(Connection con, Statement stmt) throws SQLException {
		
		DatabaseMetaData dmd = con.getMetaData();
		
		for (Iterator iter = PropertySet.getPropertySetIterator(); iter.hasNext(); ) {
			PropertySet propertySet = (PropertySet)iter.next();
			if (!propertySet.isExtension()) {
				String tableName = propertySet.getId().replace('.', '_');
				
				// Check that the table exists.
				ResultSet tableResultSet = dmd.getTables(null, null, tableName.toUpperCase(), tableOnlyType);
				
				if (tableResultSet.next()) {
					Vector columnInfos = buildColumnList(propertySet);
					
					for (Iterator iter2 = columnInfos.iterator(); iter2.hasNext(); ) {
						ColumnInfo columnInfo = (ColumnInfo)iter2.next();
						
						ResultSet columnResultSet = dmd.getColumns(null, null, tableName.toUpperCase(), columnInfo.columnName);
						if (columnResultSet.next()) {
							int dataType = columnResultSet.getInt("DATA_TYPE");
							String typeName = columnResultSet.getString("TYPE_NAME");
							// TODO: Check that the column information is
							// correct.  Display a fatal error if it is not.
						} else {
							// The column does not exist so we add it.
							String sql = 
								"ALTER TABLE " + tableName
								+ " ADD \"" + columnInfo.columnName
								+ "\" " + columnInfo.columnDefinition;
							stmt.executeUpdate(sql);	
						}
						columnResultSet.close();
					}
				} else {
					// Table does not exist, so create it.
					createTable(propertySet, stmt);
				}
				
				tableResultSet.close();
			}
		}
		
		// Having ensured that all the tables exist, now
		// create the foreign key constraints.
		// This must be done in a second pass because
		// otherwise we might try to create a foreign key
		// constraint before the foreign key has been created.
		for (Iterator iter = PropertySet.getPropertySetIterator(); iter.hasNext(); ) {
			PropertySet propertySet = (PropertySet)iter.next();
			
			if (!propertySet.isExtension()) {
				String tableName = propertySet.getId().replace('.', '_');
				
				// Check the foreign keys in derived tables that point to the
				// base table entries.
				
				if (propertySet.getBasePropertySet() != null) {
					String primaryTableName = propertySet.getBasePropertySet().getId().replace('.', '_');
					checkForeignKey(dmd, stmt, tableName, "_ID", primaryTableName);
				}
				
				// Check all the other foreign keys.
				Vector columnInfos = buildColumnList(propertySet);
				
				for (Iterator iter2 = columnInfos.iterator(); iter2.hasNext(); ) {
					ColumnInfo columnInfo = (ColumnInfo)iter2.next();
					
					if (columnInfo.foreignKeyPropertySet != null) {
						String primaryTableName = columnInfo.foreignKeyPropertySet.getId().replace('.', '_');
						checkForeignKey(dmd, stmt, tableName, columnInfo.columnName, primaryTableName);
					}
				}
			}
		}		
	}

	/**
	 * @param stmt
	 * @param dmd
	 * @param tableName
	 * @param columnName
	 * @param primaryTableName
	 */
	private void checkForeignKey(DatabaseMetaData dmd, Statement stmt, String tableName, String columnName, String primaryTableName) 
			throws SQLException {
		ResultSet columnResultSet2 = dmd.getCrossReference(null, null, primaryTableName.toUpperCase(), null, null, tableName.toUpperCase());
		traceResultSet(columnResultSet2);
		
		ResultSet columnResultSet = dmd.getCrossReference(null, null, primaryTableName.toUpperCase(), null, null, tableName.toUpperCase());
		if (columnResultSet.next()) {
			// A foreign key constraint already exists.
			// Check that it is the correct constraint.
			if (!columnResultSet.getString("PKCOLUMN_NAME").equals("_ID")
			 || !columnResultSet.getString("FKCOLUMN_NAME").equals(columnName)
			 || columnResultSet.next()) {
				throw new RuntimeException("The database schema is invalid.  "
						+ "Table " + tableName.toUpperCase() + " must contain a foreign key column called " + columnName
						+ " constrained to primary key _ID in table " + primaryTableName.toUpperCase() 
						+ ".  No other foreign key/primary key mappings may exist between these two tables.");
			}
		} else {
			// The foreign key constraint does not exist so we add it.
			String sql = 
				"ALTER TABLE " + tableName
				+ " ADD FOREIGN KEY (\"" + columnName
				+ "\") REFERENCES " + primaryTableName + "(_ID)";
			stmt.executeUpdate(sql);	
		}
		columnResultSet.close();
	}

	static ExtendableObject materializeObjectCached(ResultSet rs, PropertySet propertySet, IObjectKey parentKey, SessionManager sessionManager) throws SQLException {
		int rowId = rs.getInt(1); // '_ID' column
		ObjectKeyCached key = new ObjectKeyCached(rowId, sessionManager);
		
		ExtendableObject extendableObject = materializeObject(rs, propertySet, key, parentKey, sessionManager);
		
		key.setObject(extendableObject);
		
		return extendableObject;
	}

	/**
	 * Materialize an object from a row of data.
	 * <P>
	 * This version of this method should be called when
	 * the caller knows the parent of the object to
	 * be materialized (or at least, the key to the parent).
	 * The parent key is passed to this method by the caller
	 * and that saves this method from needing to build a
	 * new parent key from the object's data in the database.
	 *
	 * @param rs
	 * @param propertySet
	 * @param key
	 * @param parentKey
	 * @param sessionManager
	 * @return
	 * @throws SQLException
	 */ 
	static ExtendableObject materializeObject(ResultSet rs, PropertySet propertySet, IObjectKey key, IObjectKey parentKey, SessionManager sessionManager) throws SQLException {
		/**
		 * The list of parameters to be passed to the constructor
		 * of this object.
		 */
		Object[] constructorParameters;
		
		Vector constructorProperties = propertySet.getConstructorProperties();
		constructorParameters = new Object[3 + constructorProperties.size()];
		Map extensionMap = new HashMap();
		constructorParameters[0] = key;
		constructorParameters[1] = extensionMap;
		constructorParameters[2] = parentKey;
		
		// Set the remaining parameters to the constructor.
		for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
			String columnName = propertyAccessor.getLocalName();
			Class valueClass = propertyAccessor.getValueClass(); 
			Object value;
			if (propertyAccessor.isList()) {
				value = new ListManagerCached(sessionManager, propertyAccessor);
			} else if (valueClass == int.class) {
				value = new Integer(rs.getInt(columnName));
			} else if (valueClass == long.class) {
				value = new Long(rs.getLong(columnName));
			} else if (valueClass == Long.class) {
				value = new Long(rs.getLong(columnName));
			} else if (valueClass == String.class) {
				value = rs.getString(columnName);
			} else if (valueClass == Date.class) {
				value = rs.getDate(columnName);
			} else if (ExtendableObject.class.isAssignableFrom(valueClass)) {
				int rowIdOfProperty = rs.getInt(columnName);
				PropertySet propertySetOfProperty = PropertySet.getPropertySet(valueClass);
				
				// We have a problem here.
				// It may be that the type of this property is a derivable property set,
				// so the value of the property is a property set that is derived from the
				// property set for the type of this property.
				// It may also be that the property set for the type of this property
				// is not cached but the value itself is of a derived property set
				// that is cached.
				// Therefore we cannot necessarily know if a property value is
				// cached or not until we have read the type of the
				// instance of the property value from the database.
				// That would defeat the performance benefits of caching.
				// Alternatively we could look up the item in all the possible
				// maps of cached derived types.
				// However, that involves some work for what is really
				// a rare scenario.  Therefore we take another approach.
				// We just don't cache the object.  This means even if
				// a property set is set to be cached, there may be some
				// instances of that property set that are read not from the
				// cache but for which another instance is created by
				// reading the database.  There is nothing in the design
				// of the caches that prohibit duplicates of the item being
				// created outside the cache.
				Map map = sessionManager.getMapOfCachedObjects(propertySetOfProperty);
				if (map == null) {
					value = new ObjectKeyUncached(rowIdOfProperty, propertySetOfProperty, sessionManager);
				} else {
					value = ((ExtendableObject)map.get(new Integer(rowIdOfProperty))).getObjectKey();
				}
			} else {
				throw new RuntimeException("unknown type");
			}
			
			if (rs.wasNull()) {
				value = null;
			}
			constructorParameters[propertyAccessor.getIndexIntoConstructorParameters()] = value;
		}
		
		// We can now create the object.
		ExtendableObject extendableObject = (ExtendableObject)propertySet.constructImplementationObject(constructorParameters);
		
		return extendableObject;
	}
	
	/**
	 * Materialize an object from a row of data.
	 * <P>
	 * This version of this method should be called when
	 * the caller does not know the parent of the object to
	 * be materialized.  The parent key is build from data in
	 * the row.
	 * 
	 * @param rs
	 * @param propertySet
	 * @param key
	 * @param sessionManager
	 * @return
	 * @throws SQLException
	 */
	static ExtendableObject materializeObject(ResultSet rs, PropertySet propertySet, IObjectKey key, SessionManager sessionManager) throws SQLException {
		// We need to obtain the key for the parent object.
		// The parent object may be cached or may be uncached.
		
		// The property set of the parent object may not be known
		// without looking at the row data.  For example, the
		// parent of an account may be another account (if
		// the account is a sub-account) or may be the session.
		
		// A column exists in this table for each list which
		// can contain objects of this type.  Only one of these
		// columns can be non-null so we must find that column.
		// The value of that column will be the integer id of
		// the parent.
		
		// An optimization would allow the 
		// column to be absent when the parent object is the
		// session object (as only one session object may exist).
		
		// For each list that may contain this object, see if the
		// appropriate column is non-null.
		
		PropertySet parentPropertySet = null;
		int parentId = -1;
		boolean nonNullValueFound = false;
		
		PropertySet propertySet2 = propertySet;
		do {
		Vector list = (Vector)tablesMap.get(propertySet);
		
		// Find all properties in any property set that are
		// a list of objects with the type as this property set.
		// A column must exist in this table for each such property
		// that exists in another property set.
		for (Iterator iter2 = list.iterator(); iter2.hasNext(); ) {
			ParentList parentList = (ParentList)iter2.next();

			parentId = rs.getInt(parentList.columnName);
			if (!rs.wasNull()) {		
				parentPropertySet = parentList.parentPropertySet;
				nonNullValueFound = true;
				break;
			}
		}	
			propertySet2 = propertySet2.getBasePropertySet();
		} while (propertySet2 != null);	
			
		if (!nonNullValueFound) {
			// A database optimization causes no parent column to
			// exist for the case where the parent object is the
			// session.
			try {
				parentPropertySet = PropertySet.getPropertySet("net.sf.jmoney.session");
			} catch (PropertySetNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException("internal error");
			}
			// TODO: Is zero always the id of the session object?
			parentId = 0;
		}
		
		// Determine if the parent object is cached or uncached.
		IObjectKey parentKey;
		if (parentPropertySet.getId().equals("net.sf.jmoney.transaction") 
				|| parentPropertySet.getId().equals("net.sf.jmoney.entry")) {
			// Not cached
			parentKey = new ObjectKeyUncached(parentId, parentPropertySet, sessionManager);
		} else if (parentPropertySet.getId().equals("net.sf.jmoney.session")) { 
			parentKey = sessionManager.getSessionKey();
		} else {
			// Cached
			parentKey = sessionManager.lookupObject(parentPropertySet, parentId).getObjectKey();
		}
		
		ExtendableObject extendableObject = JDBCDatastorePlugin.materializeObject(rs, propertySet, key, parentKey, sessionManager);
		
		return extendableObject;
	}


	/**
	 * Create a table.  This method should be called when
	 * a new database is being initialized or when a new
	 * table is needed because a new extendable property 
	 * set has been added.
	 * 
	 * @param propertySet The property set whose table is to
	 * 			be created.  This property set must not be an
	 * 			extension property set.  (No tables exist for extension
	 * 			property sets.  Extension property sets are supported
	 * 			by adding columns to the tables for the property sets
	 * 			which they extend).
	 * @param stmt A <code>Statement</code> object 
	 * 			that is to be used by this method
	 * 			to submit the 'CREATE TABLE' command.
	 * @throws SQLException
	 */
	void createTable(PropertySet propertySet, Statement stmt) throws SQLException {
		String sql = "CREATE TABLE "
			+ propertySet.getId().replace('.', '_') 
			+ " (_id INT IDENTITY";
		
		String foreignKeys = "";
		
		Vector columnInfos = buildColumnList(propertySet);
		
		for (Iterator iter2 = columnInfos.iterator(); iter2.hasNext(); ) {
			ColumnInfo columnInfo = (ColumnInfo)iter2.next();
			
			sql += ", \"" + columnInfo.columnName + "\" " + columnInfo.columnDefinition;
		}
		sql += foreignKeys;
		
		sql += ")";
		
		stmt.executeQuery(sql);
	}

	/**
	 * @param propertySet
	 * @param values
	 * @param listProperty
	 * @param parent
	 * @param sessionManager
	 * 
	 * @return The id of the inserted row
	 */
	public static int insertIntoDatabase(PropertySet propertySet, ExtendableObject newObject, PropertyAccessor listProperty, ExtendableObject parent, SessionManager sessionManager) {
		int rowId = -1;

		// We must insert into the base table first, then the table for the objects
		// derived from the base and so on.  The reason is that each derived table
		// has a primary key field that is a foreign key into its base table.
		// We can get the chain of property sets only by starting at the given 
		// property set and repeatedly getting the base property set.  We must
		// therefore store these so that we can loop through the property sets in
		// the reverse order.
		
		Statement stmt = sessionManager.getReusableStatement();

		Vector propertySets = new Vector();
		for (PropertySet propertySet2 = propertySet; propertySet2 != null; propertySet2 = propertySet2.getBasePropertySet()) {
			propertySets.add(propertySet2);
		}
		
		for (int index = propertySets.size()-1; index >= 0; index--) {
			PropertySet propertySet2 = (PropertySet)propertySets.get(index);
			
			String sql = "INSERT INTO " 
				+ propertySet2.getId().replace('.', '_')
				+ " (";
			
			String columnNames = "";
			String columnValues = "";
			String separator = "";
			
			if (index != propertySets.size()-1) {
				columnNames = "_ID";
				columnValues = "IDENTITY()";
				separator = ", ";
			}
			
			for (Iterator iter = propertySet2.getPropertyIterator2(); iter.hasNext(); ) {
				PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
				
				if (propertyAccessor.isScalar()) {
					String columnName;
					if (propertyAccessor.getPropertySet().isExtension()) {
						columnName = propertyAccessor.getName().replace('.', '_');
					} else {
						columnName = propertyAccessor.getLocalName();
					}
					
					// Get the value from the passed property value array.
					Object value = newObject.getPropertyValue(propertyAccessor);

					columnNames += separator + "\"" + columnName + "\"";
					columnValues += separator + valueToSQLText(value);
					
					separator = ", ";
				}
			}

			// Set the parent id in the appropriate column
			if (listProperty.getValueClass() == propertySet2.getImplementationClass()) {
				IDatabaseRowKey parentKey = (IDatabaseRowKey)parent.getObjectKey();
				String valueString = Integer.toString(parentKey.getRowId());
				String parentColumnName = listProperty.getName().replace('.', '_');
				columnNames += separator + "\"" + parentColumnName + "\"";
				columnValues += separator + valueString;
				separator = ", ";
			}

			// If the base-most property set and it is derivable, 
			// the _PROPERTY_SET column must be set.
			if (propertySet2.getBasePropertySet() == null
			 && propertySet2.isDerivable()) {
				columnNames += separator + "_PROPERTY_SET";
				// Set to the id of the final
				// (non-derivable) property set for this object.
				PropertySet finalPropertySet = (PropertySet)propertySets.get(0); 
				columnValues += separator + "\'" + finalPropertySet.getId() + "\'";
				separator = ", ";
			}
			
			sql += columnNames + ") VALUES(" + columnValues + ")";
			
			try {
				stmt.executeQuery(sql);

				// Get the row id of the object.
				// This is obtained after the row has been added
				// to the base table.
				if (index == propertySets.size()-1) {
					ResultSet rs = stmt.executeQuery("CALL IDENTITY()");
					rs.next();
					rowId = rs.getInt(1);
				}
			} catch (SQLException e) {
				// TODO Handle this properly
				e.printStackTrace();
				throw new RuntimeException("internal error");
			}
		}

		return rowId;
	}

	/**
	 * Execute SQL UPDATE statements to update the database with
	 * the new values of the properties of an object.
	 * <P>
	 * The SQL statements will verify the old values of the properties
	 * in the WHERE clause.  If the database does not contain an object
	 * with the expected old property values that an exception is raised,
	 * causing the transaction to be rolled back.
	 * 
	 * @param rowId
	 * @param oldValues
	 * @param newValues
	 * @param sessionManager
	 */
	public static void updateProperties(PropertySet propertySet, int rowId, Object[] oldValues, Object[] newValues, SessionManager sessionManager) {
		Statement stmt = sessionManager.getReusableStatement();

		// The array of property values contains the properties from the
		// base table first, then the table derived from that and so on.
		// We therefore process the tables starting with the base table
		// first.  This requires first copying the property sets into
		// an array so that we can iterate them in reverse order.
		Vector propertySets = new Vector();
		for (PropertySet propertySet2 = propertySet; propertySet2 != null; propertySet2 = propertySet2.getBasePropertySet()) {
			propertySets.add(propertySet2);
		}
		
		int propertyIndex = 0;

		for (int index = propertySets.size()-1; index >= 0; index--) {
			PropertySet propertySet2 = (PropertySet)propertySets.get(index);
			
			String sql = "UPDATE " 
				+ propertySet2.getId().replace('.', '_')
				+ " SET ";
			
			String updateClauses = "";
			String whereTerms = "";
			String separator = "";
			
			for (Iterator iter = propertySet2.getPropertyIterator2(); iter.hasNext(); ) {
				PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
				
				if (propertyAccessor.isScalar()) {
					
					if (propertyAccessor.getIndexIntoScalarProperties() != propertyIndex) {
						throw new RuntimeException("index mismatch");
					}
					// See if the value of the property has changed.
					Object oldValue = oldValues[propertyIndex];
					Object newValue = newValues[propertyIndex];
					propertyIndex++;
					
					if ((oldValue == null && newValue != null)
					 || (oldValue != null &&!oldValue.equals(newValue))) {
						
						String columnName;
						if (propertyAccessor.getPropertySet().isExtension()) {
							columnName = propertyAccessor.getName().replace('.', '_');
						} else {
							columnName = propertyAccessor.getLocalName();
						}
						
						updateClauses += separator + "\"" + columnName + "\"=" + valueToSQLText(newValue);
						whereTerms += " AND \"" + columnName + "\"=" + valueToSQLText(oldValue);
						
						separator = ", ";
					}
				}
			}
			
			// If no properties have been updated in a table then no update
			// statement should be executed.
			
			if (!separator.equals("")) {
				sql += updateClauses + " WHERE _ID=" + rowId + whereTerms;
				
				try {
					stmt.executeQuery(sql);
				} catch (SQLException e) {
					// TODO Handle this properly
					e.printStackTrace();
					throw new RuntimeException("internal error");
				}
			}
		}
	}

	/**
	 * @param actualPropertySet
	 * @param rowId
	 * @param propertyAccessor
	 * @param oldValue
	 * @param newValue
	 * @param sessionManager
	 */
	public static void updateProperties(PropertySet actualPropertySet, int rowId, PropertyAccessor propertyAccessor, Object oldValue, Object newValue, SessionManager sessionManager) {
	
	// Build two arrays of old and new values.
	// Ultimately we will have a layer between that does this
	// for us, also combining multiple updates to the same row
	// into a single update.  Until then, we need this code here.

		// TODO: improve performance here.
		int count = 0;
		for (Iterator iter = actualPropertySet.getPropertyIterator3(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor2 = (PropertyAccessor)iter.next();
			if (propertyAccessor2.isScalar()) {
				count++;
			}
		}
		
		Object [] oldValues = new Object[count];
		Object [] newValues = new Object[count];
		
		int i = 0;
		for (Iterator iter = actualPropertySet.getPropertyIterator3(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor2 = (PropertyAccessor)iter.next();
			if (propertyAccessor2.isScalar()) {
				if (propertyAccessor2 == propertyAccessor) {
				oldValues[i] = oldValue;
				newValues[i] = newValue;
				} else {
					// kludge: both null means no change.
				oldValues[i] = null;
				newValues[i] = null;
				}
				i++;
			}
		}
		updateProperties(actualPropertySet, rowId, oldValues, newValues, sessionManager);
	}

	
	/**
	 * Given a value of a property as an Object, return the text that 
	 * represents the value in an SQL statement.
	 * 
	 * @param newValue
	 * @return
	 */
	// TODO: If we always used prepared statements with parameters
	// then we may not need this method at all.
	private static String valueToSQLText(Object value) {
		String valueString;
		
		if (value != null) {
			Class valueClass = value.getClass();
			if (valueClass == String.class
					|| valueClass == char.class
					|| valueClass == Character.class) {
				valueString = '\'' + value.toString().replaceAll("'", "''") + '\'';
			} else if (valueClass == Date.class) {
				Date date = (Date)value;
				valueString = '\''
					+ new Integer(date.getYear() + 1900).toString() + "-"
					+ new Integer(date.getMonth()).toString() + "-"
					+ new Integer(date.getDay()).toString()
					+ '\'';
			} else if (ExtendableObject.class.isAssignableFrom(valueClass)) {
				ExtendableObject extendableObject = (ExtendableObject)value;
				IDatabaseRowKey key = (IDatabaseRowKey)extendableObject.getObjectKey();
				valueString = Integer.toString(key.getRowId());
			} else { 
				valueString = value.toString();
			}
		} else {
			valueString = "NULL";
		}

		return valueString;
	}

	/**
	 * Execute SQL DELETE statements to remove the given object, if present,
	 * from the database.
	 * 
	 * @param rowId
	 * @param extendableObject
	 * @param sessionManager
     * @return true if the object was present, false if the object
     * 				was not present in the database.
	 */
// TODO: throw error if object is referenced.	
	public static boolean deleteFromDatabase(int rowId, ExtendableObject extendableObject, SessionManager sessionManager) {
		PropertySet propertySet = PropertySet.getPropertySet(extendableObject.getClass()); 
		
		Statement stmt = sessionManager.getReusableStatement();

		// Because each table for a derived class contains a foreign key
		// constraint to the table for the base class, we must delete the rows
		// starting with the most derived table and ending with the base-most
		// table.
		
		// Alternatively, we could have set the 'CASCADE' option for delete
		// in the database and just delete the row in the base-most table.
		// However, it is perhaps safer not to use 'CASCADE'.
		
		for (PropertySet propertySet2 = propertySet; propertySet2 != null; propertySet2 = propertySet2.getBasePropertySet()) {
			
			String sql = "DELETE FROM " 
				+ propertySet2.getId().replace('.', '_')
				+ " WHERE _ID=" + rowId;
			
				try {
					int rowCount = stmt.executeUpdate(sql);
					if (rowCount != 1) {
						if (rowCount == 0
						 && propertySet2 == propertySet) {
							// The object does not exist in the database.
							// This is not an error, but we do return 'false'.
							return false;
						}
						throw new RuntimeException("database is inconsistent");
					}
				} catch (SQLException e) {
					// TODO Handle this properly
					e.printStackTrace();
					throw new RuntimeException("internal error");
			}
		}
		
		return true;
	}

	/**
	 * @param propertySet
	 * @param sessionManager
	 * @param objectKeyProxy The key to this object.  This is required by this
	 * 			method because it must be passed to the constructor.
	 * 			This method does not call the setObject or setRowId
	 * 			methods on this key.  It is the caller's responsibility
	 * 			to call these methods.
	 * @param parent
	 * @param constructWithCachedLists
	 * @return
	 */
	public static ExtendableObject constructExtendableObject(PropertySet propertySet, SessionManager sessionManager, IDatabaseRowKey objectKey, ExtendableObject parent, boolean constructWithCachedLists) {
		Vector constructorProperties = propertySet.getConstructorProperties();
		int numberOfParameters = constructorProperties.size();
		if (!propertySet.isExtension()) {
			numberOfParameters += 3;
		}
		Object[] constructorParameters = new Object[numberOfParameters];
		
		Object [] defaultValues = propertySet.getDefaultPropertyValues2();
		
		constructorParameters[0] = objectKey;
		constructorParameters[1] = null;
		constructorParameters[2] = parent.getObjectKey();
	
		// For all lists, set the Collection object to be a Vector.
		// For all primative properties, get the value from the passed object.
		// For all extendable objects, we get the property value from
		// the passed object and then get the object key from that.
		// This works because all objects must be in a list and that
		// list owns the object, not us.
		int indexIntoDefaultValues = 0;
		for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
			int index = propertyAccessor.getIndexIntoConstructorParameters();
			if (propertyAccessor.isScalar()) {
				// Get the value from the array of values.
				Object value = defaultValues[indexIntoDefaultValues++];
				
				if (value != null) {
					if (propertyAccessor.getValueClass().isPrimitive()
							|| propertyAccessor.getValueClass() == String.class
							|| propertyAccessor.getValueClass() == Long.class
							|| propertyAccessor.getValueClass() == Date.class) {
						constructorParameters[index] = value;
					} else {
						constructorParameters[index] = ((ExtendableObject)value).getObjectKey();
					}
				} else { 
					constructorParameters[index] = null;
				}
			} else {
				// Must be an element in an array.
				if (constructWithCachedLists) {
					constructorParameters[index] = new ListManagerCached(sessionManager, propertyAccessor);
				} else {
					constructorParameters[index] = new ListManagerUncached(objectKey, sessionManager, propertyAccessor);
				}
			}
		}
		
		// We can now create the object.
		ExtendableObject extendableObject = (ExtendableObject)propertySet.constructImplementationObject(constructorParameters);
		
		return extendableObject;
	}

}
