package net.sf.jmoney.jdbcdatastore;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

/** 
 * Initializes a preference store with default preference values 
 * for this plug-in.
 * <P>
 * This class is an implementation class for the <code>initializer</code>
 * element in the org.eclipse.core.runtime.preferences extension point.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {
	public PreferenceInitializer() {
		super();
	}
	
	@Override
	public void initializeDefaultPreferences() {
		Preferences store = JDBCDatastorePlugin.getDefault().getPluginPreferences();
		
    	store.setDefault("promptEachTime", true);
		store.setDefault("driverOption", "Other");

		/*
		 * These default values will use the HSQLDB database in-process. These
		 * are good default values because there is no database setup and so
		 * this plug-in will work 'out-of-the-box'.
		 */
		store.setDefault("driver", "org.hsqldb.jdbcDriver");
		store.setDefault("subProtocol", "hsqldb");
		store.setDefault("subProtocolData", "file:accounts");
		store.setDefault("user", "sa");
		store.setDefault("password", "");
		
		// Use this value for subProtocolData instead if you
		// want to use a database that is not in-process, allowing you
		// to inspect the database while debugging.
//		store.setDefault("subProtocolData", "hsql://localhost/accounts");
	}
}