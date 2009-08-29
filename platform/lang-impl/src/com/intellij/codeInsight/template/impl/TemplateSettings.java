package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.template.Template;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.DecodeDefaultsUtil;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.options.Scheme;
import com.intellij.openapi.options.SchemeProcessor;
import com.intellij.openapi.options.SchemesManager;
import com.intellij.openapi.options.SchemesManagerFactory;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


@State(
  name="TemplateSettings",
  storages= {
    @Storage(
      id="other",
      file = "$APP_CONFIG$/other.xml"
    )}
)
public class TemplateSettings implements PersistentStateComponent<Element>, ExportableComponent {

  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.template.impl.TemplateSettings");

  public  @NonNls static final String USER_GROUP_NAME = "user";
  private @NonNls static final String TEMPLATE_SET = "templateSet";
  private @NonNls static final String GROUP = "group";
  private @NonNls static final String TEMPLATE = "template";

  private @NonNls static final String DELETED_TEMPLATES = "deleted_templates";
  private final List<String> myDeletedTemplates = new ArrayList<String>();

  public static final char SPACE_CHAR = ' ';
  public static final char TAB_CHAR = '\t';
  public static final char ENTER_CHAR = '\n';
  public static final char DEFAULT_CHAR = 'D';

  private static final @NonNls String SPACE = "SPACE";
  private static final @NonNls String TAB = "TAB";
  private static final @NonNls String ENTER = "ENTER";

  private static final @NonNls String NAME = "name";
  private static final @NonNls String VALUE = "value";
  private static final @NonNls String DESCRIPTION = "description";
  private static final @NonNls String SHORTCUT = "shortcut";

  private static final @NonNls String VARIABLE = "variable";
  private static final @NonNls String EXPRESSION = "expression";
  private static final @NonNls String DEFAULT_VALUE = "defaultValue";
  private static final @NonNls String ALWAYS_STOP_AT = "alwaysStopAt";

  private static final @NonNls String CONTEXT = "context";
  private static final @NonNls String TO_REFORMAT = "toReformat";
  private static final @NonNls String TO_SHORTEN_FQ_NAMES = "toShortenFQNames";

  private static final @NonNls String DEFAULT_SHORTCUT = "defaultShortcut";
  private static final @NonNls String DEACTIVATED = "deactivated";

  @NonNls private static final String RESOURCE_BUNDLE = "resource-bundle";
  @NonNls private static final String KEY = "key";
  @NonNls private static final String ID = "id";

  private static final @NonNls String TEMPLATES_CONFIG_FOLDER = "templates";

  private final Map<String,Template> myTemplates = new LinkedHashMap<String,Template>();
  private final Map<String,Template> myTemplatesById = new LinkedHashMap<String,Template>();
  private final Map<String,TemplateImpl> myDefaultTemplates = new LinkedHashMap<String, TemplateImpl>();

  private int myMaxKeyLength = 0;
  private char myDefaultShortcutChar = TAB_CHAR;
  private String myLastSelectedTemplateKey;
  @NonNls
  public static final String XML_EXTENSION = ".xml";
  private final SchemesManager<TemplateGroup, TemplateGroup> mySchemesManager;
  private final SchemeProcessor<TemplateGroup> myProcessor;
  private static final String FILE_SPEC = "$ROOT_CONFIG$/templates";

