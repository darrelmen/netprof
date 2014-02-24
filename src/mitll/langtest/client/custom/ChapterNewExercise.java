package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.ItemSorter;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.StartupInfo;
import mitll.langtest.shared.custom.UserExercise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by GO22670 on 2/20/14.
 */
public class ChapterNewExercise<T extends ExerciseShell> extends NewUserExercise<T> {
  public ChapterNewExercise(final LangTestDatabaseAsync service,
                            ExerciseController controller, HasText itemMarker, EditItem editItem, UserExercise newExercise) {
    super(service, controller, itemMarker, editItem, newExercise);
  }

  Map<String,ListBoxFormField> nameToWidget = new HashMap<String, ListBoxFormField>();

  /**
   * TODO add drop down widget to allow selection of unit/lesson
   * TODO add fields in user exercise to record info
   * TODO add SQL columns to read/write info
   * TODO add server side addition to hierarchy for search by unit/lesson
   * TODO add to global list of items?
   *
   * TODO : add delete capability?  = another table???
   *
   *
   * @param container
   */
  @Override
  protected void addItemsAtTop(Panel container) {
    //super.addItemsAtTop(container);
    StartupInfo startupInfo = controller.getStartupInfo();
    Collection<String> typeOrder = controller.getStartupInfo().getTypeOrder();
//  System.out.println("startup info " + startupInfo);
    if (startupInfo.getTypeOrder().isEmpty()) return;

    BasicDialog basicDialog = new BasicDialog();

    List<SectionNode> sectionNodes = controller.getStartupInfo().getSectionNodes();
   // List<String> values = new ArrayList<String>();
    Map<String,List<String>> typeToValues = new HashMap<String, List<String>>();
    String first = typeOrder.iterator().next();

    for (SectionNode sectionNode : sectionNodes) {
      if (sectionNode.getType().equals(first)) {
        populate(typeToValues, first, sectionNode);
      }
    }

    ItemSorter sorter = new ItemSorter();
  //  Collections.sort(values);

    for (String type : typeOrder) {
//        flow.getElement().setId("unitLesson");
      //      flow.addStyleName("leftFiveMargin");

      //String value = newUserExercise.getUnitToValue().get(type);
      List<String> sortedItems = sorter.getSortedItems(typeToValues.get(type));
      ListBoxFormField listBoxFormField = basicDialog.getListBoxFormField(container, type, sortedItems);
      nameToWidget.put(type, listBoxFormField);
      //Heading child = new Heading(4, type, value);
      // child.addStyleName("rightFiveMargin");
      // flow.add(child);
    }
  }

  private void populate(Map<String, List<String>> typeToValues, String first, SectionNode sectionNode) {
    List<String> valuesForType = typeToValues.get(first);
    if (valuesForType == null) typeToValues.put(first, valuesForType = new ArrayList<String>());

    valuesForType.add(sectionNode.getName());

    for (SectionNode child : sectionNode.getChildren()) {
      populate(typeToValues, child.getType(), child);
    }
  }

  @Override
  protected void setFields() {
    super.setFields();
    for (Map.Entry<String, ListBoxFormField> pair : nameToWidget.entrySet()) {
      newUserExercise.addUnitToValue(pair.getKey(), pair.getValue().getValue());

    }
  }
}
