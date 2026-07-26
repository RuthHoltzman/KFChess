package kfchess.bus;

import kfchess.model.PieceColor;

/**
 * מתפרסם בתחילת/סוף משחק - ה-UI יכול להאזין כדי להריץ אנימציית
 * פתיחה/סיום. winner הוא null כש-phase == STARTED.
 */
public record GameLifecycleEvent(Phase phase, PieceColor winner) implements GameEvent {
    public enum Phase { STARTED, ENDED }
}