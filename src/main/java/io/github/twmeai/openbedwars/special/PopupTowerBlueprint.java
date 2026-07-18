package io.github.twmeai.openbedwars.special;

import java.util.ArrayList;
import java.util.List;

final class PopupTowerBlueprint {
    private static final List<Offset> BLOCKS = createBlocks();

    private PopupTowerBlueprint() {
    }

    static List<Offset> blocks() {
        return BLOCKS;
    }

    private static List<Offset> createBlocks() {
        List<Offset> blocks = new ArrayList<>();
        for (int y = 0; y <= 4; y++) {
            for (int right = -2; right <= 2; right++) {
                for (int forward = -2; forward <= 2; forward++) {
                    if (Math.abs(right) != 2 && Math.abs(forward) != 2) continue;
                    if (forward == -2 && right == 0 && y <= 1) continue;
                    blocks.add(new Offset(right, y, forward, Kind.WOOL));
                }
            }
        }
        for (int right = -2; right <= 2; right++) {
            for (int forward = -2; forward <= 2; forward++) {
                blocks.add(new Offset(right, 5, forward, Kind.WOOL));
                if ((Math.abs(right) == 2 || Math.abs(forward) == 2) && (right + forward & 1) == 0) {
                    blocks.add(new Offset(right, 6, forward, Kind.WOOL));
                }
            }
        }
        for (int y = 0; y <= 4; y++) {
            blocks.add(new Offset(0, y, 1, Kind.LADDER));
        }
        return List.copyOf(blocks);
    }

    enum Kind {
        WOOL,
        LADDER
    }

    record Offset(int right, int y, int forward, Kind kind) {
    }
}