  public TemplateSettings(SchemesManagerFactory schemesManagerFactory) {


    myProcessor = new SchemeProcessor<TemplateGroup>() {
      public TemplateGroup readScheme(final Document schemeContent)
          throws InvalidDataException, IOException, JDOMException {
        return readTemplateFile(schemeContent, schemeContent.getRootElement().getAttributeValue("group"), false, false);
      }


      public boolean shouldBeSaved(final TemplateGroup template) {
        for (TemplateImpl t : template.getElements()) {
          if (differsFromDefault(t)) {
            return true;
          }
        }
        return false;
      }

      public Document writeScheme(final TemplateGroup template) throws WriteExternalException {
        Element templateSetElement = new Element(TEMPLATE_SET);
        templateSetElement.setAttribute(GROUP, template.getName());

        for (TemplateImpl t : template.getElements()) {
          if (differsFromDefault(t)) {
            saveTemplate(t, templateSetElement);
          }
        }

        return new Document(templateSetElement);
      }

      public void initScheme(final TemplateGroup scheme) {
        Collection<TemplateImpl> templates = scheme.getElements();

        for (TemplateImpl template : templates) {
          addTemplateImpl(template);
        }
      }

      public void onSchemeAdded(final TemplateGroup scheme) {
        for (TemplateImpl template : scheme.getElements()) {
          addTemplateImpl(template);
        }
      }

      public void onSchemeDeleted(final TemplateGroup scheme) {
        for (TemplateImpl template : scheme.getElements()) {
          removeTemplate(template);
        }
      }

      public void onCurrentSchemeChanged(final Scheme newCurrentScheme) {

      }
    };

    mySchemesManager = schemesManagerFactory.createSchemesManager(FILE_SPEC, myProcessor, RoamingType.PER_USER);

    loadTemplates();
  }

  private boolean differsFromDefault(TemplateImpl t) {
    TemplateImpl def = myDefaultTemplates.get(t.getKey());
    if (def == null) return true;
    return !t.equals(def) || !t.contextsEqual(def);
  }

  @NotNull
  public File[] getExportFiles() {
    return new File[]{getTemplateDirectory(true),PathManager.getDefaultOptionsFile()};
  }

  @NotNull
  public String getPresentableName() {
    return CodeInsightBundle.message("templates.export.display.name");
  }

  public static TemplateSettings getInstance() {
    return ServiceManager.getService(TemplateSettings.class);
  }

  public void loadState(Element parentNode) {
    Element element = parentNode.getChild(DEFAULT_SHORTCUT);
    if (element != null) {
      String shortcut = element.getAttributeValue(SHORTCUT);
      if (TAB.equals(shortcut)) {
        myDefaultShortcutChar = TAB_CHAR;
      } else if (ENTER.equals(shortcut)) {
        myDefaultShortcutChar = ENTER_CHAR;
      } else {
        myDefaultShortcutChar = SPACE_CHAR;
      }
    }

    Element deleted = parentNode.getChild(DELETED_TEMPLATES);
    if (deleted != null) {
      List children = deleted.getChildren();
      for (final Object aChildren : children) {
        Element child = (Element)aChildren;
        myDeletedTemplates.add(child.getAttributeValue(NAME));
      }
    }

    for (String name : myDeletedTemplates) {
      Template toDelete = myTemplates.get(name);
      if (toDelete != null) {
        removeTemplate(toDelete);
      }
    }

    //TODO lesya reload schemes
  }

  public Element getState()  {
    Element parentNode = new Element("TemplateSettings");
    Element element = new Element(DEFAULT_SHORTCUT);
    if (myDefaultShortcutChar == TAB_CHAR) {
      element.setAttribute(SHORTCUT, TAB);
    } else if (myDefaultShortcutChar == ENTER_CHAR) {
      element.setAttribute(SHORTCUT, ENTER);
    } else {
      element.setAttribute(SHORTCUT, SPACE);
    }
    parentNode.addContent(element);

    if (myDeletedTemplates.size() > 0) {
      Element deleted = new Element(DELETED_TEMPLATES);
      for (final String myDeletedTemplate : myDeletedTemplates) {
        Element template = new Element(TEMPLATE);
        template.setAttribute(NAME, myDeletedTemplate);
        deleted.addContent(template);

      }
      parentNode.addContent(deleted);
    }
    return parentNode;
  }

  public String getLastSelectedTemplateKey() {
    return myLastSelectedTemplateKey;
  }

  public void setLastSelectedTemplateKey(String key) {
    myLastSelectedTemplateKey = key;
  }

  public TemplateImpl[] getTemplates() {
    return myTemplates.values().toArray(new TemplateImpl[myTemplates.size()]);
  }

