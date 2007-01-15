package net.sf.jmoney.oda.driver;

import java.util.Vector;

import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;

public class ParameterMetaData implements IParameterMetaData {

	Vector<ParameterData> parameters = new Vector<ParameterData>();
	
	public ParameterMetaData(IFetcher fetcher) {
		fetcher.addParameters(parameters);
	}

	public int getParameterCount() throws OdaException {
		return parameters.size();
	}

	public int getParameterMode(int parameterNumber) throws OdaException {
		// All parameters are input only parameters
		return parameterModeIn;
	}

	public int getParameterType(int parameterNumber) throws OdaException {
		return parameters.get(parameterNumber-1).getColumnType().getNativeType();
	}

	public String getParameterTypeName(int parameterNumber) throws OdaException {
		return parameters.get(parameterNumber-1).getColumnType().getNativeTypeName();
	}

	public int getPrecision(int parameterNumber) throws OdaException {
		return parameters.get(parameterNumber-1).getColumnType().getPrecision();
	}

	public int getScale(int parameterNumber) throws OdaException {
		return parameters.get(parameterNumber-1).getColumnType().getScale();
	}

	public int isNullable(int parameterNumber) throws OdaException {
		return parameters.get(parameterNumber-1).isNullable() ? parameterNullable : parameterNoNulls;
	}
}
