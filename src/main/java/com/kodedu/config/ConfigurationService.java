package com.kodedu.config;

import com.kodedu.boot.AppStarter;
import com.kodedu.config.templates.TemplatesConfigBean;
import com.kodedu.copilot.config.CopilotConfigBean;
import com.kodedu.controller.ApplicationController;
import com.kodedu.service.ThreadService;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Settings chrome: Outline-style tabs, pinned Save/Load, optional pop-out window.
 */
@Component
public class ConfigurationService {

    private final ShortCutConfigBean shortCutConfigBean;
    private final LocationConfigBean locationConfigBean;
    private final EditorConfigBean editorConfigBean;
    private final PreviewConfigBean previewConfigBean;
    private final HtmlConfigBean htmlConfigBean;
    private final DocbookConfigBean docbookConfigBean;
    private final ApplicationController controller;
    private final StoredConfigBean storedConfigBean;
    private final ThreadService threadService;
    private final SpellcheckConfigBean spellcheckConfigBean;
    private final TerminalConfigBean terminalConfigBean;
    private final ExtensionConfigBean extensionConfigBean;
    private final Epub3ConfigBean epub3ConfigBean;
    private final RevealjsConfigBean revealjsConfigBean;
    private final PdfConfigBean pdfConfigBean;
    private final TemplatesConfigBean templatesConfigBean;
    private final FileHistoryConfigBean fileHistoryConfigBean;
    private final CopilotConfigBean copilotConfigBean;

    private VBox configBox;
    private VBox settingsShell;
    private final Button saveAllButton = new Button("Save all");
    private final List<ConfigurationBase> loadedBeans = new ArrayList<>();
    private Stage settingsStage;

    @Autowired
    public ConfigurationService(ShortCutConfigBean shortCutConfigBean, LocationConfigBean locationConfigBean, EditorConfigBean editorConfigBean,
                                PreviewConfigBean previewConfigBean, HtmlConfigBean htmlConfigBean,
                                DocbookConfigBean docbookConfigBean, ApplicationController controller,
                                StoredConfigBean storedConfigBean, ThreadService threadService,
                                SpellcheckConfigBean spellcheckConfigBean, TerminalConfigBean terminalConfigBean,
                                ExtensionConfigBean extensionConfigBean, Epub3ConfigBean epub3ConfigBean,
                                RevealjsConfigBean revealjsConfigBean, PdfConfigBean pdfConfigBean,
                                TemplatesConfigBean templatesConfigBean, FileHistoryConfigBean fileHistoryConfigBean,
                                CopilotConfigBean copilotConfigBean) {
        this.shortCutConfigBean = shortCutConfigBean;
        this.locationConfigBean = locationConfigBean;
        this.editorConfigBean = editorConfigBean;
        this.previewConfigBean = previewConfigBean;
        this.htmlConfigBean = htmlConfigBean;
        this.docbookConfigBean = docbookConfigBean;
        this.controller = controller;
        this.storedConfigBean = storedConfigBean;
        this.threadService = threadService;
        this.spellcheckConfigBean = spellcheckConfigBean;
        this.terminalConfigBean = terminalConfigBean;
        this.extensionConfigBean = extensionConfigBean;
        this.epub3ConfigBean = epub3ConfigBean;
        this.revealjsConfigBean = revealjsConfigBean;
        this.pdfConfigBean = pdfConfigBean;
        this.templatesConfigBean = templatesConfigBean;
        this.fileHistoryConfigBean = fileHistoryConfigBean;
        this.copilotConfigBean = copilotConfigBean;
    }

