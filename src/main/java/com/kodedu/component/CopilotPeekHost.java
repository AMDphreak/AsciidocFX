package com.kodedu.component;

import com.kodedu.copilot.component.CopilotPanel;
import javafx.animation.PauseTransition;
import javafx.beans.InvalidationListener;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Files-pane Copilot peek: clipped to the workdir column, expanding as an overlay
 * over the board on hover or focus. Clips back after a short grace delay.
 */
public class CopilotPeekHost extends Pane {

    public static final double PEEK_HEIGHT = 136;
    private static final double OVERLAY_DEFAULT_WIDTH = 420;
    private static final double OVERLAY_DEFAULT_HEIGHT = 360;
    private static final double OVERLAY_MAX_WIDTH = 640;
    private static final double OVERLAY_MAX_HEIGHT = 480;
    private static final double RESIZE_EDGE = 10;

    private final CopilotPanel panel;
    private final Region peekSlot;
    private final StackPane workspaceFrame;
    private final Rectangle clip = new Rectangle();
    private final PauseTransition collapseDelay = new PauseTransition(Duration.millis(450));
    private final InvalidationListener relayoutListener = observable -> relayout();

    private boolean expanded;
    private boolean resizing;
    private double overlayWidth = OVERLAY_DEFAULT_WIDTH;
    private double overlayHeight = OVERLAY_DEFAULT_HEIGHT;
    private double resizeStartX;
    private double resizeStartY;
    private double resizeStartW;
    private double resizeStartH;

