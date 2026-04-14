package com.github.vardanmkrtchyann.threadpredictorplugin.actions;

import com.github.vardanmkrtchyann.threadpredictorplugin.services.GeminiService;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class PredictorIntention implements IntentionAction {

    @NotNull
    @Override
    public String getText() {
        return "Predict thread output (AI)"; // The text that appears in the Alt+Enter menu
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "AI Analysis";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        // Only show the lightbulb if the cursor is inside a Java Method
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        return PsiTreeUtil.getParentOfType(element, PsiMethod.class) != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        PsiMethod methodAtCaret = PsiTreeUtil.getParentOfType(element, PsiMethod.class);

        if (methodAtCaret == null) return;

        final String codeToAnalyze = methodAtCaret.getText();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Gemini is analyzing threads...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                String aiResponse = GeminiService.analyzeCode(codeToAnalyze);

                ApplicationManager.getApplication().invokeLater(() -> {
                    NotificationGroupManager.getInstance()
                            .getNotificationGroup("Custom Notification Group")
                            .createNotification("AI Thread Prediction", aiResponse, NotificationType.INFORMATION)
                            .notify(project);
                });
            }
        });
    }

    @Override
    public boolean startInWriteAction() {
        // We return false because we are only reading code, not modifying it.
        return false;
    }
}