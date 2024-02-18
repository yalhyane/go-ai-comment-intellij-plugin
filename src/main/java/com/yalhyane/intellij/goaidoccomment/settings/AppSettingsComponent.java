package com.yalhyane.intellij.goaidoccomment.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class AppSettingsComponent {
    public static final String DEFAULT_OPENAI_MODEL = "gpt-3.5-turbo";

    private final JPanel mainPanel;
    private final JBTextField openAiToken = new JBTextField();
    private final ComboBox<String> openAiModel = new ComboBox<String>(new String[]{"gpt-3.5-turbo", "gpt-4"});


    public AppSettingsComponent() {

        mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("OpenAI token: "), openAiToken, 1, false)
                .addLabeledComponent(new JBLabel("OpenAI model: "), openAiModel, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return openAiToken;
    }

    @NotNull
    public String getOpenAiToken() {
        return openAiToken.getText();
    }

    public void setOpenAiToken(@NotNull String token) {
        openAiToken.setText(token);
    }


    public void setOpenaiModel(String model) {
        openAiModel.setSelectedItem(model);
    }

    public String getOpenAiModel() {
        if (openAiModel.getSelectedItem() == null) {
            return DEFAULT_OPENAI_MODEL;
        }
        return openAiModel.getSelectedItem().toString();
    }
}
