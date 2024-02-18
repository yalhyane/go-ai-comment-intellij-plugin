package com.yalhyane.intellij.goaidoccomment;

import com.goide.GoLanguage;
import com.goide.psi.GoFunctionOrMethodDeclaration;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.yalhyane.intellij.goaidoccomment.settings.AppSettingsState;
import com.yalhyane.intellij.goaidoccomment.settings.OpenSettingsAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class AddAiCommentAction extends AnAction  {

   // constants
    public static final String PLUGIN_ID = "com.yalhyane.intellij.goAiDocComment.go-ai-doc-comment";
    public static final String ACTION_ID = "Go.GenerateDocComment";
    private static final String MISSING_SETTINGS_NOTIFICATION_TITLE = "Missing settings";
    private static final String INVALID_BLOCK_NOTIFICATION_TITLE = "Invalid Block";
    private static final String UPDATE_SETTINGS_NOTIFICATION_CONTENT = "Please configure openAI token and model under AI comment settings";
    private static final String INVALID_ELEMENT_NOTIFICATION_CONTENT = "Could not detect code block";
    private static final String INVALID_BLOCK_NOTIFICATION_CONTENT = "Please select code block or place the caret inside a function";
    private static final String GENERAL_ERROR_NOTIFICATION_TITLE = "AI Comment";
    private static final String COULD_NOT_DETECT_EDITOR_OR_FILE_NOTIFICATION_CONTENT = "Could not detect editor/file";
    static final AnAction OPEN_SETTINGS_ACTION = new OpenSettingsAction();


    private AppSettingsState settings;
    private PromptService promptService;
    public AddAiCommentAction() {
        super();
        reloadSettings();
    }

    public static AddAiCommentAction getInstance() {
        return (AddAiCommentAction) ActionManager.getInstance().getAction(ACTION_ID);
    }

    public void reloadSettings() {
        this.settings = AppSettingsState.getInstance();
        this.promptService = new PromptService(settings.openAiToken, settings.openAiModel);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {

        if (Objects.equals(this.settings.openAiToken, "") || Objects.equals(this.settings.openAiModel, "")) {
            this.showErrorNotification(MISSING_SETTINGS_NOTIFICATION_TITLE, UPDATE_SETTINGS_NOTIFICATION_CONTENT, OPEN_SETTINGS_ACTION);
            return;
        }

        Project project = event.getRequiredData(CommonDataKeys.PROJECT);
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        if (editor == null || psiFile == null) {
            this.showErrorNotification(GENERAL_ERROR_NOTIFICATION_TITLE, COULD_NOT_DETECT_EDITOR_OR_FILE_NOTIFICATION_CONTENT);
            return;
        }
        CaretModel caret = editor.getCaretModel();
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
                this.showErrorNotification(INVALID_BLOCK_NOTIFICATION_TITLE, INVALID_ELEMENT_NOTIFICATION_CONTENT);
                return;
            }

            GoFunctionOrMethodDeclaration pe = PsiTreeUtil.getParentOfType(element1, GoFunctionOrMethodDeclaration.class);
            if (pe == null) {
                this.showErrorNotification(INVALID_BLOCK_NOTIFICATION_TITLE, INVALID_BLOCK_NOTIFICATION_CONTENT);
                return;
            }

            blockName = pe.getName();
            blockCode = pe.getText();
            blockType = "function";
            element = pe;
            // find first comment before whitespace

        }


        if (element == null) {
            this.showErrorNotification(INVALID_BLOCK_NOTIFICATION_TITLE, INVALID_BLOCK_NOTIFICATION_CONTENT);
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



        WriteCommandAction.runWriteCommandAction(project, () -> {
            // delete whitespaces
            for (PsiElement el : dropElements) {
                el.delete();
            }
            String comment = this.getComment(blockName, blockCode, blockType);
            PsiParserFacade factory = PsiParserFacade.SERVICE.getInstance(project);
            PsiComment newPsiComment = factory.createLineCommentFromText(GoLanguage.INSTANCE, comment);
            if (psiComment != null) {
                psiComment.replace(newPsiComment);
            } else {
                element.getParent().addBefore(newPsiComment, element);
            }
        });

    }

    private String getComment(String funcName, String funcBody, String blockType) {

        try {

            String comment = promptService.execute(blockType, funcBody);

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
            this.showErrorNotification(GENERAL_ERROR_NOTIFICATION_TITLE, e.getMessage(), new OpenSettingsAction("Check settings"));
            return "";
        }
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
        e.getPresentation().setEnabled(editor != null && psiFile != null && this.settings.openAiToken != null);
    }

    private void showErrorNotification(String title, String content, @Nullable AnAction... actions) {
        Notification notification = new Notification(PLUGIN_ID, title, content, NotificationType.ERROR);
        if (actions != null) {
            notification.addActions((Collection<? extends AnAction>) Arrays.asList(actions));
        }
        Notifications.Bus.notify(notification);
    }
}
