package net.sf.jmoney.views.feedback;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class StatusContentProvider implements ITreeContentProvider {

		public void dispose() { // do nothing
		}

		public Object[] getChildren(Object element) {
			IStatus status = (IStatus)element;
			return status.getChildren();
		}

		public Object[] getElements(Object element) {
			/*
			 * If the input is a single status (not a multi-status)
			 * then we return that as the element.  It would be more
			 * consistent if we just returned an empty list, as the
			 * root status is not normally displayed in the tree,
			 * but for time being, leave it like this.
			 */
			IStatus status = (IStatus)element;
			if (status.isMultiStatus()) {
				return status.getChildren();
			} else {
				return new Object [] { status };
			}
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			IStatus status = (IStatus)element;
			return status.isMultiStatus();
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { // do nothing
		}
	}
