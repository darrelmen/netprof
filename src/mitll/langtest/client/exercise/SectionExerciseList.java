package mitll.langtest.client.exercise;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;

import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 2/25/13
 * Time: 1:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class SectionExerciseList extends PagingExerciseList {
  private Map<String, Collection<String>> typeToSections;
  private Panel sectionPanel;

  public SectionExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                             UserFeedback feedback,
                             boolean arabicDataCollect, boolean showTurkToken, boolean showInOrder) {
    super(currentExerciseVPanel, service, feedback, arabicDataCollect, showTurkToken, showInOrder);
  }

  @Override
  protected void addTableWithPager() {
    add(sectionPanel = new VerticalPanel());
    super.addTableWithPager();    //To change body of overridden methods use File | Settings | File Templates.
  }

  @Override
  public void getExercises(long userID) {
    service.getTypeToSection(new AsyncCallback<Map<String, Collection<String>>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Map<String, Collection<String>> result) {
        typeToSections = result;
        System.out.println("got " +typeToSections);

        final Grid g = new Grid(typeToSections.keySet().size(),3);

        int row = 0;
        for (String type : result.keySet()) {
          ListBox listBox = new ListBox();
          for (String section : result.get(type)) {
            listBox.addItem(section);
          }

          // todo add checkbox
          g.setText(row,0,type);

          g.setWidget(row++,1,listBox);
        }
        sectionPanel.add(g);
      }
    });

    super.getExercises(userID);
  }
}
