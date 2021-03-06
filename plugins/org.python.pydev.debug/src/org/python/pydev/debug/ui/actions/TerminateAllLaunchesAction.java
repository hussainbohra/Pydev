package org.python.pydev.debug.ui.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.ui.texteditor.IUpdate;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.KeyBindingHelper;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

public class TerminateAllLaunchesAction extends PyAction implements IUpdate{

    public TerminateAllLaunchesAction() {
        KeySequence binding = KeyBindingHelper.getCommandKeyBinding("org.python.pydev.debug.ui.actions.terminateAllLaunchesAction");
        String str = binding != null?"("+binding.format()+" when on Pydev editor)":"(unbinded)";
        
        this.setImageDescriptor(PydevPlugin.getImageCache().getDescriptor(UIConstants.TERMINATE_ALL));
        this.setToolTipText("Terminate ALL."+ str);
        
        update();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.IUpdate#update()
     */
    public void update() {
        ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
        try {
            for (ILaunch iLaunch : launches) {
                if(!iLaunch.isTerminated()){
                    setEnabled(true);
                    return;
                }
            }
            setEnabled(false);
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
    }

    
    public void run(IAction action) {
        Job job = new Job("Terminate all Launches") {
            
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
                for (ILaunch iLaunch : launches) {
                    try {
                        if(!iLaunch.isTerminated()){
                            iLaunch.terminate();
                        }
                    } catch (Exception e) {
                        PydevPlugin.log(e);
                    }
                }
                return Status.OK_STATUS;
            }
        };
        job.setPriority(Job.INTERACTIVE);
        job.schedule();
    }

    
    public void run() {
        run(this);
    }

    public void dispose() {
        // TODO Auto-generated method stub
        
    }


}
