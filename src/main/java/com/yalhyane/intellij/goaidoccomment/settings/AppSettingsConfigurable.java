package com.yalhyane.intellij.goaidoccomment.settings;
import com.intellij.openapi.options.Configurable;
import com.yalhyane.intellij.goaidoccomment.AddAiCommentAction;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Provides controller functionality for application settings.
 */
public class AppSettingsConfigurable implements Configurable {

    private AppSettingsComponent settingsComponent;

    // A default constructor with no arguments is required because this implementation
    // is registered as an applicationConfigurable EP

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "AI Comment";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return settingsComponent.getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        settingsComponent = new AppSettingsComponent();
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        AppSettingsState settings = AppSettingsState.getInstance();
        return !settingsComponent.getOpenAiToken().equals(settings.openAiToken) ||
                !settingsComponent.getOpenAiModel().equals(settings.openAiModel);
    }

    @Override
    public void apply() {
        AppSettingsState settings = AppSettingsState.getInstance();
        settings.openAiToken = settingsComponent.getOpenAiToken();
        settings.openAiModel = settingsComponent.getOpenAiModel();
        AddAiCommentAction.getInstance().reloadSettings();

    }

    @Override
    public void reset() {
        AppSettingsState settings = AppSettingsState.getInstance();
        settingsComponent.setOpenAiToken(settings.openAiToken);
        settingsComponent.setOpenaiModel(settings.openAiModel);
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }

}
