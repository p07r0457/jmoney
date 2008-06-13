import junit.framework.Test;
import junit.framework.TestSuite;


public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("QIF Parsing");
		//$JUnit-BEGIN$

		suite.addTest(new TestSuite(FinanceQIFTests.class));
		suite.addTest(new TestSuite(jGnashTests.class));
		
		//$JUnit-END$
		return suite;
	}

}
