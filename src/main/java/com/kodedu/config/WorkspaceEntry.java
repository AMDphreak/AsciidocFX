package com.kodedu.config;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A persisted workdir the user can reopen from the workspace index.
 */
public class WorkspaceEntry {

    private final StringProperty path = new SimpleStringProperty();
    private final BooleanProperty open = new SimpleBooleanProperty(false);

    public WorkspaceEntry() {
    }

    public WorkspaceEntry(String path, boolean open) {
        setPath(path);
        setOpen(open);
    }

    public String getPath() {
        return path.get();
    }

    public StringProperty pathProperty() {
        return path;
    }

    public void setPath(String path) {
        this.path.set(path);
    }

    public boolean isOpen() {
        return open.get();
    }

    public BooleanProperty openProperty() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open.set(open);
    }

    public String displayName() {
        String value = getPath();
        if (value == null || value.isBlank()) {
            return "";
        }
        Path parsed = Path.of(value);
        Path name = parsed.getFileName();
        return name != null ? name.toString() : value;
    }

    @Override
    public String toString() {
        String name = displayName();
        return isOpen() ? name + "  (open)" : name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkspaceEntry that)) {
            return false;
        }
        return Objects.equals(getPath(), that.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPath());
    }
}
