package com.intellij.xdebugger.impl.ui.tree;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.impl.ui.XDebuggerExpressionComboBox;
import com.intellij.xdebugger.impl.ui.tree.nodes.XDebuggerTreeNode;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.tree.TreePath;

/**
 * @author nik
 */
public abstract class XDebuggerTreeInplaceEditor extends TreeInplaceEditor {
  private final XDebuggerTreeNode myNode;
  protected final XDebuggerExpressionComboBox myExpressionEditor;
  protected XDebuggerTree myTree;

  public XDebuggerTreeInplaceEditor(final XDebuggerTreeNode node, @NonNls final String historyId) {
    myNode = node;
    myTree = myNode.getTree();
    myExpressionEditor = new XDebuggerExpressionComboBox(myTree.getProject(), myTree.getEditorsProvider(), historyId, myTree.getSourcePosition());
  }

  protected JComponent getPreferredFocusedComponent() {
    return myExpressionEditor.getPreferredFocusedComponent();
  }

  public XDebuggerTreeNode getNode() {
    return myNode;
  }

  public Editor getEditor() {
    return myExpressionEditor.getEditor();
  }

  public JComponent getEditorComponent() {
    return myExpressionEditor.getEditorComponent();
  }

  protected TreePath getNodePath() {
    return myNode.getPath();
  }

  protected JTree getTree() {
    return myNode.getTree();
  }

  protected Project getProject() {
    return myNode.getTree().getProject();
  }
}