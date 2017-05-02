package com.jiajieshen.plugins.viewholdergenerator;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.jiajieshen.plugins.viewholdergenerator.model.Element;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by xin on 4/28/17.
 */
public class ViewHolderWriter extends WriteCommandAction.Simple {

    private static final String VIEW_HOLDER_CLASS_NAME = "ViewHolder";
    private static
    final String RECYCLER_VIEW_HOLDER_QUALIFIED_NAME = "android.support.v7.widget.RecyclerView.ViewHolder";
    private static final String RECYCLER_VIEW_HOLDER_SIMPLE_NAME = "ViewHolder";

    protected PsiFile mFile;
    protected Project mProject;
    protected PsiClass mClass;
    protected ArrayList<Element> mElements;
    protected PsiElementFactory mFactory;
    protected String mLayoutFileName;

    protected ViewHolderWriter(PsiFile file,
                               PsiClass clazz,
                               String command,
                               ArrayList<Element> elements,
                               String layoutFileName) {
        super(clazz.getProject(), command);

        mFile = file;
        mProject = clazz.getProject();
        mClass = clazz;
        mFactory = JavaPsiFacade.getElementFactory(mProject);
        mLayoutFileName = layoutFileName;
        Iterator<Element> elementIterator = elements.iterator();
        Element element;
        while (elementIterator.hasNext()) {
            element = elementIterator.next();
            if (!element.used) {
                elementIterator.remove();
            }
        }
        mElements = elements;
    }

    @Override
    protected void run() throws Throwable {

        generateViewHolder();

        // 格式化代码
        CodeStyleManager.getInstance(mProject).reformat(mClass);
    }

    private void generateViewHolder() {
        // view holder class
        StringBuilder holderBuilder = new StringBuilder();
        holderBuilder.append(VIEW_HOLDER_CLASS_NAME);
        holderBuilder.append("(View itemView) {");
        holderBuilder.append("super(itemView);");
        holderBuilder.append("}");

        PsiClass viewHolder = mFactory.createClassFromText(holderBuilder.toString(), mClass);
        viewHolder.setName(VIEW_HOLDER_CLASS_NAME);
        viewHolder.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
        viewHolder.getModifierList().setModifierProperty(PsiModifier.STATIC, true);

        // extends RecyclerView.ViewHolder
        GlobalSearchScope searchScope = GlobalSearchScope.allScope(mProject);
        PsiClass[] psiClasses = PsiShortNamesCache.getInstance(mProject)
                .getClassesByName(RECYCLER_VIEW_HOLDER_SIMPLE_NAME, searchScope);
        PsiClass recyclerViewHolderClass = null;
        for (PsiClass psiClass : psiClasses) {
            if (RECYCLER_VIEW_HOLDER_QUALIFIED_NAME.equals(psiClass.getQualifiedName())) {
                recyclerViewHolderClass = psiClass;
            }
        }
        if (recyclerViewHolderClass != null) {
            PsiJavaCodeReferenceElement ref = mFactory.createClassReferenceElement(recyclerViewHolderClass);
            viewHolder.getExtendsList().add(ref);
        }

        // add fields
        for (Element element : mElements) {
            StringBuilder sb = new StringBuilder();
            sb.append(element.name);
            sb.append(" ");
            sb.append(element.fieldName);
            sb.append(";");

            PsiField psiField = mFactory.createFieldFromText(sb.toString(), viewHolder);
            psiField.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
            viewHolder.add(psiField);
        }

        // add findViewById
        PsiMethod constructor = viewHolder.getConstructors()[0];
        if (constructor != null) {
            for (Element element : mElements) {
                StringBuilder sb = new StringBuilder();
                sb.append(element.fieldName);
                sb.append("=(");
                sb.append(element.name);
                sb.append(")itemView.findViewById(R.id.");
                sb.append(element.id);
                sb.append(");");

                PsiStatement statement = mFactory.createStatementFromText(sb.toString(), viewHolder);
                constructor.getBody().add(statement);
            }
        }

        mClass.add(viewHolder);
    }
}
