package org.python.pydev.debug.ui.actions;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
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
					final PyVariable variable = (PyVariable) element;
					Display display = Display.getDefault();

					display.syncExec(new Runnable() {
						public void run() {
							List elementList = ((TreeSelection) selection).toList();
							elementList.remove(variable);
						}
					});
					IWorkbenchPage page = DebugUIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
					IViewPart part = page.findView(IDebugUIConstants.ID_VARIABLE_VIEW);
					((VariablesView) part).getViewer();
				}
			}
		}
		return null;
	}
}