    public void loadConfigurations(Runnable... runnables) {

        shortCutConfigBean.load();
        locationConfigBean.load();
        storedConfigBean.load();
        fileHistoryConfigBean.load();
        editorConfigBean.load();
        previewConfigBean.load();
        htmlConfigBean.load();
        docbookConfigBean.load();
        spellcheckConfigBean.load();
        terminalConfigBean.load();
        extensionConfigBean.load();
        pdfConfigBean.load();
        epub3ConfigBean.load();
        revealjsConfigBean.load();
        templatesConfigBean.load();
        copilotConfigBean.load();

        List<ConfigurationBase> configBeanList = Arrays.asList(
                shortCutConfigBean,
                editorConfigBean,
                terminalConfigBean,
                locationConfigBean,
                previewConfigBean,
                htmlConfigBean,
                docbookConfigBean,
                extensionConfigBean,
                pdfConfigBean,
                epub3ConfigBean,
                revealjsConfigBean,
                templatesConfigBean,
                fileHistoryConfigBean,
                copilotConfigBean
        );

        TabPane settingsTabs = new TabPane();
        settingsTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        settingsTabs.getStyleClass().add("settings-tabs");
        if (controller.documentNavTabs != null) {
            controller.documentNavTabs.getStyleClass().stream()
                    .filter(style -> !"tab-pane".equals(style))
                    .forEach(style -> {
                        if (!settingsTabs.getStyleClass().contains(style)) {
                            settingsTabs.getStyleClass().add(style);
                        }
                    });
        }

        Map<ConfigurationBase, HBox> saveRows = new LinkedHashMap<>();
        HBox footer = new HBox(8);
        footer.getStyleClass().add("settings-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(6, 8, 8, 8));

        saveAllButton.setTooltip(new Tooltip("Save every settings page that has unsaved changes"));
        saveAllButton.getStyleClass().add("settings-save-all");
        saveAllButton.setOnAction(event -> saveAll());

        for (ConfigurationBase configBean : configBeanList) {
            VBox form = configBean.createForm();
            if (form == null) {
                continue;
            }
            form.setPadding(new Insets(0, 5, 5, 0));
            HBox saveRow = stripSaveRow(form);
            if (saveRow != null) {
                hookCleanOnSaveOrLoad(configBean, saveRow);
                saveRows.put(configBean, saveRow);
            }
            configBean.setOnConfigChanged(() -> {
                if (settingsShell != null && settingsShell.getScene() != null) {
                    configBean.markDirty();
                }
            });

            ScrollPane formScrollPane = new ScrollPane(form);
            formScrollPane.setFitToHeight(true);
            formScrollPane.setFitToWidth(true);
            VBox.setVgrow(formScrollPane, Priority.ALWAYS);

            Tab tab = new Tab(configBean.formName());
            tab.setClosable(false);
            tab.setGraphic(new FontIcon(iconFor(configBean.formName())));
            tab.setContent(formScrollPane);
            tab.setUserData(configBean);
            configBean.dirtyProperty().addListener((observable, wasDirty, dirty) ->
                    tab.setText(dirty ? configBean.formName() + " *" : configBean.formName()));
            settingsTabs.getTabs().add(tab);
            loadedBeans.add(configBean);
        }

        settingsTabs.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (newTab != null && newTab.getUserData() instanceof ConfigurationBase bean) {
                showSaveRow(footer, saveRows.get(bean));
            }
        });

        Button popOutButton = new Button();
        popOutButton.setGraphic(new FontIcon(FontAwesome.EXTERNAL_LINK));
        popOutButton.setTooltip(new Tooltip("Open settings in a new window"));
        popOutButton.getStyleClass().addAll("chrome-button", "settings-pop-out");
        popOutButton.setOnAction(event -> popOut());

