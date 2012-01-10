package org.python.pydev.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.ITextEditor;

public class PythonSetNextRulerAction extends Action {

    protected IVerticalRulerInfo fInfo;
    protected ITextEditor fTextEditor;

    public PythonSetNextRulerAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
    	setInfo(rulerInfo);
        setTextEditor(editor);
        setText("Set Next Statement");
    }

    protected void setTextEditor(ITextEditor textEditor) {
        fTextEditor = textEditor;
    }
    
    protected void setInfo(IVerticalRulerInfo info) {
        fInfo = info;
    }

	public void run() {
        IWorkbenchWindow window = this.fTextEditor.getSite().getWorkbenchWindow();
        RetargetSetNextAction setNextAction = new RetargetSetNextAction();
        setNextAction.init(window);
        try {
            setNextAction.performAction(this.fInfo.getLineOfLastMouseButtonActivity());
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }
}
