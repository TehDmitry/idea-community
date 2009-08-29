/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.JDOMExternalizableStringList;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import gnu.trove.THashMap;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * User: anna
 * Date: 24-Feb-2006
 */
public class SeverityRegistrar implements JDOMExternalizable, Comparator<HighlightSeverity> {
  @NonNls private static final String INFO = "info";
  private final Map<String, SeverityBasedTextAttributes> ourMap = new THashMap<String, SeverityBasedTextAttributes>();
  private final Map<String, Color> ourRendererColors = new THashMap<String, Color>();
  @NonNls private static final String COLOR = "color";

  private final JDOMExternalizableStringList myOrder = new JDOMExternalizableStringList();
  private JDOMExternalizableStringList myReadOrder;

  private static final Map<String, HighlightInfoType> STANDART_SEVERITIES = new HashMap<String, HighlightInfoType>();

  static {
    STANDART_SEVERITIES.put(HighlightSeverity.ERROR.toString(), HighlightInfoType.ERROR);
    STANDART_SEVERITIES.put(HighlightSeverity.WARNING.toString(), HighlightInfoType.WARNING);
    STANDART_SEVERITIES.put(HighlightSeverity.INFO.toString(), HighlightInfoType.INFO);
    STANDART_SEVERITIES.put(HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING.toString(), HighlightInfoType.GENERIC_WARNINGS_OR_ERRORS_FROM_SERVER);
    final EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    for (SeveritiesProvider provider : Extensions.getExtensions(SeveritiesProvider.EP_NAME)) {
      for (HighlightInfoType highlightInfoType : provider.getSeveritiesHighlightInfoTypes()) {
        final HighlightSeverity highlightSeverity = highlightInfoType.getSeverity(null);
        STANDART_SEVERITIES.put(highlightSeverity.toString(), highlightInfoType);
        final TextAttributesKey attributesKey = highlightInfoType.getAttributesKey();
        TextAttributes textAttributes = scheme.getAttributes(attributesKey);
        if (textAttributes == null) {
          textAttributes = attributesKey.getDefaultAttributes();
        }
        HighlightDisplayLevel.registerSeverity(highlightSeverity, provider.getTrafficRendererColor(textAttributes));
      }
    }
  }

  public static SeverityRegistrar getInstance() {
    return InspectionProfileManager.getInstance().getSeverityRegistrar();
  }

  public static SeverityRegistrar getInstance(@Nullable Project project) {
    return project != null ? InspectionProjectProfileManager.getInstance(project).getSeverityRegistrar() : getInstance();
  }

  public void registerSeverity(SeverityBasedTextAttributes info, Color renderColor){
    final HighlightSeverity severity = info.getType().getSeverity(null);
    ourMap.put(severity.toString(), info);
    ourRendererColors.put(severity.toString(), renderColor);
    myOrder.clear();
    HighlightDisplayLevel.registerSeverity(severity, renderColor);
  }

  public Collection<SeverityBasedTextAttributes> getRegisteredHighlightingInfoTypes() {
    final Collection<SeverityBasedTextAttributes> collection = new ArrayList<SeverityBasedTextAttributes>(ourMap.values());
    for (HighlightInfoType type : STANDART_SEVERITIES.values()) {
      collection.add(getSeverityBasedTextAttributes(type));
    }
    return collection;
  }

  private SeverityBasedTextAttributes getSeverityBasedTextAttributes(HighlightInfoType type) {
    final EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    final TextAttributes textAttributes = scheme.getAttributes(type.getAttributesKey());
    if (textAttributes != null) {
      return new SeverityBasedTextAttributes(textAttributes, (HighlightInfoType.HighlightInfoTypeImpl)type);
    }
    return new SeverityBasedTextAttributes(getTextAttributesBySeverity(type.getSeverity(null)), (HighlightInfoType.HighlightInfoTypeImpl)type);
  }


  public SeverityBasedTextAttributes unregisterSeverity(HighlightSeverity severity){
    return ourMap.remove(severity.toString());
  }