        Label title = new Label("Settings");
        title.getStyleClass().add("settings-title");
        HBox header = new HBox(8, title, new Pane(), popOutButton);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 8, 4, 8));
        header.getStyleClass().add("settings-header");

        settingsShell = new VBox(header, settingsTabs, footer);
        settingsShell.getStyleClass().add("settings-shell");
        VBox.setVgrow(settingsTabs, Priority.ALWAYS);

        if (!settingsTabs.getTabs().isEmpty()
                && settingsTabs.getTabs().get(0).getUserData() instanceof ConfigurationBase first) {
            showSaveRow(footer, saveRows.get(first));
        }

        threadService.runActionLater(() -> {
            configBox = controller.getConfigBox();
            configBox.getChildren().setAll(settingsShell);
            VBox.setVgrow(settingsShell, Priority.ALWAYS);
            loadedBeans.forEach(ConfigurationBase::markClean);
            for (Runnable runnable : runnables) {
                runnable.run();
            }
        });
    }

    public boolean isPoppedOut() {
        return settingsStage != null && settingsStage.isShowing();
    }

    public Stage getSettingsStage() {
        return settingsStage;
    }

    public void popOut() {
        if (settingsShell == null) {
            return;
        }
        if (isPoppedOut()) {
            focusPopOut();
            return;
        }
        if (settingsStage == null) {
            settingsStage = new Stage();
            settingsStage.setTitle("Settings");
            settingsStage.initOwner(controller.getStage());
            try (InputStream logoStream = AppStarter.class.getResourceAsStream("/logo.png")) {
                if (logoStream != null) {
                    settingsStage.getIcons().add(new Image(logoStream));
                }
            } catch (Exception ignored) {
            }
            settingsStage.setOnCloseRequest(event -> dock(false));
        }
        if (settingsShell.getParent() instanceof Pane parent) {
            parent.getChildren().remove(settingsShell);
        }
        Scene scene = new Scene(settingsShell, 720, 640);
        settingsStage.setScene(scene);
        if (controller.getStage() != null && controller.getStage().getScene() != null) {
            scene.getStylesheets().setAll(controller.getStage().getScene().getStylesheets());
            scene.getRoot().setStyle(controller.getStage().getScene().getRoot().getStyle());
        }
        controller.applyCurrentTheme(settingsStage);
        settingsStage.show();
        settingsStage.toFront();
        controller.onSettingsPoppedOut();
    }

    public void focusPopOut() {
        if (settingsStage != null) {
            settingsStage.show();
            settingsStage.toFront();
        }
    }

    public void dock(boolean showDocked) {
        if (settingsShell == null || configBox == null) {
            return;
        }
        if (settingsStage != null) {
            settingsStage.hide();
            if (settingsStage.getScene() != null && settingsStage.getScene().getRoot() == settingsShell) {
                settingsStage.setScene(null);
            }
        }
        if (settingsShell.getParent() instanceof Pane parent) {
            parent.getChildren().remove(settingsShell);
        }
        if (!configBox.getChildren().contains(settingsShell)) {
            configBox.getChildren().setAll(settingsShell);
            VBox.setVgrow(settingsShell, Priority.ALWAYS);
        }
        controller.onSettingsDocked(showDocked);
    }

    private void saveAll() {
        for (ConfigurationBase bean : loadedBeans) {
            if (bean.isDirty()) {
                bean.save();
                bean.markClean();
            }
        }
    }

    private void showSaveRow(HBox footer, HBox saveRow) {
        if (saveAllButton.getParent() instanceof Pane parent) {
            parent.getChildren().remove(saveAllButton);
        }
        if (saveRow == null) {
            footer.getChildren().setAll(saveAllButton);
            return;
        }
        if (saveRow.getParent() instanceof Pane parent) {
            parent.getChildren().remove(saveRow);
        }
        int insertAt = 0;
        for (int i = 0; i < saveRow.getChildren().size(); i++) {
            if (saveRow.getChildren().get(i) instanceof Button button && "Save".equals(button.getText())) {
                insertAt = i + 1;
                break;
            }
        }
        saveRow.getChildren().add(insertAt, saveAllButton);
        footer.getChildren().setAll(saveRow);
    }

    private static HBox stripSaveRow(VBox form) {
        for (int i = form.getChildren().size() - 1; i >= 0; i--) {
            Node node = form.getChildren().get(i);
            if (node instanceof HBox box && containsSaveButton(box)) {
                form.getChildren().remove(i);
                return box;
            }
        }
        return null;
    }

    private static boolean containsSaveButton(HBox box) {
        return box.getChildren().stream().anyMatch(node ->
                node instanceof Button button && "Save".equals(button.getText()));
    }

    private static void hookCleanOnSaveOrLoad(ConfigurationBase bean, HBox saveRow) {
        for (Node node : saveRow.getChildren()) {
            if (node instanceof Button button
                    && ("Save".equals(button.getText()) || "Load".equals(button.getText()))) {
                button.addEventHandler(ActionEvent.ACTION, event -> bean.markClean());
            }
        }
    }

    private static FontAwesome iconFor(String formName) {
        String name = formName == null ? "" : formName.toLowerCase();
        if (name.contains("shortcut")) {
            return FontAwesome.KEYBOARD_O;
        }
        if (name.contains("editor")) {
            return FontAwesome.PENCIL;
        }
        if (name.contains("terminal")) {
            return FontAwesome.TERMINAL;
        }
        if (name.contains("location")) {
            return FontAwesome.FOLDER_OPEN_O;
        }
        if (name.contains("preview")) {
            return FontAwesome.EYE;
        }
        if (name.contains("html")) {
            return FontAwesome.HTML5;
        }
        if (name.contains("docbook")) {
            return FontAwesome.FILE_TEXT_O;
        }
        if (name.contains("extension")) {
            return FontAwesome.PUZZLE_PIECE;
        }
        if (name.contains("pdf")) {
            return FontAwesome.FILE_PDF_O;
        }
        if (name.contains("epub")) {
            return FontAwesome.BOOK;
        }
        if (name.contains("reveal")) {
            return FontAwesome.DESKTOP;
        }
        if (name.contains("template")) {
            return FontAwesome.FILES_O;
        }
        if (name.contains("history")) {
            return FontAwesome.HISTORY;
        }
        if (name.contains("copilot") || name.contains("ask")) {
            return FontAwesome.COMMENTING;
        }
        if (name.contains("spell")) {
            return FontAwesome.CHECK;
        }
        return FontAwesome.COG;
    }
}
