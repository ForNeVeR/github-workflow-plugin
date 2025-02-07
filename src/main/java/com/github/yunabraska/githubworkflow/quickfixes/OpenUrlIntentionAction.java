package com.github.yunabraska.githubworkflow.quickfixes;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class OpenUrlIntentionAction extends QuickFix {
    private final String url;
    private final String text;

    public OpenUrlIntentionAction(final String url, final String text, final Icon icon) {
        super(icon);
        this.url = url;
        this.text = text;
    }

    @NotNull
    @Override
    public String getText() {
        return text;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean isAvailable(@NotNull final Project project, final Editor editor, final PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file) {
        BrowserUtil.browse(url);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
