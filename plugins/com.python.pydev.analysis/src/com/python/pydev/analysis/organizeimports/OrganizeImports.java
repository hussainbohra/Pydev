/*
 * Created on 01/06/2008
 */
package com.python.pydev.analysis.organizeimports;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.IOrganizeImports;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.CtxInsensitiveImportComplProposal;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.builder.AnalysisParserObserver;
import com.python.pydev.analysis.builder.AnalysisRunner;
import com.python.pydev.analysis.ctrl_1.UndefinedVariableFixParticipant;
import com.python.pydev.analysis.ui.AutoImportsPreferencesPage;

/**
 * Used to present the user a way to add imports for the undefined variables.
 *
 * @author Fabio
 */
public class OrganizeImports implements IOrganizeImports{

    private static final String DIALOG_SETTINGS = "com.python.pydev.analysis.ORGANIZE_IMPORTS_DIALOG"; //$NON-NLS-1$;
    
    /**
     * That's where everything happens.
     * 
     * Important: if the document is in a rewrite session, trying to highlight a given session does not work
     * (so, we cannot be in a rewrite session in this case).
     */
    public boolean beforePerformArrangeImports(final PySelection ps, final PyEdit edit) {
        if(!AutoImportsPreferencesPage.doAutoImportOnOrganizeImports()){
            return true;
        }
        ArrayList<MarkerAnnotationAndPosition> undefinedVariablesMarkers = getUndefinedVariableMarkers(edit);
        

        //sort them
        TreeMap<Integer, MarkerAnnotationAndPosition> map = new TreeMap<Integer, MarkerAnnotationAndPosition>();
        
        for (MarkerAnnotationAndPosition marker : undefinedVariablesMarkers) {
            int start = marker.position.offset;
            map.put(start, marker);
        }
        
        //create the participant that'll help (will not force a reparse)
        final UndefinedVariableFixParticipant variableFixParticipant = new UndefinedVariableFixParticipant(false);
        
        //These are the completions to apply. We must apply them all at once after finishing it because we can't do
        //it one by one during the processing because that'd make markers change.
        final List<ICompletionProposalExtension2> completionsToApply = new ArrayList<ICompletionProposalExtension2>(); 
        
        
        //keeps the strings we've already treated.
        final HashSet<String> treatedVars = new HashSet<String>();

        //variable to hold whether we should keep on choosing the imports
        final Boolean[] keepGoing = new Boolean[]{true}; 
        
        
        final IDialogSettings dialogSettings = AnalysisPlugin.getDefault().getDialogSettings();
        
        //analyse the markers (one by one)
        for (final MarkerAnnotationAndPosition marker : map.values()) {
            if(!keepGoing[0]){
                break;
            }
            try {
                
                final int start = marker.position.offset;
                final int end = start+marker.position.length;
                
                
                if(start >= 0 && end > start){
                    IDocument doc = ps.getDoc();
                    ArrayList<ICompletionProposal> props = new ArrayList<ICompletionProposal>();
                    try {
                        String string = doc.get(start, end-start);
                        if(treatedVars.contains(string)){
                            continue;
                        }
                        variableFixParticipant.addProps(marker, null, null, ps, start, 
                                edit.getPythonNature(), edit, props);
                        
                        if(props.size() > 0){
                            edit.selectAndReveal(start, end-start);
                            treatedVars.add(string);
                            Shell activeShell = Display.getCurrent().getActiveShell();
                            
                            ElementListSelectionDialog dialog= new ElementListSelectionDialog(activeShell, 
                                    new LabelProvider(){
                                
                                //get the image and text for each completion
                                
                                @Override
                                public Image getImage(Object element) {
                                    CtxInsensitiveImportComplProposal comp = ((CtxInsensitiveImportComplProposal)element);
                                    return comp.getImage();
                                }
                                
                                @Override
                                public String getText(Object element) {
                                    CtxInsensitiveImportComplProposal comp = ((CtxInsensitiveImportComplProposal)element);
                                    return comp.getDisplayString();
                                }
                                
                            }) {
                                
                                //override things to return the last position of the dialog correctly

                                /**
                                 * @see org.eclipse.ui.dialogs.SelectionDialog#getDialogBoundsSettings()
                                 */
                                protected IDialogSettings getDialogBoundsSettings() {
                                    IDialogSettings section = dialogSettings.getSection(DIALOG_SETTINGS);
                                    if (section == null) {
                                        section = dialogSettings.addNewSection(DIALOG_SETTINGS);
                                    }
                                    return section;
                                }

                                /* (non-Javadoc)
                                 * @see org.eclipse.jface.dialogs.Dialog#getInitialSize()
                                 */
                                protected Point getInitialSize() {
                                    IDialogSettings settings = getDialogBoundsSettings();
                                    if (settings != null) {
                                        try {
                                            int width = settings.getInt("DIALOG_WIDTH"); //$NON-NLS-1$
                                            int height = settings.getInt("DIALOG_HEIGHT"); //$NON-NLS-1$
                                            if (width > 0 & height > 0) {
                                                return new Point(width, height);
                                            }
                                        } catch (NumberFormatException nfe) {
                                            //make the default return
                                        }
                                    }
                                    return new Point(300, 300);
                                }
                            };
                            
                            dialog.setTitle("Choose import");
                            dialog.setMessage("Which import should be added?");
                            dialog.setElements(props.toArray());
                            int returnCode = dialog.open();
                            if (returnCode == Window.OK) {
                                ICompletionProposalExtension2 firstResult = (ICompletionProposalExtension2) 
                                    dialog.getFirstResult();
                                
                                completionsToApply.add(firstResult);
                            }else if(returnCode == Window.CANCEL){
                                keepGoing[0] = false;
                                continue;
                            }

                        }
                    } catch (Exception e) {
                        PydevPlugin.log(e);
                    }
                }
                
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }
        

        for (ICompletionProposalExtension2 comp : completionsToApply) {
            int offset = 0; //the offset is not used in this case, because the actual completion does nothing,
                            //we'll only add the import.
            comp.apply(edit.getPySourceViewer(), ' ', 0, offset);
        }

        return true;

    }

    /**
     * @return the markers representing undefined variables found in the editor.
     */
    private ArrayList<MarkerAnnotationAndPosition> getUndefinedVariableMarkers(final PyEdit edit) {
        PySourceViewer s = edit.getPySourceViewer();
        
        ArrayList<MarkerAnnotationAndPosition> undefinedVariablesMarkers = new ArrayList<MarkerAnnotationAndPosition>();
        
        //get the markers we are interested in (undefined variables)
        for(Iterator<MarkerAnnotationAndPosition> it=s.getMarkerIterator();it.hasNext();){
            MarkerAnnotationAndPosition m = it.next();
            IMarker marker = m.markerAnnotation.getMarker();
            try {
                String type = marker.getType();
                if(type != null && type.equals(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER)){
                    Integer attribute = marker.getAttribute(AnalysisRunner.PYDEV_ANALYSIS_TYPE, -1 );
                    if (attribute != null && attribute.equals(IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE)){
                        undefinedVariablesMarkers.add(m);
                    }

                }
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }
        return undefinedVariablesMarkers;
    }

    /**
     * After all the imports are arranged, let's ask for a reparse of the document
     */
    public void afterPerformArrangeImports(PySelection ps, PyEdit pyEdit) {
        if(!AutoImportsPreferencesPage.doAutoImportOnOrganizeImports()){
            return;
        }
        if(pyEdit != null){
            PyParser parser = pyEdit.getParser();
            if(parser != null){
                parser.forceReparse(new Tuple<String, Boolean>(
                    AnalysisParserObserver.ANALYSIS_PARSER_OBSERVER_FORCE, true));
            }
        }

    }


}
