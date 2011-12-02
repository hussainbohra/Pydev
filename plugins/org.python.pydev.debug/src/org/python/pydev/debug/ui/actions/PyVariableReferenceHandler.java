package org.python.pydev.debug.ui.actions;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.viewers.model.ITreeModelContentProviderTarget;
import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.IVariableLocator;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.model.PyVariable;
import org.python.pydev.debug.model.PyVariableCollection;

/**
 * @author hussain.bohra
 *
 * Handler for variable view -> context menu -> All References. 
 * If selected variable is a tree node i.e. PyVariableCollection object
 * 	it creates a new node 'Referrer' inside the existing tree
 *
 * if selected variable is a leaf node i.e. PyVariable object
 * 	Creates the new PyVariableCollection from selected variable
 * 		if selected variable is at the root level i.e. in the method scope
 * 			replace existing variable with new variable collection in the frame
 *
 * 		if selected variable is inside any PyVariableCollection i.e. inside
 * 		any complex datastructure (obj, list, map..)
 * 			replace existing variable with new variable collection in the Parent
 * 			collection
 *
 *  Initialize referrers and fire an update event
 */
@SuppressWarnings("restriction")
public class PyVariableReferenceHandler extends AbstractHandler {


	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			Iterator iter = ((IStructuredSelection) selection).iterator();
			while (iter.hasNext()) {
				Object element = iter.next();
				if (element instanceof PyVariableCollection) {
					PyVariableCollection selectedElement = ((PyVariableCollection) element);
					try {
						// Pydev adds a "Globals" node, Referrers
						// can't be collected for the same
						if (!("Globals".equals(selectedElement.getName()))) {
							selectedElement.initializeReferrers();
							selectedElement.fireEvent();
						}
					} catch (DebugException e) {
						e.printStackTrace();
					}
				} else if (element instanceof PyVariable) {
					IVariableLocator locator = ((PyVariable) element)
							.getLocator();
					PyVariable variable = (PyVariable) element;
					removeVariable(element);
					PyVariableCollection newCollection = createVariableCollection(
							variable, locator);

					if (locator instanceof PyStackFrame) {
						PyStackFrame currentFrame = (PyStackFrame) locator;
						currentFrame.replaceVariables(variable, newCollection);
						newCollection.initializeReferrers();
						newCollection.fireEvent();
					} else if (locator instanceof PyVariableCollection) {
						PyVariableCollection parentCollection = (PyVariableCollection) locator;
						parentCollection.replaceVariables(variable,
								newCollection);
						newCollection.initializeReferrers();
						parentCollection.fireEvent();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Identify an instance of a variable viewer from the Active workbench
	 * and remove the selected primitive type varriable (int/str/boolean..)
	 * from the pyvariableCollection
	 *
	 * @return
	 */
	private void removeVariable(Object variable) {
		IWorkbenchPage page = DebugUIPlugin.getDefault().getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		IViewPart part = page.findView(IDebugUIConstants.ID_VARIABLE_VIEW);
		Viewer viewer = ((VariablesView) part).getViewer();
		((ITreeModelContentProviderTarget) viewer).remove(variable);
	}

	/**
	 * Creates the new PyVariableCollection using the selected PyVariable
	 * Update the Network State to NETWORK_STATE_ARRIVED (value = 2)
	 *
	 * @param locator
	 * @param variable
	 * @return
	 */
	private PyVariableCollection createVariableCollection(PyVariable variable,
			IVariableLocator locator) {

		PyVariableCollection newVariableCollection = null;
		try {
			newVariableCollection = new PyVariableCollection(
					(AbstractDebugTarget) variable.getDebugTarget(),
					variable.getName(), variable.getReferenceTypeName(),
					variable.getValueString(), locator);

			newVariableCollection.updateNetworkState(2);
		} catch (DebugException e) {
			e.printStackTrace();
		}
		return newVariableCollection;
	}
}
