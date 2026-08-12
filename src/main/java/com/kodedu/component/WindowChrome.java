package com.kodedu.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

/**
 * Custom undecorated window chrome (title bar + resize) that follows the active UI theme.
 */
public final class WindowChrome {

    public static final double TITLE_BAR_HEIGHT = 36;
    private static final double RESIZE_MARGIN = 5;

    private WindowChrome() {
    }

    public static void prepareStage(Stage stage) {
        if (stage.getStyle() == StageStyle.DECORATED) {
            stage.initStyle(StageStyle.UNDECORATED);
        }
    }

    public static Parent decorate(Stage stage, Parent content) {
        BorderPane chromeRoot = new BorderPane();
        chromeRoot.getStyleClass().add("window-chrome-root");

        HBox titleBar = createTitleBar(stage);
        chromeRoot.setTop(titleBar);
        chromeRoot.setCenter(content);

        installFocusStyle(stage, chromeRoot);
        installResizeSupport(stage, chromeRoot);

        return chromeRoot;
    }

    private static HBox createTitleBar(Stage stage) {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("window-title-bar");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPrefHeight(TITLE_BAR_HEIGHT);
        titleBar.setMinHeight(TITLE_BAR_HEIGHT);
        titleBar.setMaxHeight(TITLE_BAR_HEIGHT);
        titleBar.setPadding(new Insets(0, 0, 0, 10));

        ImageView iconView = new ImageView();
        iconView.setFitWidth(16);
        iconView.setFitHeight(16);
        iconView.setPreserveRatio(true);
        if (!stage.getIcons().isEmpty()) {
            iconView.setImage(stage.getIcons().get(0));
        } else {
            stage.getIcons().addListener((javafx.collections.ListChangeListener<Image>) change -> {
                if (!stage.getIcons().isEmpty()) {
                    iconView.setImage(stage.getIcons().get(0));
                }
            });
        }

        Label titleLabel = new Label();
        titleLabel.getStyleClass().add("window-title-label");
        titleLabel.textProperty().bind(stage.titleProperty());
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleLabel.setPadding(new Insets(0, 8, 0, 8));

        Button minimizeButton = captionButton("window-btn-minimize", "—", e -> stage.setIconified(true));
        Button maximizeButton = captionButton("window-btn-maximize", "□", e -> toggleMaximized(stage));
        Button closeButton = captionButton("window-btn-close", "✕", e ->
                stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)));
        closeButton.getStyleClass().add("window-btn-close");

        stage.maximizedProperty().addListener((obs, was, isMax) ->
                maximizeButton.setText(Boolean.TRUE.equals(isMax) ? "❐" : "□"));

        Region dragSpacer = new Region();
        HBox.setHgrow(dragSpacer, Priority.ALWAYS);

        // Keep title + icon in a drag region; buttons stay interactive
        HBox dragRegion = new HBox(8, iconView, titleLabel);
        dragRegion.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(dragRegion, Priority.ALWAYS);
        installDrag(stage, dragRegion);

        titleBar.getChildren().addAll(dragRegion, minimizeButton, maximizeButton, closeButton);
        return titleBar;
    }

    private static Button captionButton(String styleClass, String text, javafx.event.EventHandler<MouseEvent> onAction) {
        Button button = new Button(text);
        button.getStyleClass().addAll("window-btn", styleClass);
        button.setFocusTraversable(false);
        button.setMinWidth(46);
        button.setPrefWidth(46);
        button.setMaxHeight(Double.MAX_VALUE);
        button.setOnMouseClicked(onAction);
        return button;
    }

    private static void installDrag(Stage stage, Pane dragRegion) {
        final Delta drag = new Delta();

        dragRegion.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (stage.isMaximized()) {
                return;
            }
            drag.x = event.getScreenX() - stage.getX();
            drag.y = event.getScreenY() - stage.getY();
        });

        dragRegion.setOnMouseDragged(event -> {
            if (event.getButton() != MouseButton.PRIMARY || stage.isMaximized()) {
                return;
            }
            stage.setX(event.getScreenX() - drag.x);
            stage.setY(event.getScreenY() - drag.y);
        });

        dragRegion.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                toggleMaximized(stage);
            }
        });
    }

    private static void toggleMaximized(Stage stage) {
        stage.setMaximized(!stage.isMaximized());
        if (stage.isMaximized()) {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
        }
    }

    private static void installFocusStyle(Stage stage, Parent chromeRoot) {
        Runnable sync = () -> {
            chromeRoot.getStyleClass().remove("window-inactive");
            if (!stage.isFocused()) {
                chromeRoot.getStyleClass().add("window-inactive");
            }
        };
        stage.focusedProperty().addListener((obs, was, isFocused) -> sync.run());
        sync.run();
    }

    private static void installResizeSupport(Stage stage, BorderPane chromeRoot) {
        chromeRoot.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (stage.isMaximized()) {
                chromeRoot.setCursor(Cursor.DEFAULT);
                return;
            }
            Cursor cursor = cursorFor(event.getX(), event.getY(), chromeRoot.getWidth(), chromeRoot.getHeight());
            chromeRoot.setCursor(cursor);
        });

        final ResizeSession session = new ResizeSession();

        chromeRoot.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (stage.isMaximized() || event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            Cursor cursor = cursorFor(event.getX(), event.getY(), chromeRoot.getWidth(), chromeRoot.getHeight());
            if (cursor == Cursor.DEFAULT) {
                session.active = false;
                return;
            }
            session.active = true;
            session.cursor = cursor;
            session.screenX = event.getScreenX();
            session.screenY = event.getScreenY();
            session.stageX = stage.getX();
            session.stageY = stage.getY();
            session.stageW = stage.getWidth();
            session.stageH = stage.getHeight();
            event.consume();
        });

        chromeRoot.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!session.active || stage.isMaximized()) {
                return;
            }
            double dx = event.getScreenX() - session.screenX;
            double dy = event.getScreenY() - session.screenY;
            Cursor c = session.cursor;

            double newX = session.stageX;
            double newY = session.stageY;
            double newW = session.stageW;
            double newH = session.stageH;

            if (c == Cursor.NW_RESIZE || c == Cursor.W_RESIZE || c == Cursor.SW_RESIZE) {
                newX = session.stageX + dx;
                newW = session.stageW - dx;
            }
            if (c == Cursor.NE_RESIZE || c == Cursor.E_RESIZE || c == Cursor.SE_RESIZE) {
                newW = session.stageW + dx;
            }
            if (c == Cursor.NW_RESIZE || c == Cursor.N_RESIZE || c == Cursor.NE_RESIZE) {
                newY = session.stageY + dy;
                newH = session.stageH - dy;
            }
            if (c == Cursor.SW_RESIZE || c == Cursor.S_RESIZE || c == Cursor.SE_RESIZE) {
                newH = session.stageH + dy;
            }

            double minW = Math.max(stage.getMinWidth(), 400);
            double minH = Math.max(stage.getMinHeight(), 300);
            if (newW < minW) {
                if (c == Cursor.NW_RESIZE || c == Cursor.W_RESIZE || c == Cursor.SW_RESIZE) {
                    newX = session.stageX + session.stageW - minW;
                }
                newW = minW;
            }
            if (newH < minH) {
                if (c == Cursor.NW_RESIZE || c == Cursor.N_RESIZE || c == Cursor.NE_RESIZE) {
                    newY = session.stageY + session.stageH - minH;
                }
                newH = minH;
            }

            stage.setX(newX);
            stage.setY(newY);
            stage.setWidth(newW);
            stage.setHeight(newH);
            event.consume();
        });

        chromeRoot.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> session.active = false);
    }

    private static Cursor cursorFor(double x, double y, double width, double height) {
        boolean left = x < RESIZE_MARGIN;
        boolean right = x > width - RESIZE_MARGIN;
        boolean top = y < RESIZE_MARGIN;
        boolean bottom = y > height - RESIZE_MARGIN;

        if (left && top) {
            return Cursor.NW_RESIZE;
        }
        if (right && top) {
            return Cursor.NE_RESIZE;
        }
        if (left && bottom) {
            return Cursor.SW_RESIZE;
        }
        if (right && bottom) {
            return Cursor.SE_RESIZE;
        }
        if (left) {
            return Cursor.W_RESIZE;
        }
        if (right) {
            return Cursor.E_RESIZE;
        }
        if (top) {
            return Cursor.N_RESIZE;
        }
        if (bottom) {
            return Cursor.S_RESIZE;
        }
        return Cursor.DEFAULT;
    }

    private static final class Delta {
        private double x;
        private double y;
    }

    private static final class ResizeSession {
        private boolean active;
        private Cursor cursor = Cursor.DEFAULT;
        private double screenX;
        private double screenY;
        private double stageX;
        private double stageY;
        private double stageW;
        private double stageH;
    }
}
