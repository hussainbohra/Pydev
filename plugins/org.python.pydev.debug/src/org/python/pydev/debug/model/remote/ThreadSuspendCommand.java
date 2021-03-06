/*
 * Author: atotic
 * Created on Apr 22, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * Suspend thread network command.
 * 
 * See protocol docs for more info.
 */
public class ThreadSuspendCommand extends AbstractDebuggerCommand {

    String thread;
    
    public ThreadSuspendCommand(AbstractDebugTarget debugger, String thread) {
        super(debugger);
        this.thread = thread;
    }

    public String getOutgoing() {
        return makeCommand(CMD_THREAD_SUSPEND, sequence, thread);
    }
}
