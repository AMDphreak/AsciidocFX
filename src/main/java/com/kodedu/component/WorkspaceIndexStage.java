package com.kodedu.component;

import com.kodedu.config.StoredConfigBean;
import com.kodedu.config.WorkspaceEntry;
import com.kodedu.helper.IOHelper;
import com.kodedu.service.DirectoryService;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Sticky Notes-style index of persisted workspaces. Opening loads a workdir;
 * closing clears the session-restore flag without deleting the record.
 */
public class WorkspaceIndexStage {

    private final StoredConfigBean storedConfigBean;
    private final DirectoryService directoryService;
    private final Consumer<Stage> themeApplier;
    private Stage stage;
    private ListView<WorkspaceEntry> listView;

    public WorkspaceIndexStage(StoredConfigBean storedConfigBean, DirectoryService directoryService,
                               Consumer<Stage> themeApplier) {
        this.storedConfigBean = storedConfigBean;
        this.directoryService = directoryService;
        this.themeApplier = themeApplier;
    }

    public Stage getStage() {
        return stage;
    }

    public void show(Window owner) {
        if (stage == null) {
            build(owner);
        }
        refresh();
        if (!stage.isShowing()) {
            stage.show();
        }
        stage.toFront();
        stage.requestFocus();
    }

    private void build(Window owner) {
        stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("Workspaces");
        stage.setWidth(420);
        stage.setHeight(360);

        listView = new ListView<>(storedConfigBean.getWorkspaces());
        listView.setPlaceholder(new javafx.scene.control.Label("No saved workspaces"));
        listView.setCellFactory(view -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(WorkspaceEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item.toString());
                    setTooltip(new Tooltip(item.getPath()));
                }
            }
        });
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() > 1) {
                openSelected();
            }
        });

        Button open = new Button("Open");
        open.setOnAction(event -> openSelected());
        Button close = new Button("Close");
        close.setOnAction(event -> closeSelected());
        Button remove = new Button("Remove");
        remove.setOnAction(event -> removeSelected());

        HBox buttons = new HBox(8, open, close, remove);
        buttons.setPadding(new Insets(8));

        BorderPane root = new BorderPane();
        root.setCenter(listView);
        root.setBottom(buttons);
        root.setPadding(new Insets(8));

        stage.setScene(new Scene(root));
        if (themeApplier != null) {
            themeApplier.accept(stage);
        }
    }

    private void refresh() {
        if (listView != null) {
            listView.refresh();
        }
    }

    private void openSelected() {
        WorkspaceEntry selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getPath() == null) {
            return;
        }
        Path path = IOHelper.getPath(selected.getPath());
        storedConfigBean.activateWorkspace(path);
        directoryService.changeWorkigDir(path);
        refresh();
    }

    private void closeSelected() {
        WorkspaceEntry selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        storedConfigBean.closeWorkspace(selected.getPath());
        refresh();
    }

    private void removeSelected() {
        WorkspaceEntry selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        storedConfigBean.removeWorkspace(selected.getPath());
        refresh();
    }
}
