package net.sf.jmoney.model2;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public abstract class AbstractDataOperation extends AbstractOperation {

	private Session session;

	public AbstractDataOperation(Session session, String label) {
		super(label);
		this.session = session;
	}

	/**
	 * The changes that need to be undone to 'undo' this operation (i.e. these
	 * are the changes made when the operation was executed or last redone), or
	 * null if this operation has not yet been executed or has been undone.
	 */
	private ChangeManager.UndoableChange redoChanges = null;

	/**
	 * The changes that need to be undone to 'redo' this operation (i.e. these
	 * are the changes made when the operation was last undone), or null if this
	 * operation has not yet been executed or has not yet been undone, or if
	 * this operation was redone since it was last undone.
	 */
	private ChangeManager.UndoableChange undoChanges = null;

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		session.getChangeManager().setUndoableChange();

		execute();

		redoChanges = session.getChangeManager().takeUndoableChange();

		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		session.getChangeManager().setUndoableChange();

		undoChanges.undoChanges();
		undoChanges = null;

		redoChanges = session.getChangeManager().takeUndoableChange();

		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		session.getChangeManager().setUndoableChange();

		redoChanges.undoChanges();
		redoChanges = null;

		undoChanges = session.getChangeManager().takeUndoableChange();

		return Status.OK_STATUS;
	}

	public abstract IStatus execute() throws ExecutionException;
}
