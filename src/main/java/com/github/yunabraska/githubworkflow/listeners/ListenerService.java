package com.github.yunabraska.githubworkflow.listeners;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

@Service
public final class ListenerService implements Disposable {
    @Override
    public void dispose() {
    }

    public static ListenerService getInstance() {
        return ApplicationManager.getApplication().getService(ListenerService.class);
    }

    public static ListenerService getInstance(final Project project) {
        return project.getService(ListenerService.class);
    }
}