    public CopilotPeekHost(CopilotPanel panel, Region peekSlot, StackPane workspaceFrame) {
        this.panel = panel;
        this.peekSlot = peekSlot;
        this.workspaceFrame = workspaceFrame;
        getStyleClass().add("copilot-peek-host");
        setManaged(false);
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
        getChildren().add(panel);

        panel.setMinWidth(OVERLAY_DEFAULT_WIDTH);
        panel.setPrefWidth(OVERLAY_DEFAULT_WIDTH);
        panel.prefHeightProperty().bind(heightProperty());

        collapseDelay.setOnFinished(event -> {
            if (!shouldStayExpanded()) {
                collapse();
            }
        });

        addEventFilter(MouseEvent.MOUSE_ENTERED, event -> expand());
        addEventFilter(MouseEvent.MOUSE_MOVED, event -> updateResizeCursor(event));
        addEventFilter(MouseEvent.MOUSE_EXITED, event -> {
            if (!resizing) {
                collapseDelay.playFromStart();
            }
        });
        addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (expanded && onResizeEdge(event)) {
                resizing = true;
                collapseDelay.stop();
                resizeStartX = event.getScreenX();
                resizeStartY = event.getScreenY();
                resizeStartW = getWidth();
                resizeStartH = getHeight();
                event.consume();
            }
        });
        addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (resizing) {
                overlayWidth = clamp(resizeStartW + (event.getScreenX() - resizeStartX),
                        Math.max(160, peekWidth()), OVERLAY_MAX_WIDTH);
                overlayHeight = clamp(resizeStartH + (event.getScreenY() - resizeStartY),
                        PEEK_HEIGHT, OVERLAY_MAX_HEIGHT);
                relayout();
                event.consume();
            }
        });
        addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (resizing) {
                resizing = false;
                if (!isHover() && !panelHasFocus()) {
                    collapseDelay.playFromStart();
                }
                event.consume();
            }
        });

        peekSlot.layoutBoundsProperty().addListener(relayoutListener);
        peekSlot.localToSceneTransformProperty().addListener(relayoutListener);
        workspaceFrame.layoutBoundsProperty().addListener(relayoutListener);
        workspaceFrame.widthProperty().addListener(relayoutListener);
        workspaceFrame.heightProperty().addListener(relayoutListener);
        sceneProperty().addListener((observable, oldScene, newScene) -> relayout());
    }

    public void attachPanel() {
        if (panel.getParent() != this) {
            getChildren().setAll(panel);
            panel.setMinWidth(OVERLAY_DEFAULT_WIDTH);
            panel.setPrefWidth(OVERLAY_DEFAULT_WIDTH);
            panel.prefHeightProperty().bind(heightProperty());
        }
        setVisible(true);
        peekSlot.setVisible(true);
        peekSlot.setManaged(true);
        peekSlot.setMinHeight(PEEK_HEIGHT);
        peekSlot.setPrefHeight(PEEK_HEIGHT);
        peekSlot.setMaxHeight(PEEK_HEIGHT);
        collapse();
        relayout();
        toFront();
    }

    public void detachPanel() {
        collapseDelay.stop();
        panel.prefHeightProperty().unbind();
        getChildren().remove(panel);
        setVisible(false);
        peekSlot.setVisible(false);
        peekSlot.setManaged(false);
        peekSlot.setMinHeight(0);
        peekSlot.setPrefHeight(0);
        peekSlot.setMaxHeight(0);
    }

    public boolean hostsPanel() {
        return panel.getParent() == this;
    }

    private void expand() {
        collapseDelay.stop();
        if (!expanded) {
            expanded = true;
            getStyleClass().remove("expanded");
            getStyleClass().add("expanded");
        }
        relayout();
        toFront();
    }

    private void collapse() {
        resizing = false;
        expanded = false;
        getStyleClass().remove("expanded");
        setCursor(Cursor.DEFAULT);
        relayout();
    }

    private void relayout() {
        if (peekSlot == null || workspaceFrame == null || !isVisible() || peekSlot.getScene() == null) {
            return;
        }
        Bounds slotScene = peekSlot.localToScene(peekSlot.getBoundsInLocal());
        Bounds frameScene = workspaceFrame.localToScene(workspaceFrame.getBoundsInLocal());
        if (slotScene == null || frameScene == null || slotScene.getWidth() < 8) {
            return;
        }
        setLayoutX(slotScene.getMinX() - frameScene.getMinX());
        setLayoutY(slotScene.getMinY() - frameScene.getMinY());
        double peekW = Math.max(120, slotScene.getWidth());
        if (expanded) {
            double maxW = Math.max(peekW, Math.min(OVERLAY_MAX_WIDTH, frameScene.getWidth() - getLayoutX() - 12));
            double maxH = Math.max(PEEK_HEIGHT, Math.min(OVERLAY_MAX_HEIGHT, frameScene.getHeight() - getLayoutY() - 8));
            double width = clamp(overlayWidth, peekW, maxW);
            double height = clamp(overlayHeight, PEEK_HEIGHT, maxH);
            setMinSize(peekW, PEEK_HEIGHT);
            setPrefSize(width, height);
            setMaxSize(maxW, maxH);
            resize(width, height);
        } else {
            setMinSize(peekW, PEEK_HEIGHT);
            setPrefSize(peekW, PEEK_HEIGHT);
            setMaxSize(peekW, PEEK_HEIGHT);
            resize(peekW, PEEK_HEIGHT);
        }
    }

    private double peekWidth() {
        Bounds slotScene = peekSlot.localToScene(peekSlot.getBoundsInLocal());
        return slotScene == null ? 160 : Math.max(120, slotScene.getWidth());
    }

    private boolean shouldStayExpanded() {
        return resizing || isHover() || panelHasFocus();
    }

    private boolean panelHasFocus() {
        return panel.getScene() != null && panel.getScene().getFocusOwner() != null
                && isParentOf(panel.getScene().getFocusOwner(), panel);
    }

    private static boolean isParentOf(javafx.scene.Node node, javafx.scene.Node ancestor) {
        javafx.scene.Node current = node;
        while (current != null) {
            if (current == ancestor) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private boolean onResizeEdge(MouseEvent event) {
        return event.getX() >= getWidth() - RESIZE_EDGE || event.getY() >= getHeight() - RESIZE_EDGE;
    }

    private void updateResizeCursor(MouseEvent event) {
        if (!expanded) {
            setCursor(Cursor.DEFAULT);
            return;
        }
        boolean right = event.getX() >= getWidth() - RESIZE_EDGE;
        boolean bottom = event.getY() >= getHeight() - RESIZE_EDGE;
        if (right && bottom) {
            setCursor(Cursor.SE_RESIZE);
        } else if (right) {
            setCursor(Cursor.E_RESIZE);
        } else if (bottom) {
            setCursor(Cursor.S_RESIZE);
        } else {
            setCursor(Cursor.DEFAULT);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
