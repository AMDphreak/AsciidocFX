package com.kodedu.component;

/**
 * Where the Copilot chat sits relative to the document.
 * Default is {@link #WORKDIR_PEEK}: a clipped peek at the bottom of the files pane.
 */
public enum CopilotDock {
    WORKDIR_PEEK,
    RIGHT_OF_PREVIEW,
    LEFT_OF_DOCUMENT,
    TOP_OF_WORKSPACE,
    BOTTOM_OF_WINDOW,
    FLOAT
}
