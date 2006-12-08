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
	
	public void initializeDefaultPreferences() {
		Preferences store = JDBCDatastorePlugin.getDefault().getPluginPreferences();
		
    	store.setDefault("promptEachTime", true);
		store.setDefault("driverOption", "Other");
		store.setDefault("driver", "org.hsqldb.jdbcDriver");
		store.setDefault("subProtocol", "hsqldb");
		store.setDefault("subProtocolData", "hsql://localhost/accounts");
		store.setDefault("user", "sa");
		store.setDefault("password", "");
	}
}