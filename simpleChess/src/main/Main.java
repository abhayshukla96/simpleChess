package main;
import javax.swing.JFrame;

public class Main {
	// In your Main class (outside of GamePanel.java)
	public static void main(String[] args) {
	    JFrame window = new JFrame("Chess Game");
	    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    window.setResizable(false);

	    GamePanel gp = new GamePanel();
	    
	    window.add(gp);
	    
	    // ➡️ CRITICAL FIX: The pack() call MUST happen after adding the panel.
	    window.pack(); 

	    window.setLocationRelativeTo(null);
	    window.setVisible(true);

	    gp.launchGame();
	} }
