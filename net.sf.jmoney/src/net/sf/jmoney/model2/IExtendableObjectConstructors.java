package net.sf.jmoney.model2;


public interface IExtendableObjectConstructors<E extends ExtendableObject> {
	E construct(IObjectKey objectKey, ListKey parentKey);
	E construct(IObjectKey objectKey, ListKey parentKey, IValues values);
}

