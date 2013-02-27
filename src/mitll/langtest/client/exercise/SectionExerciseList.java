package mitll.langtest.client.exercise;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.ExerciseShell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
  private Map<String,ListBox> typeToBox = new HashMap<String, ListBox>();
  private List<RadioButton> radios = new ArrayList<RadioButton>();

  public SectionExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                             UserFeedback feedback,
                             boolean arabicDataCollect, boolean showTurkToken, boolean showInOrder) {
    super(currentExerciseVPanel, service, feedback, arabicDataCollect, showTurkToken, showInOrder);
  }

  @Override
  protected void checkBeforeLoad(ExerciseShell e) {}

  @Override
  protected void addTableWithPager() {
    add(sectionPanel = new VerticalPanel());
    super.addTableWithPager();
  }

  @Override
  public void getExercises(final long userID) {
    service.getTypeToSection(new AsyncCallback<Map<String, Collection<String>>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Map<String, Collection<String>> result) {
        typeToSections = result;
        sectionPanel.clear();

        final Grid g = new Grid(typeToSections.keySet().size()+1,2);
        String first = null;
        int row = 0;
        for (final String type : result.keySet()) {
          final ListBox listBox = new ListBox();
          typeToBox.put(type,listBox);
          if (first == null) first = type;
          List<String> sections = getSections(result, type);
          for (String section : sections) {
            listBox.addItem(section);
          }
          listBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
              String itemText = listBox.getItemText(listBox.getSelectedIndex());
              //System.out.println("box " + type + " select " +itemText);
              pushNewSectionHistoryToken(type, itemText);
            }
          });

          int col = 0;
          final RadioButton radio = new RadioButton("SectionType", type);
          radios.add(radio);
          radio.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              for (RadioButton rb : radios) {
                typeToBox.get(rb.getText()).setEnabled(rb == radio);
              }
            }
          });
          g.setWidget(row, col++, radio);
          radio.setValue(row == 0);
          listBox.setEnabled(row == 0);

          g.setWidget(row++,col++,listBox);
        }
        Anchor widget = new Anchor("E-MAIL");
        g.setText(row,0, "");
        g.setWidget(row,1, widget);
        widget.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            showEmail();
          }
        });
        sectionPanel.add(g);

        if (first != null) {
          pushFirstSelection(first);
        }
        else {
          noSectionsGetExercises(userID);
        }
      }
    });
  }

  public void showEmail() {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    final Triple triple = getTriple(History.getToken());

    dialogBox.setText("E-MAIL " + triple.type + " " + triple.section);

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    Button closeButton = new Button("Cancel");
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButton");
    FlowPanel hp = new FlowPanel();
    hp.getElement().getStyle().setFloat(Style.Float.LEFT);
    Button sendButton;
    hp.add(sendButton = new Button("Send"));
   /* sendButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
       service.sendEmail(user.getUser(),fromEmail.getText());
      }
    });*/
    hp.add(closeButton);
    final VerticalPanel dialogVPanel = new VerticalPanel();


   // Grid grid = new Grid(4,2);
    FlexTable grid = new FlexTable();
    grid.setStyleName("body");
    int row = 0;
    grid.setText(row, 0, "From");
    final TextBox fromEmail = new TextBox();
    final TextBox toEmail = new TextBox();
    grid.setWidget(row++, 1, fromEmail);

    grid.setText(row, 0, "To");
    grid.setWidget(row++, 1, toEmail);

    grid.setText(row++, 0, "Message");
    final TextArea widget = new TextArea();
    widget.setCharacterWidth(50);
    widget.setVisibleLines(3);
    grid.setWidget(row, 0, widget);
    grid.getFlexCellFormatter().setColSpan(row, 0, 2);
    //String url = GWT.getHostPageBaseURL() + "#"+History.getToken();

    widget.setText("Hi,\n" +
      " Here's a link to : " + triple.section + ".\n");

    sendButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (fromEmail.getText().length() == 0 || !fromEmail.getText().contains("@")) {
          Window.alert("Please enter valid from email.");
        } else if (toEmail.getText().length() == 0 || !toEmail.getText().contains("@")) {
          Window.alert("Please enter valid to email.");
        }
        service.sendEmail(user.getUser(),
          fromEmail.getText(),
          toEmail.getText(),
          /*triple.type + " " +*/ triple.section,
          widget.getText(),
          History.getToken(),
          triple.section, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
              Window.alert("Couldn't contact server.");
              dialogBox.hide();
            }

            @Override
            public void onSuccess(Void result) {
              Window.alert("Message sent.");
              dialogBox.hide();

            }
          }
        );
      }
    });

    dialogVPanel.add(grid);
    dialogVPanel.add(hp);
    dialogBox.setWidget(dialogVPanel);

    // Add a handler to send the name to the server
    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
    dialogBox.center();
  }

  private void pushFirstSelection(String first) {
    String initToken = History.getToken();
    if (initToken.length() == 0) {
      ListBox listBox = typeToBox.get(first);
      String itemText = listBox.getItemText(listBox.getSelectedIndex());
      System.out.println("push first " + first + " select " + itemText);

      pushNewSectionHistoryToken(first, itemText);
    } else {
      System.out.println("fire history ");
      History.fireCurrentHistoryState();
    }
  }

  private List<String> getSections(Map<String, Collection<String>> result, String type) {
    List<String> sections = new ArrayList<String>(result.get(type));
    boolean allInt = !sections.isEmpty();
    for (String s : sections) {
      try {
        Integer.parseInt(s);
      } catch (NumberFormatException e) {
        allInt = false;
        break;
      }
    }
    if (allInt) {
      Collections.sort(sections, new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          int first = Integer.parseInt(o1);
          int second = Integer.parseInt(o2);
          return first < second ? -1 : first > second ? +1 : 0;
        }
      });
    } else {
      Collections.sort(sections);
    }
    return sections;
  }

  private void noSectionsGetExercises(long userID) {
    super.getExercises(userID);
  }

  private void loadExercises(final String type, final String section, final String item) {
    System.out.println("loadExercises " + type + " " + section + " item " +item);
    feedback.setMailtoWithHistory(type,section,History.getToken());
    service.getExercisesForSection(type, section, new SetExercisesCallback() {
      @Override
      public void onSuccess(List<ExerciseShell> result) {
        if (!result.isEmpty()) {
          if (item != null) {
            rememberExercises(result);
            if (!loadByID(item)) {
              loadFirstExercise();
            }
          }
          else {
            super.onSuccess(result);
          }
        }
      }
    });
  }

  private void pushNewSectionHistoryToken(String type, String section) {
    System.out.println("------------ push history " + type +"/"+ section + " -------------- ");
    History.newItem("type=" + type + ";section=" + section);
  }

  @Override
  protected void gotClickOnItem(ExerciseShell e) {
//    String historyToken = getHistoryToken(e.getID());
//    System.out.println("------------ push history " + historyToken+ " -------------- ");

    //History.newItem(historyToken, false);

    //super.gotClickOnItem(e);
  }

  @Override
  protected String getHistoryTokenForLink(String id) {
    return "#"+getHistoryToken(id);
  }

  protected String getHistoryToken(String id) {
    for (RadioButton rb : radios) {
      if (rb.getValue()) {
        String type = rb.getText();
        ListBox listBox = typeToBox.get(type);
        String section = listBox.getItemText(listBox.getSelectedIndex());
        // System.out.println("------------ push history " + type +"/"+ section + "/" +id+ " -------------- ");

        return "type=" + type + ";section=" + section + ";item=" + id;
      }
    }
    return "";
  }

  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    String token = event.getValue();
    System.out.println("onValueChange " + token);
    if (token.startsWith("type")) {
      try {
        Triple triple = getTriple(token);

        restoreRadioButtonState(triple.type);
        restoreListBoxState(triple.type, triple.section);

        loadExercises(triple.type, triple.section, triple.item);
      } catch (Exception e) {
        System.out.println("onValueChange " + token + " badly formed.");
      }
    }
    else {
      super.onValueChange(event);
    }
  }

  private Triple getTriple(String token) {
    String[] parts = token.split(";");
    String typePart = parts[0];
    String sectionPart = parts[1];
    String type = typePart.split("=")[1].trim();
    String section = sectionPart.split("=")[1].trim();
    String item = null;
    if (parts.length == 3) {
      item = parts[2].split("=")[1].trim();
    }
    return new Triple(type, section, item);
  }

  private static class Triple {
    String type, section, item;

    public Triple(String type, String section, String item) { this.type = type; this.section = section; this.item = item;}
  }

  private void restoreRadioButtonState(String type) {
    boolean found = false;
    for (RadioButton rb : radios) {
      boolean foundMatch = rb.getText().equals(type);
      if (foundMatch) found = true;
      rb.setValue(foundMatch);
    }
    if (!found && !radios.isEmpty()) { // so if we get a bad type, something is selected
      radios.get(0).setValue(true);
    }
  }

  private void restoreListBoxState(String type, String section) {
    for (ListBox lb : typeToBox.values()) {
      lb.setEnabled(false);
    }
    ListBox listBox = typeToBox.get(type);
    listBox.setEnabled(true);
    for (int i = 0; i < listBox.getItemCount(); i++) {
      String itemText = listBox.getItemText(i);
      if (itemText.equals(section)) {
        listBox.setSelectedIndex(i);
        break;
      }
    }
  }


  private static final int HEIGHT_OF_CELL_TABLE_WITH_15_ROWS = 390 - 20-65;

  protected int getTableHeaderHeight() {
    return 625 - HEIGHT_OF_CELL_TABLE_WITH_15_ROWS;
  }
}
