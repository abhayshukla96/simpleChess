package main;

import java.awt.Color;
import java.awt.Graphics2D;

public class Board {
    public static final int sqSize = 100;
    public static final int half = sqSize / 2;

    public void draw(Graphics2D g2) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                g2.setColor((c + r) % 2 == 0 ? new Color(210, 165, 125) : new Color(175, 115, 70));
                g2.fillRect(c * sqSize, r * sqSize, sqSize, sqSize);
            }
        }
    }
}