  public HighlightInfoType.HighlightInfoTypeImpl getHighlightInfoTypeBySeverity(HighlightSeverity severity) {
    HighlightInfoType infoType = STANDART_SEVERITIES.get(severity.toString());
    if (infoType != null) {
      return (HighlightInfoType.HighlightInfoTypeImpl)infoType;
    }

    if (severity == HighlightSeverity.INFORMATION){
      return (HighlightInfoType.HighlightInfoTypeImpl)HighlightInfoType.INFORMATION;
    }

    final SeverityBasedTextAttributes type = ourMap.get(severity.toString());
    return (HighlightInfoType.HighlightInfoTypeImpl)(type != null ? type.getType() : HighlightInfoType.WARNING);
  }

  @Nullable
  public TextAttributes getTextAttributesBySeverity(HighlightSeverity severity) {
    final SeverityBasedTextAttributes infoType = ourMap.get(severity.toString());
    if (infoType != null) {
      return infoType.getAttributes();
    }
    return null;
  }


  public void readExternal(Element element) throws InvalidDataException {
    ourMap.clear();
    ourRendererColors.clear();
    final List children = element.getChildren(INFO);
    if (children != null){
      for (Object child : children) {
        final Element infoElement = (Element)child;

        final SeverityBasedTextAttributes highlightInfo = new SeverityBasedTextAttributes();
        highlightInfo.readExternal(infoElement);

        Color color = null;
        final String colorStr = infoElement.getAttributeValue(COLOR);
        if (colorStr != null){
          color = new Color(Integer.parseInt(colorStr, 16));
        }
        registerSeverity(highlightInfo, color);
      }
    }
    myOrder.clear();
    myOrder.readExternal(element);

    myReadOrder = new JDOMExternalizableStringList();
    myReadOrder.addAll(myOrder);

    final Set<String> knownSeverities = new HashSet<String>(ourMap.keySet()); //remove all irrelevant
    knownSeverities.addAll(STANDART_SEVERITIES.keySet());
    myOrder.retainAll(knownSeverities);

    if (myOrder.isEmpty()) {
      initOrder();
    } else {//enforce include all known
      List<HighlightSeverity> order = new ArrayList<HighlightSeverity>();
      for (HighlightInfoType type : STANDART_SEVERITIES.values()) {
        order.add(type.getSeverity(null));
      }
      Collections.sort(order);
      for (int i = 0; i < order.size(); i++) {
        HighlightSeverity stdSeverity = order.get(i);
        if (!myOrder.contains(stdSeverity.toString())) {
          for (int oIdx = 0; oIdx < myOrder.size(); oIdx++) {
            final HighlightInfoType type = STANDART_SEVERITIES.get(myOrder.get(oIdx));
            if (type != null && order.indexOf(type.getSeverity(null)) > i) {
              myOrder.add(oIdx, stdSeverity.toString());
              myReadOrder = null;
              break;
            }
          }
        }
      }
    }
  }

  public void writeExternal(Element element) throws WriteExternalException {
    for (String severity : myOrder) {
      Element info = new Element(INFO);
      final SeverityBasedTextAttributes infoType = ourMap.get(severity);
      if (infoType != null) {
        infoType.writeExternal(info);
        final Color color = ourRendererColors.get(severity);
        if (color != null) {
          info.setAttribute(COLOR, Integer.toString(color.getRGB() & 0xFFFFFF, 16));
        }
        element.addContent(info);
      }
    }
    if (myReadOrder != null) {
      myReadOrder.writeExternal(element);
    } else {
      myOrder.writeExternal(element);
    }
  }

  public int getSeveritiesCount() {
    return createCurrentSeverities().size();
  }

  public HighlightSeverity getSeverityByIndex(final int i) {
    return getSeverity(getOrder().get(i));
  }

  public HighlightSeverity getSeverity(final String name) {
    final HighlightInfoType type = STANDART_SEVERITIES.get(name);
    if (type != null) return type.getSeverity(null);
    final SeverityBasedTextAttributes attributes = ourMap.get(name);
    if (attributes != null) return attributes.getSeverity();
    return null;
  }

