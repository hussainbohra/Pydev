package org.python.pydev.debug.ui.actions;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.python.pydev.debug.model.PyVariableCollection;

public class PyVariableRefreshHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			Iterator iter = ((IStructuredSelection) selection).iterator();
			while (iter.hasNext()) {
				Object element = iter.next();
				if (element instanceof PyVariableCollection) {
					PyVariableCollection selectedElement = ((PyVariableCollection) element);
					try {
						if (("Referrers".equals(selectedElement.getName()))) {
							selectedElement.updateNetworkState(0); // 0 NETWORK_REQUEST_NOT_REQUESTED
							selectedElement.setCurrentRange(-1); // -1 indicates refresh
							selectedElement.getVariables();
							selectedElement.setCurrentRange(0); // reset the current range after refresh
							selectedElement.fireEvent();
						}
					} catch (DebugException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}
}
