package net.sf.jmoney.model2;

public interface CurrentSessionChangeListener extends SessionChangeListener {
	/**
	 * The session has been replaced.  All views of session data
	 * should be fully refreshed.
	 * 
	 * @param oldSession the previous open session, or null if no
	 * 		session was previously open
	 * @param newSession the new session, or null if the previous
	 *      session was closed using the File, Close action
	 */
    void sessionReplaced(Session oldSession, Session newSession);
}
