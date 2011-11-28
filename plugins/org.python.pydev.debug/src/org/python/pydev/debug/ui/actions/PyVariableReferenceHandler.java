package org.python.pydev.debug.ui.actions;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.python.pydev.debug.model.PyVariable;
import org.python.pydev.debug.model.PyVariableCollection;

/**
 * @author hussain.bohra 
 * Handler for variable view -> context menu -> All References. 
 * If entry is PyVariableCollection, it creates a new node 'Referrer' inside the existing tree
 * 
 */
public class PyVariableReferenceHandler extends AbstractHandler {


	public Object execute(ExecutionEvent event) throws ExecutionException {
		final ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			Iterator iter = ((IStructuredSelection) selection).iterator();
			while (iter.hasNext()) {
				Object element = iter.next();
				if (element instanceof PyVariableCollection) {
					((PyVariableCollection) element).initializeReferrers();
				} else if (element instanceof PyVariable) {
					// TODO: Identify how to change the tree element from PyVariable to PyVariableCollection
					PyVariable variable = (PyVariable) element;
				}
			}
		}
		return null;
	}
}
