package org.python.pydev.debug.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.AbstractPydevPrefs;
import org.python.pydev.utils.LabelFieldEditor;

public class SetNextPrefsPage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	/**
	 * Initializer sets the preference store
	 */
	public SetNextPrefsPage() {
		super("Set Next Statement", GRID);
		setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
	}

	public void init(IWorkbench workbench) {

	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		addField(new BooleanFieldEditor(
				AbstractPydevPrefs.BLOCK_SET_NEXT,
				"Block execution of Set Next Statement in any context where try..except..finally exists",
				AbstractPydevPrefs.DEFAULT_BLOCK_SET_NEXT, parent));
		addField(new LabelFieldEditor(
				"NOTE",
				"\nNote: In stackless python, executing set next statement in the context of try..except..finally crashes the application."
						+ "\nHence select the above checkbox if you are using stackless python.",
				parent));
	}

}
