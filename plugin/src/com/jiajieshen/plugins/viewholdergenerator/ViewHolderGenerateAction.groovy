package com.jiajieshen.plugins.viewholdergenerator

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.generation.actions.BaseGenerateAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilBase
import com.jiajieshen.plugins.viewholdergenerator.form.EntryList
import com.jiajieshen.plugins.viewholdergenerator.iface.ICancelListener
import com.jiajieshen.plugins.viewholdergenerator.iface.IConfirmListener
import com.jiajieshen.plugins.viewholdergenerator.model.Element
import com.jiajieshen.plugins.viewholdergenerator.util.Utils
import groovy.transform.CompileStatic
import org.jetbrains.annotations.NotNull

import javax.swing.*

/**
 * Created by xin on 4/28/17.
 */
@CompileStatic
public class ViewHolderGenerateAction extends BaseGenerateAction implements IConfirmListener, ICancelListener {

    protected JFrame mDialog
    protected static final Logger log = Logger.getInstance(ViewHolderGenerateAction.class)

    public ViewHolderGenerateAction() {
        super(null)
    }

    public ViewHolderGenerateAction(CodeInsightActionHandler handler) {
        super(handler)
    }

    @Override
    protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return (super.isValidForFile(project, editor, file) && Utils.getLayoutFileFromCaret(editor, file) != null)
    }

    @Override
    protected boolean isValidForClass(PsiClass targetClass) {
        return super.isValidForClass(targetClass)
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        Editor editor = event.getData(PlatformDataKeys.EDITOR);

        actionPerformedImpl(project, editor);
    }

    @Override
    public void actionPerformedImpl(Project project, Editor editor) {
        PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
        PsiFile layout = Utils.getLayoutFileFromCaret(editor, file);

        if (layout == null) {
            Utils.showErrorNotification(project, "No layout found");
            return; // no layout found
        }

        log.info("Layout file: " + layout.getVirtualFile());

        ArrayList<Element> elements = Utils.getIDsFromLayout(layout);
        if (!elements.isEmpty()) {
            showDialog(project, editor, elements);
        } else {
            Utils.showErrorNotification(project, "No IDs found in layout");
        }
    }

    protected void showDialog(Project project, Editor editor, ArrayList<Element> elements) {
        PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (file == null) {
            return;
        }

        EntryList panel = new EntryList(project, editor, elements, null, this, this);

        mDialog = new JFrame();
        mDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        mDialog.getRootPane().setDefaultButton(panel.getConfirmButton());
        mDialog.getContentPane().add(panel);
        mDialog.pack();
        mDialog.setLocationRelativeTo(null);
        mDialog.setVisible(true);
    }

    @Override
    void onCancel() {
        closeDialog();
    }

    protected void closeDialog() {
        if (mDialog == null) {
            return;
        }
        mDialog.setVisible(false);
        mDialog.dispose();
    }

    @Override
    void onConfirm(Project project, Editor editor, ArrayList<Element> elements) {
        PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (file == null) {
            return;
        }
        PsiFile layout = Utils.getLayoutFileFromCaret(editor, file);

        closeDialog();

        int selectedCount = elements.count {
            it.used
        }
        .intValue();

        if (selectedCount > 0) {
            new ViewHolderWriter(file, getTargetClass(editor, file), "Generate Injections", elements, layout.getName()).execute();
        } else { // just notify user about no element selected
            Utils.showInfoNotification(project, "No injection was selected");
        }
    }
}
