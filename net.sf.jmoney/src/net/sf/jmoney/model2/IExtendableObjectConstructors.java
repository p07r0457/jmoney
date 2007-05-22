package net.sf.jmoney.model2;


public interface IExtendableObjectConstructors<E extends ExtendableObject> {
	E construct(IObjectKey objectKey, IObjectKey parentKey);
	E construct(IObjectKey objectKey, IObjectKey parentKey, IValues values);
}

