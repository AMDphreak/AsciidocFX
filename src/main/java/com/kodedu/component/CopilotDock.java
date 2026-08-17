package com.kodedu.component;

/**
 * Where the Copilot chat sits relative to the document.
 * Default is {@link #RIGHT_OF_PREVIEW}: editor and preview stay together.
 */
public enum CopilotDock {
    RIGHT_OF_PREVIEW,
    LEFT_OF_DOCUMENT,
    TOP_OF_WORKSPACE,
    BOTTOM_OF_WINDOW,
    FLOAT
}
