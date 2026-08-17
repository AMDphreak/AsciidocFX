= Fork notes (AMDphreak)

This fork tracks https://github.com/asciidocfx/AsciidocFX with local-run and
dark-preview work. The following items are **not solved**.

== Unsolved: themed custom title bar (upstream #679)

**Status:** unsolved. Do not ship custom window chrome.

Painting a JavaFX title bar (`StageStyle.UNDECORATED` or JavaFX 25 preview
`StageStyle.EXTENDED` + `HeaderBar`) left the window undraggable on Windows 11
in this fork: no move, and caption buttons often dead. Native OS decorations
are restored so the window can be moved and resized.

A themed caption that follows Dark/Light and still speaks Win32 hit-testing
(`WM_NCHITTEST` / snap / system menu) is still open. JavaFX 25 `HeaderBar` is
the intended API, but it did not work here. Do not reopen a PR until dragging
is proven on Windows.

Related: https://github.com/asciidocfx/AsciidocFX/issues/679
Retracted PR: https://github.com/asciidocfx/AsciidocFX/pull/691

== Still in this tree (not chrome)

* Dark preview CSS switching with the UI theme
* Ace/editor surface fill so the editor is not white-on-dark at startup
* `WindowPlacement`: restore last windowed size; first run ≈ 78% (`π/4`) of
  the work area, clamped on-screen
* Declarative `-Plocal-run` OpenJFX from Maven Central
