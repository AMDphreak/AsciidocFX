package com.kodedu.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

/**
 * Drop zones shown while dragging Copilot. Hit-testing uses screen coordinates
 * so the overlay can stay mouse-transparent during the drag.
 */
public class CopilotDropOverlay extends StackPane {

    private final DropZone top = zone("Top of workspace", CopilotDock.TOP_OF_WORKSPACE);
    private final DropZone bottom = zone("Bottom of window", CopilotDock.BOTTOM_OF_WINDOW);
    private final DropZone left = zone("Left of editor", CopilotDock.LEFT_OF_DOCUMENT);
    private final DropZone right = zone("Right of preview", CopilotDock.RIGHT_OF_PREVIEW);
    private final DropZone center = zone("Pop-out window", CopilotDock.FLOAT);

    private CopilotDock hovered = CopilotDock.RIGHT_OF_PREVIEW;

    public CopilotDropOverlay() {
        getStyleClass().add("copilot-drop-overlay");
        setVisible(false);
        setMouseTransparent(true);

        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("copilot-drop-layout");
        layout.setTop(top);
        layout.setBottom(bottom);
        layout.setLeft(left);
        layout.setRight(right);
        layout.setCenter(center);

        top.prefHeightProperty().bind(heightProperty().multiply(0.20));
        bottom.prefHeightProperty().bind(heightProperty().multiply(0.20));
        left.prefWidthProperty().bind(widthProperty().multiply(0.18));
        right.prefWidthProperty().bind(widthProperty().multiply(0.18));

        getChildren().add(layout);
    }

    public void showZones() {
        setVisible(true);
        hover(CopilotDock.RIGHT_OF_PREVIEW);
    }

    public CopilotDock finish() {
        setVisible(false);
        hover(hovered);
        mark(top, false);
        mark(bottom, false);
        mark(left, false);
        mark(right, false);
        mark(center, false);
        return hovered;
    }

    public void hoverAtScreen(double screenX, double screenY) {
        hover(dockAt(screenX, screenY));
    }

    public CopilotDock dockAt(double screenX, double screenY) {
        if (containsScreen(right, screenX, screenY)) {
            return CopilotDock.RIGHT_OF_PREVIEW;
        }
        if (containsScreen(left, screenX, screenY)) {
            return CopilotDock.LEFT_OF_DOCUMENT;
        }
        if (containsScreen(top, screenX, screenY)) {
            return CopilotDock.TOP_OF_WORKSPACE;
        }
        if (containsScreen(bottom, screenX, screenY)) {
            return CopilotDock.BOTTOM_OF_WINDOW;
        }
        return CopilotDock.FLOAT;
    }

    private void hover(CopilotDock dock) {
        this.hovered = dock;
        mark(top, dock == CopilotDock.TOP_OF_WORKSPACE);
        mark(bottom, dock == CopilotDock.BOTTOM_OF_WINDOW);
        mark(left, dock == CopilotDock.LEFT_OF_DOCUMENT);
        mark(right, dock == CopilotDock.RIGHT_OF_PREVIEW);
        mark(center, dock == CopilotDock.FLOAT);
    }

    private static void mark(DropZone zone, boolean active) {
        zone.getStyleClass().remove("active");
        if (active) {
            zone.getStyleClass().add("active");
        }
    }

    private boolean containsScreen(DropZone zone, double screenX, double screenY) {
        javafx.geometry.Point2D local = zone.screenToLocal(screenX, screenY);
        return local != null && zone.contains(local);
    }

    private static DropZone zone(String text, CopilotDock dock) {
        DropZone box = new DropZone(dock);
        Label label = new Label(text);
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.getChildren().add(label);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    public static final class DropZone extends StackPane {
        private final CopilotDock dock;

        private DropZone(CopilotDock dock) {
            this.dock = dock;
            getStyleClass().add("copilot-drop-zone");
        }

        public CopilotDock dock() {
            return dock;
        }
    }
}
