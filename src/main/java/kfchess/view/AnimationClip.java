package kfchess.view;

import java.io.File;
import java.util.*;

public class AnimationClip {

    private final List<String> framePaths;
    private final int framesPerSec;
    private final boolean isLoop;

    public AnimationClip(String spritesFolder, int framesPerSec, boolean isLoop) {
        this.framesPerSec = framesPerSec;
        this.isLoop = isLoop;
        this.framePaths = loadSortedFramePaths(spritesFolder);
    }

    private List<String> loadSortedFramePaths(String folder) {
        File dir = new File(folder);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
        if (files == null) {
            throw new IllegalStateException(
                    "Missing animation folder: " + folder
                            + " (expected one .png file per frame, e.g. 0.png, 1.png, ...)");
        }
        if (files.length == 0) {
            throw new IllegalStateException("Animation folder is empty: " + folder);
        }
        Arrays.sort(files, Comparator.comparingInt(file -> {
            String name = file.getName();
            return Integer.parseInt(name.substring(0, name.length() - 4));
        }));
        List<String> paths = new ArrayList<>();
        for (File file : files) {
            paths.add(file.getPath());
        }
        return paths;
    }

    public int frameCount() {
        return framePaths.size();
    }

    // מחזיר את אינדקס הפריים שצריך להיות מוצג, לפי כמה זמן (במילישניות) עבר
    public int getFrameIndex(long elapsedMillis) {
        int index = (int) (elapsedMillis / (1000L / framesPerSec));
        int count = framePaths.size();
        return isLoop ? index % count : Math.min(index, count - 1);
        
    }

    public String getFramePath(int index) {
        return framePaths.get(index);
    }
}