  private List<String> createCurrentSeverities() {
    List<String> list = new ArrayList<String>();
    list.addAll(STANDART_SEVERITIES.keySet());
    list.addAll(ourMap.keySet());
    Collections.sort(list);
    return list;
  }

  public Icon getRendererIconByIndex(final int i) {
    final HighlightSeverity severity = getSeverityByIndex(i);
    HighlightDisplayLevel level = HighlightDisplayLevel.find(severity);
    if (level != null) {
      return level.getIcon();
    }

    return HighlightDisplayLevel.createIconByMask(ourRendererColors.get(severity.toString()));
  }

  public boolean isSeverityValid(final String severity) {
    return createCurrentSeverities().contains(severity);
  }

  public int compare(final HighlightSeverity s1, final HighlightSeverity s2) {
    return getOrder().indexOf(s1.myName) - getOrder().indexOf(s2.myName);
  }

  private JDOMExternalizableStringList getOrder() {
    if (myOrder.isEmpty()) {
      initOrder();
    }
    return myOrder;
  }

  private void initOrder() {
    final List<HighlightSeverity> order = new ArrayList<HighlightSeverity>();
    for (HighlightInfoType type : STANDART_SEVERITIES.values()) {
      order.add(type.getSeverity(null));
    }
    for (SeverityBasedTextAttributes attributes : ourMap.values()) {
      order.add(attributes.getSeverity());
    }
    Collections.sort(order);
    for (HighlightSeverity severity : order) {
      myOrder.add(severity.toString());
    }
  }

  public void setOrder(List<String> order) {
    myOrder.clear();
    myOrder.addAll(order);

    myReadOrder = null;
  }

  public int getSeverityIdx(@NotNull HighlightSeverity severity) {
    return getOrder().indexOf(severity.toString());
  }

  public boolean isDefaultSeverity(HighlightSeverity severity) {
    return STANDART_SEVERITIES.containsKey(severity.myName);
  }

  public static boolean skipSeverity(HighlightSeverity minSeverity) {
    for (SeveritiesProvider provider : Extensions.getExtensions(SeveritiesProvider.EP_NAME)) {
      if (!provider.isGotoBySeverityEnabled(minSeverity)) return true;
    }
    return minSeverity == HighlightSeverity.INFORMATION;
  }

  public static class SeverityBasedTextAttributes implements JDOMExternalizable {
    private final TextAttributes myAttributes;
    private final HighlightInfoType.HighlightInfoTypeImpl myType;

    //readexternal
    public SeverityBasedTextAttributes() {
      myAttributes = new TextAttributes();
      myType = new HighlightInfoType.HighlightInfoTypeImpl();
    }

    public SeverityBasedTextAttributes(final TextAttributes attributes, final HighlightInfoType.HighlightInfoTypeImpl type) {
      myAttributes = attributes;
      myType = type;
    }

    public TextAttributes getAttributes() {
      return myAttributes;
    }

    public HighlightInfoType.HighlightInfoTypeImpl getType() {
      return myType;
    }

    public void readExternal(Element element) throws InvalidDataException {
      myAttributes.readExternal(element);
      myType.readExternal(element);
    }

    public void writeExternal(Element element) throws WriteExternalException {
      myAttributes.writeExternal(element);
      myType.writeExternal(element);
    }

    public HighlightSeverity getSeverity() {
      return myType.getSeverity(null);
    }

    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final SeverityBasedTextAttributes that = (SeverityBasedTextAttributes)o;

      if (myAttributes != null ? !myAttributes.equals(that.myAttributes) : that.myAttributes != null) return false;
      if (myType != null ? !myType.equals(that.myType) : that.myType != null) return false;

      return true;
    }

    public int hashCode() {
      int result;
      result = (myAttributes != null ? myAttributes.hashCode() : 0);
      result = 31 * result + (myType != null ? myType.hashCode() : 0);
      return result;
    }
  }
}