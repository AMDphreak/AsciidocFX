package com.kodedu.config;

import com.kodedu.controller.ApplicationController;
import com.kodedu.helper.IOHelper;
import com.kodedu.other.Item;
import com.kodedu.service.ThreadService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.json.*;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static com.kodedu.other.JsonHelper.getJsonArrayOrEmpty;

/**
 * Created by usta on 07.08.2015.
 */
@Component
public class StoredConfigBean extends ConfigurationBase {

    private final ApplicationController controller;
    private final ThreadService threadService;

    private StringProperty workingDirectory = new SimpleStringProperty();
    private ObservableList<Item> recentFiles = FXCollections.observableArrayList();
    private ObservableList<String> favoriteDirectories = FXCollections.observableArrayList();
    private ObservableList<WorkspaceEntry> workspaces = FXCollections.observableArrayList();


    @Override
    public String formName() {
        return "Stored Settings";
    }

    @Autowired
    public StoredConfigBean(ApplicationController controller, ThreadService threadService) {
        super(controller, threadService);
        this.controller = controller;
        this.threadService = threadService;
    }

    public String getWorkingDirectory() {
        return workingDirectory.get();
    }

    public StringProperty workingDirectoryProperty() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory.set(workingDirectory);
    }

    public ObservableList<Item> getRecentFiles() {
        return recentFiles;
    }

    public void setRecentFiles(ObservableList<Item> recentFiles) {
        this.recentFiles = recentFiles;
    }

    public ObservableList<String> getFavoriteDirectories() {
        return favoriteDirectories;
    }

    public void setFavoriteDirectories(ObservableList<String> favoriteDirectories) {
        this.favoriteDirectories = favoriteDirectories;
    }

    public ObservableList<WorkspaceEntry> getWorkspaces() {
        return workspaces;
    }

    public Optional<WorkspaceEntry> findOpenWorkspace() {
        return workspaces.stream().filter(WorkspaceEntry::isOpen).findFirst();
    }

    public void activateWorkspace(Path path) {
        if (path == null) {
            return;
        }
        String value = path.toString();
        for (WorkspaceEntry entry : workspaces) {
            entry.setOpen(value.equals(entry.getPath()));
        }
        Optional<WorkspaceEntry> existing = workspaces.stream()
                .filter(entry -> value.equals(entry.getPath()))
                .findFirst();
        if (existing.isPresent()) {
            existing.get().setOpen(true);
        } else {
            workspaces.add(new WorkspaceEntry(value, true));
        }
        setWorkingDirectory(value);
        save();
    }

    public void updateOpenWorkspacePath(Path path) {
        if (path == null) {
            return;
        }
        String value = path.toString();
        findOpenWorkspace().ifPresent(entry -> entry.setPath(value));
        setWorkingDirectory(value);
    }

    public void closeWorkspace(String path) {
        workspaces.stream()
                .filter(entry -> Objects.equals(path, entry.getPath()))
                .forEach(entry -> entry.setOpen(false));
        save();
    }

    public void removeWorkspace(String path) {
        workspaces.removeIf(entry -> Objects.equals(path, entry.getPath()));
        save();
    }

    public void migrateWorkingDirectoryIntoWorkspaces() {
        String current = getWorkingDirectory();
        if (workspaces.isEmpty() && current != null && !current.isBlank()) {
            workspaces.add(new WorkspaceEntry(current, true));
        }
    }

    @Override
    public VBox createForm() {
        return null;
    }

    @Override
    public Path getConfigPath() {
        return super.resolveConfigPath("stored_directories.json");
    }

    @Override
    public void load(Path configPath, ActionEvent... actionEvent) {

        createConfigFileIfNotExist(configPath);
        Reader fileReader = IOHelper.fileReader(configPath);
        JsonReader jsonReader = Json.createReader(fileReader);

        JsonObject jsonObject = jsonReader.readObject();

        JsonArray recentFiles = getJsonArrayOrEmpty(jsonObject, "recentFiles");
        JsonArray favoriteDirectories = getJsonArrayOrEmpty(jsonObject, "favoriteDirectories");
        JsonArray workspacesJson = getJsonArrayOrEmpty(jsonObject, "workspaces");
        String workingDirectory = jsonObject.getString("workingDirectory", System.getProperty("user.home"));

        IOHelper.close(jsonReader, fileReader);

        threadService.runActionLater(() -> {

            if (Objects.nonNull(recentFiles)) {
                recentFiles.stream().map(e -> (JsonString) e).map(e -> e.getString())
                        .map(e -> new Item(IOHelper.getPath(e)))
                        .forEach(this.recentFiles::add);
            }
            if (Objects.nonNull(favoriteDirectories)) {
                favoriteDirectories.stream().map(e -> (JsonString) e).map(e -> e.getString()).forEach(this.favoriteDirectories::add);
            }
            this.workspaces.clear();
            for (JsonValue value : workspacesJson) {
                if (value instanceof JsonObject workspaceObject) {
                    String path = workspaceObject.getString("path", null);
                    boolean open = workspaceObject.getBoolean("open", false);
                    if (path != null && !path.isBlank()) {
                        this.workspaces.add(new WorkspaceEntry(path, open));
                    }
                }
            }
            if (Objects.nonNull(workingDirectory)) {
                this.workingDirectory.setValue(workingDirectory);
            }
            migrateWorkingDirectoryIntoWorkspaces();
        });
    }

    @Override
    public void save(ActionEvent... actionEvent) {
        saveJson(getJSON());
    }

    @Override
    public JsonObject getJSON() {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

        JsonArrayBuilder recentFilesArrayBuilder = Json.createArrayBuilder();
        JsonArrayBuilder favoriteDirectoriesArrayBuilder = Json.createArrayBuilder();
        JsonArrayBuilder workspacesArrayBuilder = Json.createArrayBuilder();

        recentFiles.stream()
                .map(Item::getPath)
                .filter(Files::exists)
                .map(e -> e.toString())
                .forEach(recentFilesArrayBuilder::add);

        favoriteDirectories.stream()
                .forEach(favoriteDirectoriesArrayBuilder::add);

        workspaces.stream()
                .filter(entry -> entry.getPath() != null && !entry.getPath().isBlank())
                .forEach(entry -> workspacesArrayBuilder.add(Json.createObjectBuilder()
                        .add("path", entry.getPath())
                        .add("open", entry.isOpen())));

        objectBuilder
                .add("workingDirectory", getWorkingDirectory() == null ? "" : getWorkingDirectory())
                .add("recentFiles", recentFilesArrayBuilder)
                .add("favoriteDirectories", favoriteDirectoriesArrayBuilder)
                .add("workspaces", workspacesArrayBuilder);

        return objectBuilder.build();
    }
}
