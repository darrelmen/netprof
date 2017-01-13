package mitll.langtest.client.angularComponent;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.ContextPractice;

/**
 * Created by je24276 on 1/6/17.
 */
public class ACWindow implements ACViewer{

    private final LangTestDatabaseAsync service;

    public ACWindow(LangTestDatabaseAsync service) {
        this.service = service;
    }

    public void viewAC(final HasWidgets contentPanel){
        contentPanel.clear();
        final FlowPanel container = new FlowPanel();
        HTML acContent = new HTML("Hi there! {{ test }}");
        container.add(acContent);
        contentPanel.add(container);
    }

}
