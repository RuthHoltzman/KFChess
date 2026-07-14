package kfchess.view;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnimationClipCache {

    private static final Map<String, AnimationClip> cache = new ConcurrentHashMap<>();

    public static AnimationClip get(String spritesFolder, int framesPerSec, boolean isLoop) {
        return cache.computeIfAbsent(spritesFolder,
            folder -> new AnimationClip(folder, framesPerSec, isLoop));
    }
}