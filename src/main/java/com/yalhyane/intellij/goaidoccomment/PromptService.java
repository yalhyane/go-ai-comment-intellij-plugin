package com.yalhyane.intellij.goaidoccomment;

import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import java.util.List;

public class PromptService {
    private static final String DEFAULT_OPENAI_MODEL = "gpt-3.5-turbo";
    private OpenAiService openAiService = null;
    private String openAiModel = DEFAULT_OPENAI_MODEL;

    public PromptService(String token) {
        this.openAiService = new OpenAiService(token);
    }


    public PromptService(String token, String model) {
        this.openAiService = new OpenAiService(token);
        this.openAiModel = model;
    }

    public String executeWithContext(String blockType, String code) throws Exception {

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                .builder()
                .model(openAiModel)
                .temperature(1.0)
                .maxTokens(200)
                .messages(
                        List.of(
                                new ChatMessage("system", this.getSystemMessage(blockType)),
                                new ChatMessage("user", code)
                        )).build();

        List<ChatCompletionChoice> choices = openAiService.createChatCompletion(chatCompletionRequest)
                .getChoices();


        if (choices.isEmpty()) {
            throw new Exception("Empty response");
        }

        return choices.get(0).getMessage().getContent();

    }

    public String execute(String blockType, String code) throws Exception {

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                .builder()
                .model(openAiModel)
                .temperature(1.0)
                .maxTokens(200)
                .messages(
                        List.of(
                                new ChatMessage("user", getPrompt(code, blockType))
                        )).build();

        List<ChatCompletionChoice> choices = openAiService.createChatCompletion(chatCompletionRequest)
                .getChoices();


        if (choices.isEmpty()) {
            throw new Exception("Empty response");
        }

        return choices.get(0).getMessage().getContent();

    }


    private String getPrompt(String blockCode, String blockType) {
        return "Write an insightful but concise comment in a complete sentence "
                .concat("in present tense for the following ")
                .concat("Golang " + blockType + " without prefacing it with anything, ")
                .concat("the response must be in the language english")
                .concat(":\n")
                .concat(blockCode);
    }

    private String getSystemMessage(String blockType) {
        return "I want you to act as a senior Golang developer. "
                .concat("The user will provide you with " + blockType + " Golang code, "
                        .concat("I want you to respond with an insightful but concise comment in a complete sentence ")
                        .concat("in present tense for the following the code ")
                        .concat("without prefacing it with anything.")
                        .concat("The response must be in English language"));
    }
}
