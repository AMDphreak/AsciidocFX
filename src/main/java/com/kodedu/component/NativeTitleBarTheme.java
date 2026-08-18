package com.kodedu.component;

import com.sun.javafx.stage.WindowHelper;
import com.sun.javafx.tk.TKStage;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Syncs the native OS title bar with the app light/dark theme.
 * <p>
 * Prefers {@code Scene.getPreferences().setColorScheme()} when the running JavaFX build
 * exposes it (JavaFX 25+ with JDK-8362091). Falls back to {@code DwmSetWindowAttribute}
 * on Windows for builds that do not yet ship scene preferences.
 */
public final class NativeTitleBarTheme {

    private static final Logger logger = LoggerFactory.getLogger(NativeTitleBarTheme.class);
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private static final boolean WINDOWS = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT).contains("win");

    private static MethodHandle dwmSetWindowAttribute;
    private static boolean dwmLookupFailed;

    static {
        if (WINDOWS) {
            try {
                Linker linker = Linker.nativeLinker();
                SymbolLookup lookup = SymbolLookup.libraryLookup("dwmapi", Arena.global());
                MemorySegment symbol = lookup.find("DwmSetWindowAttribute").orElseThrow();
                dwmSetWindowAttribute = linker.downcallHandle(symbol,
                        FunctionDescriptor.of(ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_LONG,
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT));
            } catch (Throwable t) {
                dwmLookupFailed = true;
                logger.debug("DWM title bar theme unavailable: {}", t.toString());
            }
        }
    }

    private NativeTitleBarTheme() {
    }

    public static void apply(Scene scene, boolean dark) {
        if (scene == null) {
            return;
        }
        if (applyViaScenePreferences(scene, dark)) {
            return;
        }
        if (scene.getWindow() instanceof Stage stage) {
            applyWindowsDwm(stage, dark);
            return;
        }
        scene.windowProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Window> observable, Window oldValue, Window window) {
                if (window instanceof Stage stage) {
                    observable.removeListener(this);
                    applyWindowsDwm(stage, dark);
                }
            }
        });
    }

    public static void apply(Stage stage, boolean dark) {
        if (stage == null) {
            return;
        }
        Scene scene = stage.getScene();
        if (scene != null && applyViaScenePreferences(scene, dark)) {
            return;
        }
        applyWindowsDwm(stage, dark);
    }

    private static boolean applyViaScenePreferences(Scene scene, boolean dark) {
        try {
            Method getPreferences = Scene.class.getMethod("getPreferences");
            Object preferences = getPreferences.invoke(scene);
            Method setColorScheme = preferences.getClass().getMethod("setColorScheme", ColorScheme.class);
            setColorScheme.invoke(preferences, dark ? ColorScheme.DARK : ColorScheme.LIGHT);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        } catch (ReflectiveOperationException e) {
            logger.debug("Scene color scheme preference failed: {}", e.toString());
            return false;
        }
    }

    private static void applyWindowsDwm(Stage stage, boolean dark) {
        if (!WINDOWS || dwmLookupFailed || dwmSetWindowAttribute == null) {
            return;
        }
        Runnable apply = () -> {
            try {
                TKStage peer = WindowHelper.getPeer(stage);
                if (peer == null) {
                    return;
                }
                long hwnd = peer.getRawHandle();
                if (hwnd == 0L) {
                    return;
                }
                try (Arena arena = Arena.ofConfined()) {
                    MemorySegment value = arena.allocate(ValueLayout.JAVA_INT);
                    value.set(ValueLayout.JAVA_INT, 0, dark ? 1 : 0);
                    int result = (int) dwmSetWindowAttribute.invoke(
                            hwnd,
                            DWMWA_USE_IMMERSIVE_DARK_MODE,
                            value,
                            (int) ValueLayout.JAVA_INT.byteSize());
                    if (result != 0) {
                        logger.debug("DwmSetWindowAttribute returned {}", result);
                    }
                }
            } catch (Throwable t) {
                logger.debug("Windows title bar theme failed: {}", t.toString());
            }
        };
        if (stage.isShowing()) {
            apply.run();
        } else {
            ChangeListener<Boolean> listener = new ChangeListener<>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean showing) {
                    if (showing) {
                        observable.removeListener(this);
                        Platform.runLater(apply);
                    }
                }
            };
            stage.showingProperty().addListener(listener);
        }
    }
}
