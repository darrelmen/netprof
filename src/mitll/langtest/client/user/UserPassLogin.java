package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Fieldset;
import com.github.gwtbootstrap.client.ui.Form;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by go22670 on 8/11/14.
 */
public class UserPassLogin extends UserDialog {
  public static final int ILR_CHOICE_WIDTH = 80;
  public static final int MIN_LENGTH_USER_ID = 8;
  private static final String PRACTICE = "Practice";
  private static final String DEMO = "Demo";
  private static final String DATA_COLLECTION = "Data Collection";
  public static final String REVIEW = "Review";
  private static final Map<String, String> displayToRoles = new TreeMap<String, String>();
  private static final String STUDENT = "Student";
  private static final String TEACHER_REVIEWER = "Reviewer";
  private static final String TEACHER = "Teacher";
  private static final String RECORDER = "Recorder";
  private static final List<String> ROLES = Arrays.asList(STUDENT, TEACHER);
  public static final String ARE_YOU_A = "Are you a";
  public static final String USER_ID = "User ID";
  public static final String USER_ID_TOOLTIP = "New users can choose any id and login.";
  public static final String PASSWORD = "Password";
  public static final String DIALECT = "Dialect";
  public static final String CHOOSE_A_GENDER = "Choose a gender.";
  private static final String DO_QUALITY_CONTROL = " Do Quality Control";
  private static final String MARK_AND_FIX_DEFECTS = "Mark and fix defects in text and audio";
  private static final String RECORD_REFERENCE_AUDIO = " Record Reference Audio";
  private static final String RECORD_REFERENCE_AUDIO_TOOLTIP = "Record reference audio for course content";

  public UserPassLogin(LangTestDatabaseAsync service, PropertyHandler props) {
    super(service,props);

    //getContent();

  }

  public Panel getContent() {
    Panel leftAndRight = new HorizontalPanel();
    leftAndRight.getElement().getStyle().setBackgroundColor("lightBlue");
    DivWidget left = new DivWidget();
    leftAndRight.add(left);
   // left.addStyleName("floatLeft");
    left.add(new Heading(3,"Learn how to pronounce words and practice vocabulary."));
    left.add(new Heading(4, "Do flashcards to learn or review vocabulary", "Speak your answers. Compete with your friends"));
    left.add(new Heading(4,"Get feedback on your pronunciation","compare yourself to a native speaker"));
    left.add(new Heading(4,"Make your own lists of words to study later or to share."));


    DivWidget right = new DivWidget();
    leftAndRight.add(right);
    //right.addStyleName("floatRight");
    //FormPanel w = new FormPanel();

    Form form = new Form();
    form.addStyleName("form-horizontal");

    Fieldset fieldset = new Fieldset();
    form.add(fieldset);

    right.add(form);

    final ListBoxFormField purpose = getListBoxFormField(fieldset, ARE_YOU_A, getListBox2(ROLES));
    purpose.box.setWidth("150px");

    final FormField user = addControlFormField(fieldset, USER_ID, MIN_LENGTH_USER_ID,"min 8 characters, 2 numbers like \"jlebron23\"");
    //user.setVisible(isDataCollection(purpose) || isPractice(purpose));
    addTooltip(user.box, USER_ID_TOOLTIP);
    user.box.setFocus(true);
    final FormField password = addControlFormField(fieldset, PASSWORD, true, 30, USER_ID_MAX_LENGTH, "");
    return leftAndRight;
    }
}
