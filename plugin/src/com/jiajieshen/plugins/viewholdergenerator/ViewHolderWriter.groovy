package com.jiajieshen.plugins.viewholdergenerator

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.jiajieshen.plugins.viewholdergenerator.common.Definitions
import com.jiajieshen.plugins.viewholdergenerator.model.Element
import groovy.transform.CompileStatic

/**
 * Created by xin on 4/28/17.
 */
@CompileStatic
public class ViewHolderWriter extends WriteCommandAction.Simple {

    private static final String VIEW_HOLDER_CLASS_NAME = "ViewHolder";
    private static
    final String RECYCLER_VIEW_HOLDER_QUALIFIED_NAME = "android.support.v7.widget.RecyclerView.ViewHolder";
    private static final String RECYCLER_VIEW_HOLDER_SIMPLE_NAME = "ViewHolder";

    protected PsiFile mFile;
    protected Project mProject;
    protected PsiClass mClass;
    protected List<Element> mElements;
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
        mElements = elements.findAll() {
            it.used
        };
        mFactory = JavaPsiFacade.getElementFactory(mProject);
        mLayoutFileName = layoutFileName;
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
        viewHolder.modifierList.setModifierProperty(PsiModifier.PRIVATE, true);
        viewHolder.modifierList.setModifierProperty(PsiModifier.STATIC, true);

        // extends RecyclerView.ViewHolder
        GlobalSearchScope searchScope = GlobalSearchScope.allScope(project);
        PsiClass[] psiClasses = PsiShortNamesCache.getInstance(project)
                .getClassesByName(RECYCLER_VIEW_HOLDER_SIMPLE_NAME, searchScope);
        PsiClass recyclerViewHolderClass = psiClasses.find {
            it.qualifiedName == RECYCLER_VIEW_HOLDER_QUALIFIED_NAME;
        }
        if (recyclerViewHolderClass) {
            println recyclerViewHolderClass
            PsiJavaCodeReferenceElement ref = mFactory.createClassReferenceElement(recyclerViewHolderClass);
            viewHolder.getExtendsList().add(ref);
        }

        // add fields
        mElements.each { element ->
            StringBuilder sb = new StringBuilder();
            if (element.nameFull != null && element.nameFull.length() > 0) { // custom package+class
                sb.append(element.nameFull);
            } else if (Definitions.paths.containsKey(element.name)) { // listed class
                sb.append(Definitions.paths.get(element.name));
            } else { // android.widget
                sb.append("android.widget.");
                sb.append(element.name);
            }
            sb.append(" ");
            sb.append(element.fieldName);
            sb.append(";");

            PsiField psiField = mFactory.createFieldFromText(sb.toString(), viewHolder);
            psiField.modifierList.setModifierProperty(PsiModifier.PRIVATE, true);
            viewHolder.add(psiField);
        }

        // add findViewById
        PsiMethod constructor = viewHolder.constructors[0];
        mElements.each { element ->
            StringBuilder sb = new StringBuilder();
            sb.append(element.fieldName);
            sb.append("=(");
            sb.append(element.name);
            sb.append(")itemView.findViewById(R.id.");
            sb.append(element.id);
            sb.append(");");

            PsiStatement statement = mFactory.createStatementFromText(sb.toString(), viewHolder);
            constructor.body.add(statement);
        }

        mClass.add(viewHolder);
    }
}
