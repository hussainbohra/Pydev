/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.ui.actions.ISetNextTarget;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Hussain Bohra
 */
public class PySetNextTarget implements ISetNextTarget {

	public boolean canSetNextToLine(IWorkbenchPart part, ISelection selection,
			ISuspendResume target) {
		return true;
	}

	public boolean setNextToLine(IWorkbenchPart part, int targetLine,
			ISuspendResume target) throws CoreException {
		PyStackFrame stack = null;
		if (target instanceof PyStackFrame) {
			stack = (PyStackFrame) target;
			target = stack.getThread();
		}

		if (!(part instanceof PyEdit)) {
			return false;
		}

		SimpleNode ast = getAST(part);
		if (ast == null) {
			return false;
		}

		if (target instanceof PyThread) {
			PyThread pyThread = (PyThread) target;
			if (!pyThread.isPydevThread()) {
				int sourceLine = stack.getLineNumber();
				return executeSetNextStatement(targetLine, ast, pyThread,
						sourceLine);
			}
		}
		return true;
	}

	public boolean setNextToLine(IWorkbenchPart part, ISelection selection,
			ISuspendResume target) throws CoreException {
		PyStackFrame stack = null;
		if (target instanceof PyStackFrame) {
			stack = (PyStackFrame) target;
			target = stack.getThread();
		}

		if (!(part instanceof PyEdit)) {
			return false;
		}

		SimpleNode ast = getAST(part);
		if (ast == null) {
			return false;
		}

		if (target instanceof PyThread && selection instanceof ITextSelection) {
			ITextSelection textSelection = (ITextSelection) selection;
			PyThread pyThread = (PyThread) target;
			if (!pyThread.isPydevThread()) {
				int sourceLine = stack.getLineNumber();
				int targetLine = textSelection.getStartLine();
				return executeSetNextStatement(targetLine, ast, pyThread,
						sourceLine);
			}
		}
		return true;
	}

	/**
	 * Identify an AST from the IWorkbenchPart
	 *
	 * @param part
	 * @return
	 */
	private SimpleNode getAST(IWorkbenchPart part) {
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
					Log.log(e);
					return null;
				}
				nature = PydevPlugin.getInfoForFile(editorFile).o1;
			}

			if (nature == null) {
				Log.log("Unable to determine nature!");
				return null;
			}

			try {
				sourceModule = (SourceModule) AbstractModule
						.createModuleFromDoc("", null, doc, nature, true);
			} catch (MisconfigurationException e) {
				Log.log(e);
				return null;
			}
			ast = sourceModule.getAst();
		}

		if (ast == null) {
			Log.log("Cannot determine context to run to.");
			return null;
		}
		return ast;
	}

	/**
	 * Validate the context of target line and accordingly set the instruction
	 * pointer on the target line
	 *
	 * @param targetLine
	 * @param ast
	 * @param pyThread
	 * @param sourceLine
	 * @return
	 */
	private boolean executeSetNextStatement(int targetLine, SimpleNode ast,
			PyThread pyThread, int sourceLine) {
		boolean verifyExecutionContext = PydevPlugin.getDefault().getPreferenceStore().getBoolean(PydevEditorPrefs.BLOCK_SET_NEXT);
		if (!NodeUtils.isValidContextForSetNext(ast, sourceLine, targetLine, verifyExecutionContext)) {
			return false;
		}
		String functionName = NodeUtils.getContextName(targetLine, ast);
		if (functionName == null) {
			functionName = ""; // global context
		} else {
			functionName = FullRepIterable.getLastPart(functionName).trim();
		}
		pyThread.setNextStatement(targetLine + 1, functionName);
		return true;
	}
}
