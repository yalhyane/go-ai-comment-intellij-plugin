package com.yalhyane.intellij.goaidoccomment.settings;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class OpenSettingsAction extends NotificationAction {

    public OpenSettingsAction() {
        super("Open settings");
    }

    public OpenSettingsAction(String text) {
        super(text);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
        Project project = e.getProject();
        if (project != null) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, AppSettingsConfigurable.class);
        }
    }
}
