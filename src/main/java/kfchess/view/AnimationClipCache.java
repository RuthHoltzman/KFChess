package kfchess.view;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Caches AnimationClips by sprite folder, so frame paths are scanned from disk once instead of every render. */
public class AnimationClipCache {

    private static final Map<String, AnimationClip> cache = new ConcurrentHashMap<>();

    /** The cached clip for this sprite folder, loading it on first use. */
    public static AnimationClip get(String spritesFolder, int framesPerSec, boolean isLoop) {
        return cache.computeIfAbsent(spritesFolder,
            folder -> new AnimationClip(folder, framesPerSec, isLoop));
    }
}