  public char getDefaultShortcutChar() {
    return myDefaultShortcutChar;
  }

  public void setDefaultShortcutChar(char defaultShortcutChar) {
    myDefaultShortcutChar = defaultShortcutChar;
  }

  public TemplateImpl getTemplate(@NonNls String key) {
    return (TemplateImpl) myTemplates.get(key);
  }

  public Template getTemplateById(@NonNls String id) {
    return myTemplatesById.get(id);
  }

  public int getMaxKeyLength() {
    return myMaxKeyLength;
  }

  public void addTemplate(Template template) {
    clearPreviouslyRegistered(template);
    addTemplateImpl(template);

    TemplateImpl templateImpl = (TemplateImpl)template;
    String groupName = templateImpl.getGroupName();
    TemplateGroup group = mySchemesManager.findSchemeByName(groupName);
    if (group == null) {
      group = new TemplateGroup(groupName);
      mySchemesManager.addNewScheme(group, true);
    }
    group.addElement(templateImpl);
  }

  private void clearPreviouslyRegistered(final Template template) {
    TemplateImpl existing = getTemplate(template.getKey());
    if (existing != null) {
      LOG.info("Template with key " + template.getKey() + " and id " + template.getId() + " already registered");
      TemplateGroup group = mySchemesManager.findSchemeByName(existing.getGroupName());
      if (group != null) {
        group.removeElement(existing);
        if (group.isEmpty()) {
          mySchemesManager.removeScheme(group);
        }
      }
      myTemplates.remove(template.getKey());
    }
  }

  private void addTemplateImpl(Template template) {
    if (!myTemplates.containsKey(template.getKey())) {
      myTemplates.put(template.getKey(), template);

      myMaxKeyLength = Math.max(myMaxKeyLength, template.getKey().length());
    }
    myDeletedTemplates.remove(template.getKey());

  }

  private void addTemplateById(Template template) {
    if (!myTemplatesById.containsKey(template.getId())) {
      final String id = template.getId();
      if (id != null) {
        myTemplatesById.put(id, template);
      }
    }
  }

  public void removeTemplate(Template template) {
    myTemplates.remove(template.getKey());

    TemplateImpl templImpl = (TemplateImpl)template;
    String groupName = templImpl.getGroupName();
    TemplateGroup group = mySchemesManager.findSchemeByName(groupName);

    if (group != null) {
      group.removeElement((TemplateImpl)template);
      if (group.isEmpty()) {
        mySchemesManager.removeScheme(group);
      }

    }
  }

  private TemplateImpl addTemplate(String key, String string, String group, String description, String shortcut, boolean isDefault,
                                   final String id) {
    TemplateImpl template = new TemplateImpl(key, string, group);
    template.setId(id);
    template.setDescription(description);
    if (TAB.equals(shortcut)) {
      template.setShortcutChar(TAB_CHAR);
    } else if (ENTER.equals(shortcut)) {
      template.setShortcutChar(ENTER_CHAR);
    } else if (SPACE.equals(shortcut)) {
      template.setShortcutChar(SPACE_CHAR);
    } else {
      template.setShortcutChar(DEFAULT_CHAR);
    }
    if (isDefault) {
      myDefaultTemplates.put(key, template);
    }
    return template;
  }

