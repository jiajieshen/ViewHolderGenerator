package com.jiajieshen.plugins.viewholdergenerator.iface;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.jiajieshen.plugins.viewholdergenerator.model.Element;

import java.util.ArrayList;

public interface IConfirmListener {

    void onConfirm(Project project, Editor editor, ArrayList<Element> elements);
}
