/*
 * Author: atotic
 * Created on Apr 30, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * GetVariable network command.
 * 
 * GetVariable gets the value of the variable from network as XML.
 * The caller can busy-wait for the response.
 */
public class GetVariableCommand extends AbstractDebuggerCommand {

    String locator;
    boolean isError = false;
    int responseCode;
    String payload;

    public GetVariableCommand(AbstractDebugTarget debugger, String locator) {
        super(debugger);
        this.locator = locator;
    }

    public String getOutgoing() {
        return makeCommand(getCommandId(), sequence, locator);
    }

    public boolean needResponse() {
        return true;
    }

    public void processOKResponse(int cmdCode, String payload) {
        responseCode = cmdCode;
        if (cmdCode == getCommandId())
            this.payload = payload;
        else {
            isError = true;
            PydevDebugPlugin.log(IStatus.ERROR, "Unexpected response to "+this.getClass(), null);
        }
    }

    protected int getCommandId() {
        return CMD_GET_VARIABLE;
    }
    
    public void processErrorResponse(int cmdCode, String payload) {
        responseCode = cmdCode;
        this.payload = payload;
        isError = true;
    }
        
    public String getResponse() throws CoreException {
        if (isError) 
            throw new CoreException(
                            PydevDebugPlugin.makeStatus(IStatus.ERROR,
                            "pydevd error:" + payload , 
                             null));
        else
            return payload;
    }
}

