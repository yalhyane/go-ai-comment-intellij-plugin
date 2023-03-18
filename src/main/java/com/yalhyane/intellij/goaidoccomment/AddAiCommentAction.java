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
import com.yalhyane.intellij.goaidoccomment.settings.AppSettingsState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class AddAiCommentAction extends AnAction  {

    private AppSettingsState settings;
    private OkHttpChatGptApi chatGptAPI;

    public AddAiCommentAction() {
        super();
        this.settings = AppSettingsState.getInstance();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        System.out.println("Token: " + this.settings.chatgptToken);

        Project project = event.getRequiredData(CommonDataKeys.PROJECT);
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        CaretModel caret = editor.getCaretModel();
        Document doc = editor.getDocument();
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        if (editor == null || psiFile == null) {
            return;
        }

        PsiElement element;
        String blockCode;
        String blockName;
        String blockType;

        // handle selection
        if (editor.getSelectionModel().hasSelection()) {
            blockType = "code";
            blockCode = editor.getSelectionModel().getSelectedText();
            element = psiFile.findElementAt(editor.getSelectionModel().getSelectionStart());
            blockName = "";
        } else {
            // handle function or method
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

            blockName = pe.getName();
            blockCode = pe.getText();
            blockType = "function";
            element = pe;
            // find first comment before whitespace

        }


        if (element == null) {
            System.out.println("No code block detected");
            return;
        }
        ArrayList<PsiElement> dropElements =new ArrayList<PsiElement>();
        PsiElement prevElement = element.getPrevSibling();
        Boolean skipFirst = false;
        while (prevElement instanceof PsiWhiteSpace) {
            if (!skipFirst) {
              //  dropElements.add(prevElement);
            }
            //skipFirst = true;
            prevElement = prevElement.getPrevSibling();
        }



        PsiComment psiComment;
        if (prevElement instanceof PsiComment) {
            psiComment = (PsiComment) prevElement;
        } else {
            psiComment = null;
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
            // delete whitespaces
            for (PsiElement el : dropElements) {
                el.delete();
            }
            String comment = this.getComment(blockName, blockCode, blockType);
            System.out.println("Comment to add: " + comment);
            PsiParserFacade factory = PsiParserFacade.SERVICE.getInstance(project);
            PsiComment newPsiComment = factory.createLineCommentFromText(GoLanguage.INSTANCE, comment);
            if (psiComment != null) {
                psiComment.replace(newPsiComment);
            } else {
                System.out.println("pe.getParent(): " + element.getParent().getClass());
                element.getParent().addBefore(newPsiComment, element);
            }
        });

    }

    private String getComment(String funcName, String funcBody, String blockType) {
        chatGptAPI = new OkHttpChatGptApi(settings.chatgptToken);
        String prompt = getPrompt(funcBody, blockType);
        try {
            String comment = chatGptAPI.completion(prompt);

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

    private String getPrompt(String blockCode, String blockType) {
        return "Write an insightful but concise comment in a complete sentence"
                .concat("in present tense for the following")
                .concat("Golang " + blockType + " without prefacing it with anything,")
                .concat("the response must be in the language english")
                .concat(":\n")
                .concat(blockCode);
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
        e.getPresentation().setEnabled(editor != null && psiFile != null && this.settings.chatgptToken != null);
    }
}
