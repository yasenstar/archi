package com.archimatetool.csv.export;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.archimatetool.csv.CSVConstants;
import com.archimatetool.csv.CSVImportExportPlugin;
import com.archimatetool.csv.IPreferenceConstants;
import com.archimatetool.editor.ui.IArchiImages;
import com.archimatetool.editor.ui.UIUtils;
import com.archimatetool.editor.utils.StringUtils;



/**
 * Export to CSV Wizard Page
 * 
 * @author Phillip Beauvoir
 */
public class ExportAsCSVPage extends WizardPage implements IPreferenceConstants, CSVConstants {

    private static String HELP_ID = "com.archimatetool.help.ExportAsCSVPage"; //$NON-NLS-1$
    
    private Text fFileTextField;
    private Combo fDelimiterCombo;
    private Button fStripNewlinesButton;
    private Button fLeadingCharsButton;
    private Button fWriteHeaderButton;
    private Combo fEncodingCombo;
    
    public ExportAsCSVPage() {
        super("ExportAsCSVPage"); //$NON-NLS-1$
        
        setTitle(Messages.ExportAsCSVPage_0);
        setDescription(Messages.ExportAsCSVPage_1);
        setImageDescriptor(IArchiImages.ImageFactory.getImageDescriptor(IArchiImages.ECLIPSE_IMAGE_EXPORT_DIR_WIZARD));
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout());
        setControl(container);
        
        PlatformUI.getWorkbench().getHelpSystem().setHelp(container, HELP_ID);
        
        Group exportGroup = new Group(container, SWT.NULL);
        exportGroup.setText(Messages.ExportAsCSVPage_2);
        exportGroup.setLayout(new GridLayout(3, false));
        exportGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        Label label = new Label(exportGroup, SWT.NULL);
        label.setText(Messages.ExportAsCSVPage_3);
        
        fFileTextField = new Text(exportGroup, SWT.BORDER | SWT.SINGLE);
        fFileTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        // Single text control so strip CRLFs
        UIUtils.conformSingleTextControl(fFileTextField);
        
        fFileTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                validateFields();
            }
        });
        
        Button fileButton = new Button(exportGroup, SWT.PUSH);
        fileButton.setText(Messages.ExportAsCSVPage_4);
        fileButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String file = chooseFilePath();
                if(file != null) {
                    fFileTextField.setText(file);
                }
            }
        });
        
        label = new Label(exportGroup, SWT.NULL);
        label.setText(Messages.ExportAsCSVPage_5);
        
        fDelimiterCombo = new Combo(exportGroup, SWT.READ_ONLY);
        fDelimiterCombo.setItems(DELIMITER_NAMES);
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        fDelimiterCombo.setLayoutData(gd);
        
        // Encoding
        label = new Label(exportGroup, SWT.NULL);
        label.setText(Messages.ExportAsCSVPage_15);
        fEncodingCombo = new Combo(exportGroup, SWT.READ_ONLY);
        fEncodingCombo.setItems(ENCODINGS);
        
        Group optionsGroup = new Group(container, SWT.NULL);
        optionsGroup.setText(Messages.ExportAsCSVPage_7);
        optionsGroup.setLayout(new GridLayout(1, false));
        optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        // Strip new lines
        fStripNewlinesButton = new Button(optionsGroup, SWT.CHECK);
        fStripNewlinesButton.setText(Messages.ExportAsCSVPage_8);
        
        // Leding chars
        // See http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm#CSVAndExcel
        fLeadingCharsButton = new Button(optionsGroup, SWT.CHECK);
        fLeadingCharsButton.setText(Messages.ExportAsCSVPage_9);
        
        // Write header
        fWriteHeaderButton = new Button(optionsGroup, SWT.CHECK);
        fWriteHeaderButton.setText(Messages.ExportAsCSVPage_6);
        
        label = new Label(container, SWT.NULL);
        
        loadPreferences();
        
        // Validate our fields
        validateFields();
    }
    
    /**
     * @return The export file path
     */
    String getExportFilePath() {
        return fFileTextField.getText();
    }
    
    /**
     * @return The delimiter index
     */
    int getDelimiterIndex() {
        return fDelimiterCombo.getSelectionIndex();
    }
    
    boolean getStripNewlines() {
        return fStripNewlinesButton.getSelection();
    }
    
    boolean getUseLeadingCharsHack() {
        return fLeadingCharsButton.getSelection();
    }
    
    boolean getWriteHeader() {
        return fWriteHeaderButton.getSelection();
    }

    String getEncoding() {
        return fEncodingCombo.getText();
    }

    private String chooseFilePath() {
        FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
        dialog.setText(Messages.ExportAsCSVPage_11);
        dialog.setFilterExtensions(new String[] { FILE_EXTENSION_WILDCARD, "*.*" } ); //$NON-NLS-1$ 
        dialog.setFileName(getExportFilePath());
        return dialog.open();
    }

    private void validateFields() {
        // File path
        String filePath = getExportFilePath();
        if(!StringUtils.isSetAfterTrim(filePath)) {
            setErrorMessage(Messages.ExportAsCSVPage_13);
            return;
        }
        
        if(new File(filePath).isDirectory()) {
            setErrorMessage(Messages.ExportAsCSVPage_14);
            return;
        }
        
        // Check valid file name
        try {
            FileSystems.getDefault().getPath(filePath);
        }
        catch(InvalidPathException ex) {
            setErrorMessage(Messages.ExportAsCSVPage_14);
            return;
        }
        
        setErrorMessage(null);
    }

    /**
     * Update the page status
     */
    @Override
    public void setErrorMessage(String message) {
        super.setErrorMessage(message);
        setPageComplete(message == null);
    }

    void storePreferences() {
        IPreferenceStore store = CSVImportExportPlugin.getDefault().getPreferenceStore();
        store.setValue(CSV_EXPORT_PREFS_LAST_FILE, new File(getExportFilePath()).getPath());
        store.setValue(CSV_EXPORT_PREFS_SEPARATOR, getDelimiterIndex());
        store.setValue(CSV_EXPORT_PREFS_STRIP_NEW_LINES, getStripNewlines());
        store.setValue(CSV_EXPORT_PREFS_LEADING_CHARS_HACK, getUseLeadingCharsHack());
        store.setValue(CSV_EXPORT_PREFS_ENCODING, getEncoding());
        store.setValue(CSV_EXPORT_PREFS_WRITE_HEADER, getWriteHeader());
    }
    
    void loadPreferences() {
        IPreferenceStore store = CSVImportExportPlugin.getDefault().getPreferenceStore();
        
        // Last saved file
        String lastFilePath = store.getString(CSV_EXPORT_PREFS_LAST_FILE);
        if(lastFilePath != null && !"".equals(lastFilePath)) { //$NON-NLS-1$
            fFileTextField.setText(lastFilePath);
        }
        else {
            fFileTextField.setText(new File(System.getProperty("user.home"), "export.csv").getPath()); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // Delimiter
        int separator = store.getInt(CSV_EXPORT_PREFS_SEPARATOR);
        if(separator > -1 && separator < DELIMITER_NAMES.length) {
            fDelimiterCombo.setText(DELIMITER_NAMES[separator]);
        }
        
        // Strip newlines
        boolean selected = store.getBoolean(CSV_EXPORT_PREFS_STRIP_NEW_LINES);
        fStripNewlinesButton.setSelection(selected);
        
        // Leading chars hack
        selected = store.getBoolean(CSV_EXPORT_PREFS_LEADING_CHARS_HACK);
        fLeadingCharsButton.setSelection(selected);
        
        // Encoding
        String encoding = store.getString(CSV_EXPORT_PREFS_ENCODING);
        fEncodingCombo.setText(encoding);
        
        // Write Header
        selected = store.getBoolean(CSV_EXPORT_PREFS_WRITE_HEADER);
        fWriteHeaderButton.setSelection(selected);
    }
}
