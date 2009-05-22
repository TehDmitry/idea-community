package com.intellij.packaging.artifacts;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.ui.ArtifactPropertiesEditor;
import com.intellij.packaging.ui.PackagingEditorContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public abstract class ArtifactProperties<S> implements PersistentStateComponent<S> {

  public void onBuildFinished(@NotNull Project project, @NotNull Artifact artifact) {
  }

  public abstract ArtifactPropertiesEditor createEditor(@NotNull PackagingEditorContext context);
}