  @Nullable
  private static File getTemplateDirectory(boolean toCreate) {
    String directoryPath = PathManager.getConfigPath() + File.separator + TEMPLATES_CONFIG_FOLDER;
    File directory = new File(directoryPath);
    if (!directory.exists()) {
      if (!toCreate) {
        return null;
      }
      if (!directory.mkdir()) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("cannot create directory: " + directory.getAbsolutePath());
        }
        return null;
      }
    }
    return directory;
  }

  private void loadTemplates() {

    Collection<TemplateGroup> loaded = mySchemesManager.loadSchemes();
    for (TemplateGroup group : loaded) {
      Collection<TemplateImpl> templates = group.getElements();

      for (TemplateImpl template : templates) {
        addTemplateImpl(template);
      }

    }


    try {
      for(DefaultLiveTemplatesProvider provider: Extensions.getExtensions(DefaultLiveTemplatesProvider.EP_NAME)) {
        for (String defTemplate : provider.getDefaultLiveTemplateFiles()) {
          String templateName = getDefaultTemplateName(defTemplate);
          InputStream inputStream = DecodeDefaultsUtil.getDefaultsInputStream(provider, defTemplate);
          if (inputStream != null) {
            readDefTemplateFile(inputStream, templateName);
          }
        }
      }
    } catch (Exception e) {
      LOG.error(e);
    }
  }

  public static String getDefaultTemplateName(String defTemplate) {
    return defTemplate.substring(defTemplate.lastIndexOf("/") + 1);
  }

  public void readDefTemplateFile(InputStream inputStream, String defGroupName) throws JDOMException, InvalidDataException, IOException {
    readTemplateFile(JDOMUtil.loadDocument(inputStream), defGroupName, true, true);
  }

  @Nullable
  public TemplateGroup readTemplateFile(Document document, @NonNls String defGroupName, boolean isDefault, boolean registerTemplate) throws InvalidDataException {
    if (document == null) {
      throw new InvalidDataException();
    }
    Element root = document.getRootElement();
    if (root == null || !TEMPLATE_SET.equals(root.getName())) {
      throw new InvalidDataException();
    }

    String groupName = root.getAttributeValue(GROUP);
    if (groupName == null || groupName.length() == 0) groupName = defGroupName;

    TemplateGroup result = new TemplateGroup(groupName);

    Map<String, TemplateImpl> created = new LinkedHashMap<String,  TemplateImpl>();

    for (final Object o1 : root.getChildren(TEMPLATE)) {
      Element element = (Element)o1;

      TemplateImpl template = readTemplateFromElement(isDefault, groupName, element);
      boolean doNotRegister = isDefault && (myDeletedTemplates.contains(template.getKey()) || myTemplates.containsKey(template.getKey()));

      if(!doNotRegister) {
        created.put(template.getKey(), template);
      }



    }

    if (registerTemplate) {
      TemplateGroup existingScheme = mySchemesManager.findSchemeByName(result.getName());
      if (existingScheme != null) {
        result = existingScheme;
      }
    }

    for (TemplateImpl template : created.values()) {
      if (registerTemplate) {
        clearPreviouslyRegistered(template);
        addTemplateImpl(template);
      }

      result.addElement(template);
    }

    if (registerTemplate) {
      TemplateGroup existingScheme = mySchemesManager.findSchemeByName(result.getName());
      if (existingScheme == null && !result.isEmpty()) {
        mySchemesManager.addNewScheme(result, false);
      }
    }

    return result.isEmpty() ? null : result;

  }

  private TemplateImpl readTemplateFromElement(final boolean isDefault, final String groupName, final Element element) throws
                                                                                                                       InvalidDataException {
    String name = element.getAttributeValue(NAME);
    String value = element.getAttributeValue(VALUE);
    String description;
    String resourceBundle = element.getAttributeValue(RESOURCE_BUNDLE);
    String key = element.getAttributeValue(KEY);
    String id = element.getAttributeValue(ID);
    if (resourceBundle != null && key != null) {
      ResourceBundle bundle = ResourceBundle.getBundle(resourceBundle);
      description = bundle.getString(key);
    }
    else {
      description = element.getAttributeValue(DESCRIPTION);
    }
    String shortcut = element.getAttributeValue(SHORTCUT);
    TemplateImpl template = addTemplate(name, value, groupName, description, shortcut, isDefault, id);

    template.setToReformat(Boolean.parseBoolean(element.getAttributeValue(TO_REFORMAT)));
    template.setToShortenLongNames(Boolean.parseBoolean(element.getAttributeValue(TO_SHORTEN_FQ_NAMES)));
    template.setDeactivated(Boolean.parseBoolean(element.getAttributeValue(DEACTIVATED)));


    for (final Object o : element.getChildren(VARIABLE)) {
      Element e = (Element)o;
      String variableName = e.getAttributeValue(NAME);
      String expression = e.getAttributeValue(EXPRESSION);
      String defaultValue = e.getAttributeValue(DEFAULT_VALUE);
      boolean isAlwaysStopAt = Boolean.parseBoolean(e.getAttributeValue(ALWAYS_STOP_AT));
      template.addVariable(variableName, expression, defaultValue, isAlwaysStopAt);
    }

    Element context = element.getChild(CONTEXT);
    if (context != null) {
      template.getTemplateContext().readExternal(context);
    }

    return template;
  }

  public void readHiddenTemplateFile(Document document) throws InvalidDataException {
    if (document == null) {
      throw new InvalidDataException();
    }
    Element root = document.getRootElement();
    if (root == null || !TEMPLATE_SET.equals(root.getName())) {
      throw new InvalidDataException();
    }

    for (final Object o1 : root.getChildren(TEMPLATE)) {

      addTemplateById(readTemplateFromElement(false, null, (Element)o1));
    }


  }

  private static void saveTemplate(TemplateImpl template, Element templateSetElement) {
    Element element = new Element(TEMPLATE);
    final String id = template.getId();
    if (id != null) {
      element.setAttribute(ID, id);
    }
    element.setAttribute(NAME, template.getKey());
    element.setAttribute(VALUE, template.getString());
    if (template.getShortcutChar() == TAB_CHAR) {
      element.setAttribute(SHORTCUT, TAB);
    } else if (template.getShortcutChar() == ENTER_CHAR) {
      element.setAttribute(SHORTCUT, ENTER);
    } else if (template.getShortcutChar() == SPACE_CHAR) {
      element.setAttribute(SHORTCUT, SPACE);
    }
    if (template.getDescription() != null) {
      element.setAttribute(DESCRIPTION, template.getDescription());
    }
    element.setAttribute(TO_REFORMAT, Boolean.toString(template.isToReformat()));
    element.setAttribute(TO_SHORTEN_FQ_NAMES, Boolean.toString(template.isToShortenLongNames()));
    if (template.isDeactivated()) {
      element.setAttribute(DEACTIVATED, Boolean.toString(true));
    }

    for (int i = 0; i < template.getVariableCount(); i++) {
      Element variableElement = new Element(VARIABLE);
      variableElement.setAttribute(NAME, template.getVariableNameAt(i));
      variableElement.setAttribute(EXPRESSION, template.getExpressionStringAt(i));
      variableElement.setAttribute(DEFAULT_VALUE, template.getDefaultValueStringAt(i));
      variableElement.setAttribute(ALWAYS_STOP_AT, Boolean.toString(template.isAlwaysStopAt(i)));
      element.addContent(variableElement);
    }

    try {
      Element contextElement = new Element(CONTEXT);
      template.getTemplateContext().writeExternal(contextElement);
      element.addContent(contextElement);
    } catch (WriteExternalException e) {
    }
    templateSetElement.addContent(element);
  }

  public void setTemplates(List<TemplateGroup> newGroups) {
    myTemplates.clear();
    myDeletedTemplates.clear();
    for (TemplateImpl template : myDefaultTemplates.values()) {
      myDeletedTemplates.add(template.getKey());
    }
    mySchemesManager.clearAllSchemes();
    myMaxKeyLength = 0;
    for (TemplateGroup group : newGroups) {
      if (!group.isEmpty()) {
        mySchemesManager.addNewScheme(group, true);
        for (TemplateImpl template : group.getElements()) {
          clearPreviouslyRegistered(template);
          addTemplateImpl(template);
        }
      }
    }
  }

  public SchemesManager<TemplateGroup,TemplateGroup> getSchemesManager() {
    return mySchemesManager;
  }

  public List<TemplateGroup> getTemplateGroups() {
    return mySchemesManager.getAllSchemes();
  }
}