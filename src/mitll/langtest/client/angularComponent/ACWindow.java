package mitll.langtest.client.angularComponent;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.ContextPractice;
import com.google.gwt.user.client.Window;

/**
 * Created by je24276 on 1/6/17.
 */
public class ACWindow implements ACViewer{

    private final LangTestDatabaseAsync service;

    public ACWindow(LangTestDatabaseAsync service) {
        this.service = service;
    }

    public void viewAC(final HasWidgets contentPanel){
        /*contentPanel.clear();
        final FlowPanel container = new FlowPanel();
        HTML acContent = new HTML("<div ng-app=\"myApp\">\n" +
                "<div ng-controller=\"MyAppCtrl\">\n" +
                "  {{variable}}\n" +
                "  <button ng-click=\"increment()\">increment</button>\n" +
                "</div>\n" +
                "</div>\n");
                //"<script type=\"text/javascript\" src=\"langtest/js/angular.js\"></script>\n"+
                //"<script type=\"text/javascript\" src=\"langtest/js/simple-app.js\"></script>");
        container.add(acContent);
        contentPanel.add(container);
        ScriptInjector.fromUrl("langtest/js/angular.js").setCallback(
                new Callback<Void, Exception>() {
                    @Override
                    public void onFailure(Exception e) {
                        Window.alert("Script load failed.");
                    }

                    @Override
                    public void onSuccess(Void aVoid) {
                        ScriptInjector.fromUrl("langtest/js/simple-app.js").setCallback(
                                new Callback<Void, Exception>() {
                                    @Override
                                    public void onFailure(Exception e) {
                                        Window.alert("Script load failed.");
                                    }

                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Window.alert("Script load succeeded!!");

                                    }
                                }
                        ).inject();
                    }
                }
        ).inject();*/

    }

}
