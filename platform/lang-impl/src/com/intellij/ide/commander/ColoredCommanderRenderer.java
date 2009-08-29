package com.intellij.ide.commander;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

final class ColoredCommanderRenderer extends ColoredListCellRenderer {
  private final CommanderPanel myCommanderPanel;

  public ColoredCommanderRenderer(@NotNull final CommanderPanel commanderPanel) {
    myCommanderPanel = commanderPanel;
  }

  public Component getListCellRendererComponent(final JList list, final Object value, final int index, boolean selected, boolean hasFocus){
    hasFocus = selected; // border around inactive items 

    if (!myCommanderPanel.isActive()) {
      selected = false;
    }

    return super.getListCellRendererComponent(list, value, index, selected, hasFocus);
  }

  protected void customizeCellRenderer(final JList list, final Object value, final int index, final boolean selected, final boolean hasFocus) {
    Color color = UIUtil.getListForeground();
    SimpleTextAttributes attributes = null;
    String locationString = null;

    if (value instanceof NodeDescriptor) {
      final NodeDescriptor descriptor = (NodeDescriptor)value;
      setIcon(descriptor.getClosedIcon());
      final Color elementColor = descriptor.getColor();
      
      if (elementColor != null) {
        color = elementColor;
      }

      if (descriptor instanceof AbstractTreeNode) {
        final AbstractTreeNode treeNode = (AbstractTreeNode)descriptor;
        final TextAttributesKey attributesKey = treeNode.getAttributesKey();

        if (attributesKey != null) {
          final TextAttributes textAttributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(attributesKey);

          if (textAttributes != null) attributes =  SimpleTextAttributes.fromTextAttributes(textAttributes);
        }
        locationString = treeNode.getLocationString();
      }
    }

    if(attributes == null) attributes = new SimpleTextAttributes(Font.PLAIN, color);
    final String text = value.toString();
    append(text != null? text : "", attributes);

    if (locationString != null && locationString.length() > 0) {
      append(" (" + locationString + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
  }
}