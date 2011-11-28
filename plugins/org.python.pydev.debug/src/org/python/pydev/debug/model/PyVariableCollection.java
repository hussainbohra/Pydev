/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on May 4, 2004
 */
package org.python.pydev.debug.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.GetVariableCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;

/**
 * PyVariableCollection represents container variables.
 * 
 * It knows how to fetch its contents over the network.
 * 
 */
public class PyVariableCollection extends PyVariable implements ICommandResponseListener, IVariableLocator {

    PyVariable[] variables = new PyVariable[0];
	PyVariableCollection referrers = null;
	IVariable[] waitVariables = null;
    
    static final int NETWORK_REQUEST_NOT_REQUESTED = 0;
    static final int NETWORK_REQUEST_NOT_ARRIVED = 1;
    static final int NETWORK_REQUEST_ARRIVED = 2;
    
    /**
     * Defines the network state
     */
    int networkState = NETWORK_REQUEST_NOT_REQUESTED; // Network request state: 0 did not request, 1 requested, 2 requested & arrived
    
    private boolean fireChangeEvent = true;
    
    public PyVariableCollection(AbstractDebugTarget target, String name, String type, String value, IVariableLocator locator) {
        super(target, name, type, value, locator);
    }
    
    public String getDetailText() throws DebugException {
        return super.getDetailText();
    }


    /**
     * Initialize a new empty PyVariableCollection (Referrers) under the selected variable
     * 
     */
	public void initializeReferrers() {
		if (referrers == null) {
			String value = String.format(Constants.GET_REFERRERS_EXPR,
					this.name);
			if (this.type.startsWith(Constants.GET_REFERRERS)) {
				value = String.format(Constants.GET_REFERRERS_EXPR, this.type);
			}
			String type = Constants.EXPRESSION_STRING + Constants.DELIMITER
					+ value;
			// locator for referrer will be the current PyVariableCollection
			referrers = new PyVariableCollection(this.target, "Referrers",
					type, value, this);
			if (networkState == NETWORK_REQUEST_ARRIVED) {
				addReferrersInVariables();
				target.fireEvent(new DebugEvent(this, DebugEvent.CHANGE,
						DebugEvent.STATE));
			}
		}
	}

	/**
	 * Overriding this method to communicate if any expression 
	 * needs to be evaluated in the variable view.
	 */
	public String getPyDBLocation() {
		if (type.startsWith(Constants.EXPRESSION_STRING)) {
			return locator.getPyDBLocation() + "\t" + type;
		} else {
			return super.getPyDBLocation();
		}
	}
    
	/**
	 * Add a new node (Referrers) to the existing variables list
	 */
	private void addReferrersInVariables() {
		PyVariable[] tempVariables = new PyVariable[variables.length + 1];
		tempVariables[0] = referrers;
		for (int i = 0; i < variables.length; i++) {
			tempVariables[i + 1] = variables[i];
		}
		synchronized (variables) {
			variables = tempVariables;
		}
	}

    private IVariable[] getWaitVariables() {
        if (waitVariables == null) {
            PyVariable waitVar = new PyVariable(target, "wait", "", "for network", locator);
            waitVariables = new IVariable[1];
            waitVariables[0] = waitVar;
        }
        return waitVariables;
    }
    
    public IVariable[] getTimedoutVariables() {
        return new IVariable[]{new PyVariable(target, "err:", "", "Timed out while getting var.", locator)};
    }

    /**
     * Received when the command has been completed.
     */
    public void commandComplete(AbstractDebuggerCommand cmd) {
        variables = getCommandVariables(cmd);
        if (referrers != null){
        	addReferrersInVariables();
        }
        networkState = NETWORK_REQUEST_ARRIVED;
        if (fireChangeEvent){
            target.fireEvent(new DebugEvent(this, DebugEvent.CHANGE, DebugEvent.STATE));
        }
    }

    public PyVariable[] getCommandVariables(AbstractDebuggerCommand cmd) {
        return getCommandVariables(cmd, target, this);
    }
    
    /**
     * @return a list of variables resolved for some command
     */
    public static PyVariable[] getCommandVariables(AbstractDebuggerCommand cmd, AbstractDebugTarget target, IVariableLocator locator) {
        PyVariable[] tempVariables = new PyVariable[0];
        try {
            String payload = ((GetVariableCommand) cmd).getResponse();
            tempVariables = XMLUtils.XMLToVariables(target, locator, payload);
        } catch (CoreException e) {
            tempVariables = new PyVariable[1];
            tempVariables[0] = new PyVariable(target, "Error", "pydev ERROR", "Could not resolve variable", locator);
            
            String msg = e.getMessage(); //we don't want to show this error
            if(msg == null || (msg.indexOf("Error resolving frame:") == -1 && msg.indexOf("from thread:") == -1)){
                PydevDebugPlugin.log(IStatus.ERROR, "Error fetching a variable", e);
            }
        }
        return tempVariables;
    }

    public IVariable[] getVariables() throws DebugException {
        if (networkState == NETWORK_REQUEST_ARRIVED){
            return variables;
        } else if (networkState == NETWORK_REQUEST_NOT_ARRIVED){
            return getWaitVariables();
        }

        // send the command, and then busy-wait
        GetVariableCommand cmd = getVariableCommand(target);
        cmd.setCompletionListener(this);
        networkState = NETWORK_REQUEST_NOT_ARRIVED;
        fireChangeEvent = false;    // do not fire change event while we are waiting on response
        target.postCommand(cmd);
        try {
            // VariablesView does not deal well with children changing asynchronously.
            // it causes unneeded scrolling, because view preserves selection instead
            // of visibility.
            // I try to minimize the occurrence here, by giving pydevd time to complete the
            // task before we are forced to do asynchronous notification.
            int i = 10; 
            while (--i > 0 && networkState != NETWORK_REQUEST_ARRIVED){
                Thread.sleep(50);
            }
            
        } catch (InterruptedException e) {
            Log.log(e);
        }
        fireChangeEvent = true;
        if (networkState == NETWORK_REQUEST_ARRIVED){
            return variables;
        }else{
            return getWaitVariables();
        }
    }

    
    public GetVariableCommand getVariableCommand(AbstractDebugTarget dbg) {
        return new GetVariableCommand(dbg, getPyDBLocation());
    }

    public boolean hasVariables() throws DebugException {
        return true;
    }
    
    public String getReferenceTypeName() throws DebugException {
        return type;
    }

	public AbstractDebugTarget getTarget() {
		return target;
	}
}
