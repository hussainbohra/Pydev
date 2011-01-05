package org.python.pydev.debug.model;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.debug.ui.actions.ISetNextTarget;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;
import org.python.pydev.parser.visitors.scope.OutlineCreatorVisitor;
import org.python.pydev.plugin.PydevPlugin;

public class PySetNextTarget implements ISetNextTarget {

	@Override
	public boolean canSetNextToLine(IWorkbenchPart part, ISelection selection,
			ISuspendResume target) {
		return true;
	}

	@Override
	public boolean setNextToLine(IWorkbenchPart part, ISelection selection,
			ISuspendResume target) throws CoreException {
		// System.out.println("Run to line:"+target);
		PyStackFrame stack = null;
		if (target instanceof PyStackFrame) {
			stack = (PyStackFrame) target;
			target = stack.getThread();
		}

		if (!(part instanceof PyEdit)) {
			return false;
		}

		PyEdit pyEdit = (PyEdit) part;
		SimpleNode ast = pyEdit.getAST();
		if (ast == null) {
			IDocument doc = pyEdit.getDocument();
			SourceModule sourceModule;
			IPythonNature nature = null;
			try {
				nature = pyEdit.getPythonNature();
			} catch (MisconfigurationException e) {
				// Let's try to find a suitable nature
				File editorFile = pyEdit.getEditorFile();
				if (editorFile == null || !editorFile.exists()) {
					PydevPlugin.log(e);
					return false;
				}
				nature = PydevPlugin.getInfoForFile(editorFile).o1;
			}

			if (nature == null) {
				PydevPlugin.log("Unable to determine nature!");
				return false;
			}

			try {
				sourceModule = (SourceModule) AbstractModule
						.createModuleFromDoc("", null, doc, nature, -1);
			} catch (MisconfigurationException e) {
				PydevPlugin.log(e);
				return false;
			}
			ast = sourceModule.getAst();
		}

		if (ast == null) {
			PydevPlugin.log("Cannot determine context to run to.");
			return false;
		}

		if (target instanceof PyThread && selection instanceof ITextSelection) {
			ITextSelection textSelection = (ITextSelection) selection;
			PyThread pyThread = (PyThread) target;
			if (!pyThread.isPydevThread()) {
				int sourceLine = stack.getLineNumber();
				int targetLine = textSelection.getStartLine();
				if (!NodeUtils.isValidContextForSetNext(ast, sourceLine, targetLine)) {
					return false;
				}
				String functionName = NodeUtils.getContextName(targetLine, ast);
				if (functionName == null) {
					functionName = ""; // global context
				} else {
					functionName = FullRepIterable.getLastPart(functionName)
							.trim();
				}
				pyThread.setNextStatement(targetLine + 1, functionName);
				return true;
			}
		}
		return false;
	}

	
}
