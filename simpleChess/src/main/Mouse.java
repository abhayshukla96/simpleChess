package main;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Mouse extends MouseAdapter {
    private GamePanel gp;
    public int x, y;
    public boolean pressed;
    public boolean pressedFired; 
    
    // ➡️ Define a common offset based on the JFrame title bar height
    private static final int Y_OFFSET = 30; // Start with 30 pixels

    public Mouse(GamePanel gp) {
        this.gp = gp;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // ➡️ FIX: Subtract the offset from Y to align with the panel's drawing origin
        x = e.getX();
        y = e.getY() - Y_OFFSET; 
        pressed = true;
        pressedFired = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        x = e.getX();
        y = e.getY() - Y_OFFSET; // ➡️ Apply offset
        pressed = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        x = e.getX();
        y = e.getY() - Y_OFFSET; // ➡️ Apply offset
    }
}