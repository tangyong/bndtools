package bndtools.editor.workspace;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.lib.osgi.Constants;
import bndtools.Plugin;
import bndtools.api.IBndModel;

public class PluginPathPart extends SectionPart implements PropertyChangeListener {
    
    private TableViewer viewer;
    private IBndModel model;
    private List<String> data;
    private ToolItem removeItem;

    /**
     * Create the SectionPart.
     * @param parent
     * @param toolkit
     * @param style
     */
    public PluginPathPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        createClient(getSection(), toolkit);
    }

    /**
     * Fill the section.
     */
    private void createClient(Section section, FormToolkit toolkit) {
        section.setText("Plugin Path");
        
        createToolBar(section);
        
        Table table = new Table(section, SWT.BORDER | SWT.FULL_SELECTION);
        toolkit.adapt(table);
        toolkit.paintBordersFor(table);
        section.setClient(table);
        
        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new PluginPathLabelProvider());
        
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                boolean enable = !viewer.getSelection().isEmpty();
                removeItem.setEnabled(enable);
            }
        });
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if(e.character == SWT.DEL) {
                    doRemove();
                } else if(e.character == '+') {;
                    doAdd();
                }
            }
        });

    }
    
    private void createToolBar(Section section) {
        ToolBar toolbar = new ToolBar(section, SWT.FLAT);
        section.setTextClient(toolbar);
        
        ToolItem addItem = new ToolItem(toolbar, SWT.PUSH);
        addItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
        addItem.setToolTipText("Add Path");
        
        addItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doAdd();
            }
        });
        
        removeItem = new ToolItem(toolbar, SWT.PUSH);
        removeItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
        removeItem.setDisabledImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
        removeItem.setToolTipText("Remove");
        removeItem.setEnabled(false);

        removeItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doRemove();
            }
        });
    }

    @Override
    public void initialize(IManagedForm form) {
        super.initialize(form);

        model = (IBndModel) form.getInput();
        model.addPropertyChangeListener(Constants.PLUGINPATH, this);
    }

    @Override
    public void dispose() {
        super.dispose();
        if(model != null) model.removePropertyChangeListener(Constants.PLUGINPATH, this);
    }
    
    @Override
    public void refresh() {
        List<String> modelData = model.getPluginPath();
        if (modelData != null)
            this.data = new ArrayList<String>(modelData);
        else
            this.data = new LinkedList<String>();
        viewer.setInput(this.data);
        super.refresh();
    }
    
    @Override
    public void commit(boolean onSave) {
        super.commit(onSave);
        model.setPluginPath(data);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        IFormPage page = (IFormPage) getManagedForm().getContainer();
        if (page.isActive())
            refresh();
        else
            markStale();
    }
    
    IFile getEditorFile() {
        IFormPage page = (IFormPage) getManagedForm().getContainer();
        IFile file = ResourceUtil.getFile(page.getEditorInput());
        return file;
    }
    
    void doAdd() {
        IFile file = getEditorFile();
        
        FileDialog dialog = new FileDialog(getManagedForm().getForm().getShell());
        if (file != null)
            dialog.setFilterPath(file.getParent().getLocation().toString());
        dialog.setFilterExtensions(new String[] {"*.jar"}); //$NON-NLS-1$
        String res = dialog.open();
        if (res != null) {
            String[] fileNames = dialog.getFileNames();
            if (fileNames != null && fileNames.length > 0) {
                for (String fileName : fileNames)
                    data.add(fileName);
                viewer.add(fileNames);
                markDirty();
            }
        }
    }
    
    void doRemove() {
        IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();

        viewer.remove(sel.toArray());
        data.removeAll(sel.toList());

        if (!sel.isEmpty())
            markDirty();
    }
    
    private static final class PluginPathLabelProvider extends StyledCellLabelProvider {
        
        private final Image jarImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/jar_obj.gif").createImage();
        
        @Override
        public void update(ViewerCell cell) {
            String path = (String) cell.getElement();
            
            cell.setText(path);
            cell.setImage(jarImg);
        }
        
        @Override
        public void dispose() {
            super.dispose();
            jarImg.dispose();
        }
    }

}
