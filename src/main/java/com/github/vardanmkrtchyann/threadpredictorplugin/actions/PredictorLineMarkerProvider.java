package com.github.vardanmkrtchyann.threadpredictorplugin.actions;

import com.github.vardanmkrtchyann.threadpredictorplugin.services.GeminiService;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PredictorLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {

        // Target 1: Thread execution methods (.start(), .submit(), .runAsync())
        if (element instanceof PsiIdentifier && element.getParent() instanceof PsiReferenceExpression) {
            PsiReferenceExpression ref = (PsiReferenceExpression) element.getParent();
            if (ref.getParent() instanceof PsiMethodCallExpression) {
                String methodName = element.getText();

                if ("start".equals(methodName) || "submit".equals(methodName) ||
                        "execute".equals(methodName) || "runAsync".equals(methodName) ||
                        "supplyAsync".equals(methodName)) {

                    return createMarker(element);
                }
            }
        }

        // Target 2: synchronized blocks and methods
        if (element instanceof PsiKeyword) {
            if (PsiKeyword.SYNCHRONIZED.equals(element.getText())) {
                return createMarker(element);
            }
        }

        return null;
    }

    private LineMarkerInfo<PsiElement> createMarker(PsiElement element) {
        return new LineMarkerInfo<>(
                element,
                element.getTextRange(),
                AllIcons.Debugger.Threads,
                psiElement -> "Predict Thread Execution (AI)",
                (mouseEvent, psiElement) -> runAiAnalysis(psiElement),
                GutterIconRenderer.Alignment.LEFT,
                () -> "AI Thread Predictor"
        );
    }

    private void runAiAnalysis(PsiElement triggerElement) {
        Project project = triggerElement.getProject();
        PsiClass enclosingClass = PsiTreeUtil.getParentOfType(triggerElement, PsiClass.class);

        if (enclosingClass == null) return;

        final String codeToAnalyze = enclosingClass.getText();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Gemini is analyzing threads...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                String aiResponse = GeminiService.analyzeCode(codeToAnalyze);

                ApplicationManager.getApplication().invokeLater(() -> {
                    NotificationGroupManager.getInstance()
                            .getNotificationGroup("Custom Notification Group")
                            .createNotification("AI Execution Trace", aiResponse, NotificationType.INFORMATION)
                            .notify(project);
                });
            }
        });
    }
}