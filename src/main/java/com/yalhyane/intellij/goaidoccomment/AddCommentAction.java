package com.yalhyane.intellij.goaidoccomment;

import com.goide.GoLanguage;
import com.goide.psi.GoFunctionOrMethodDeclaration;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class AddCommentAction extends AnAction  {

    private OkHttpChatGptApi chatGptAPI;

    public AddCommentAction() {
        super();
        this.chatGptAPI = new OkHttpChatGptApi("API_KEY");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getRequiredData(CommonDataKeys.PROJECT);
        Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);
        CaretModel caret = editor.getCaretModel();
        Document doc = editor.getDocument();
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (editor == null || psiFile == null) {
            return;
        }
        System.out.println("Caret at offset: " + caret.getOffset());

        PsiElement element1 = psiFile.findElementAt(caret.getOffset());
        if (element1 == null) {
            System.out.println("Could not detect caret");
            return;
        }

        GoFunctionOrMethodDeclaration pe = PsiTreeUtil.getParentOfType(element1, GoFunctionOrMethodDeclaration.class);
        if (pe == null) {
            System.out.println("Not in function declaration");
            return;
        }

        String funcName = pe.getName();
        String funcBody = pe.getText();
        System.out.println("GO Function Name: " + funcName);
        // System.out.println("GO Function Text: " + funcBody);

        // find first comment before whitespace
        PsiElement prevElement = pe.getPrevSibling();
        while (prevElement instanceof PsiWhiteSpace) {
            System.out.println("Function prev: " + prevElement.getClass());
            prevElement = prevElement.getPrevSibling();
        }

        PsiComment psiComment;
        if (prevElement instanceof PsiComment) {
            // System.out.println("Function has docs: " + prevElement.getText());
            psiComment = (PsiComment) prevElement;
        } else {
            psiComment = null;
            // System.out.println("Function prev: " + prevElement.getClass());
        }

//        psiFile.accept(new GoRecursiveVisitor(){
////            @Override
////            public void visitElement(@NotNull GoElement o) {
////                System.out.println("GoElement: " + o.getClass());
////            }
//
//            @Override
//            public void visitElement(@NotNull PsiElement e) {
//                if (e instanceof PsiComment) {
//                    System.out.println("PsiElement: " + e.getClass());
//                    System.out.println("Type: " + ((PsiComment) e).getTokenType().getClass());
//                    // System.out.println("Text: " + ((PsiComment) e).getText());
//
//                }
//            }
//        });

        // String selectedText = editor.getSelectionModel().getSelectedText();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            String comment = this.getComment(funcName, funcBody);
            System.out.println("Comment to add: " + comment);
            PsiParserFacade factory = PsiParserFacade.SERVICE.getInstance(project);
            PsiComment newPsiComment = factory.createLineCommentFromText(GoLanguage.INSTANCE, comment);
            if (psiComment != null) {
                psiComment.replace(newPsiComment);
            } else {
                System.out.println("pe.getParent(): " + pe.getParent().getClass());
                pe.getParent().addBefore(newPsiComment, pe);
            }
        });

    }

    private String getComment(String funcName, String funcBody) {
        String prompt = getPrompt(funcBody);
        try {
            String comment = this.chatGptAPI.completion(prompt);

            comment = comment.trim();
            if (comment.startsWith("\"")) {
                comment = comment.substring(1);
            }
            if (comment.endsWith("\"")) {
                comment = comment.substring(0, comment.length() - 1);
            }
            return " " + funcName + " " + comment;
        } catch (Exception e) {
            System.out.println(e.toString() + "\n" + e.getStackTrace().toString());
            return "";
        }
    }

    private String getPrompt(String funcBody) {
        return "Write an insightful but concise comment in a complete sentence"
                .concat("in present tense for the following")
                .concat("Golang function without prefacing it with anything,")
                .concat("the response must be in the language english")
                .concat(":\n")
                .concat(funcBody);
    }

    @Override
    public boolean isDumbAware() {
        return super.isDumbAware();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);

        // Set the availability based on whether a project is open
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(editor != null && psiFile != null);
    }
}
