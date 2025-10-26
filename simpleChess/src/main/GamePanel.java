package main;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;
import javax.swing.*;

import piece.*;

public class GamePanel extends JPanel implements Runnable {
    public static final int WIDTH = 1100;
    public static final int HEIGHT = 800;
    private final int FPS = 60;
    private Thread threadGame;

    private Board board = new Board();
    private Mouse mouse; 
    
    public static CopyOnWriteArrayList<Piece> piece = new CopyOnWriteArrayList<>();
    public static CopyOnWriteArrayList<Piece> simPieces = new CopyOnWriteArrayList<>();
    private ArrayList<Piece> promoPieces = new ArrayList<>();
    private Piece activeP, checkingP;
    public static Piece castlingP;
    
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    private int currentColor = WHITE;

    private boolean promotion;
    private boolean canMove;
    private boolean validSquare;
    private boolean gameover;
    private boolean stalemate;
    
    private volatile boolean aiIsMoving = false; 

    private Stockfish stockfish = new Stockfish();


    public GamePanel() {
        this.mouse = new Mouse(this); 
        
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.black);
        

        setFocusable(true); 

        addMouseMotionListener(mouse);
        addMouseListener(mouse);
        
        setPieces();
        copyPieces(piece, simPieces);
        
        stockfish.startEngine("C:\\stockfish\\stockfish\\stockfish-windows-x86-64-avx2.exe"); 
    }

    public void launchGame() {
        threadGame = new Thread(this);
        threadGame.start();
    }

    private void setPieces() {
        // White
        piece.add(new Pawn(WHITE, 0, 6)); piece.add(new Pawn(WHITE, 1, 6)); piece.add(new Pawn(WHITE, 2, 6));
        piece.add(new Pawn(WHITE, 3, 6)); piece.add(new Pawn(WHITE, 4, 6)); piece.add(new Pawn(WHITE, 5, 6));
        piece.add(new Pawn(WHITE, 6, 6)); piece.add(new Pawn(WHITE, 7, 6));
        piece.add(new Rook(WHITE, 0, 7)); piece.add(new Rook(WHITE, 7, 7));
        piece.add(new Knight(WHITE, 1, 7)); piece.add(new Knight(WHITE, 6, 7));
        piece.add(new Bishop(WHITE, 2, 7)); piece.add(new Bishop(WHITE, 5, 7));
        piece.add(new Queen(WHITE, 3, 7));
        piece.add(new King(WHITE, 4, 7));

        // Black
        piece.add(new Pawn(BLACK, 0, 1)); piece.add(new Pawn(BLACK, 1, 1)); piece.add(new Pawn(BLACK, 2, 1));
        piece.add(new Pawn(BLACK, 3, 1)); piece.add(new Pawn(BLACK, 4, 1)); piece.add(new Pawn(BLACK, 5, 1));
        piece.add(new Pawn(BLACK, 6, 1)); piece.add(new Pawn(BLACK, 7, 1));
        piece.add(new Rook(BLACK, 0, 0)); piece.add(new Rook(BLACK, 7, 0));
        piece.add(new Knight(BLACK, 1, 0)); piece.add(new Knight(BLACK, 6, 0));
        piece.add(new Bishop(BLACK, 2, 0)); piece.add(new Bishop(BLACK, 5, 0));
        piece.add(new Queen(BLACK, 3, 0));
        piece.add(new King(BLACK, 4, 0));
    }

    private void copyPieces(CopyOnWriteArrayList<Piece> src, CopyOnWriteArrayList<Piece> dest) {
        dest.clear();
        for (Piece p : src) dest.add(p.copy());
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / 60; // 60 FPS (in nanoseconds)
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;
        
        while (threadGame!= null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update(); // Game logic update
                repaint(); // Calls paintComponent
                delta--;
            }
        }
    }

    private void update() {
        if (gameover || stalemate || promotion || aiIsMoving) {
            if (promotion) promoting();
            return;
        }


        if (mouse.pressedFired) {
            if (activeP == null) {
                
                selectActivePiece(); 
            }
            mouse.pressedFired = false;
        }

        // 2. Checking for continuous dragging
        if (mouse.pressed && activeP != null) {
            simulate(); // Only simulate if a piece is already active and the button is held
        }

        // 3. Checking for release
        if (!mouse.pressed && activeP != null) handleMoveRelease();
    }
      private void selectActivePiece() {
        
        // debug: Print mouse click coordinates and calculated square.
        int clickedCol = mouse.x / Board.sqSize;
        int clickedRow = mouse.y / Board.sqSize;
        
        System.out.println("--- MOUSE PRESS DEBUG ---");
        System.out.println("Raw Click: (" + mouse.x + ", " + mouse.y + ")");
        System.out.println("Calculated Square: Col=" + clickedCol + ", Row=" + clickedRow);
        System.out.println("Current Color: " + (currentColor == WHITE ? "WHITE" : "BLACK"));
        
        // ----------------------------------------------------
        
        for (Piece p : simPieces) {
            if (p.color == currentColor &&
                p.col == clickedCol && 
                p.row == clickedRow) {  
                
               
                System.out.println("SUCCESS: Found Piece: " + p.type + " at (" + p.col + ", " + p.row + ")");
                activeP = p;
                activeP.preCol = activeP.col;
                activeP.preRow = activeP.row;
                break;
            }
        }
        
        if (activeP == null) {
            System.out.println("FAILURE: No piece found at that square.");
        }
    }
        private void handleMoveRelease() {
        if (validSquare) {
            // Apply move to actual pieces
            copyPieces(simPieces, piece);
            activeP.updatePosition();
            if (castlingP != null) castlingP.updatePosition();
            
            // Check end game conditions
            if (isKingInCheck()) {
                if (isCheckMate()) gameover = true;
            } else {
                if (isStalemate()) stalemate = true;
            }
            
            // Check for promotion before changing turn
            if (gameover == false && stalemate == false) {
                if (canPromote()) promotion = true;
                else {
                    changePlayer();
                    if (currentColor == BLACK) {
                        aiIsMoving = true;
                        new Thread(this::makeAIMove).start();
                    }
                }
            }
            activeP = null;
        } else {
            // Invalid move, revert
            copyPieces(piece, simPieces);
            activeP.resetPosition();
            activeP = null;
        }
    }

    private String getFEN(CopyOnWriteArrayList<Piece> boardPieces, int turnColor) {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            int emptyCount = 0;
            for (int col = 0; col < 8; col++) {
                Piece p = null;
                for (Piece piece : boardPieces) {
                    if (piece.row == row && piece.col == col) { p = piece; break; }
                }

                if (p == null) emptyCount++;
                else {
                    if (emptyCount > 0) { sb.append(emptyCount); emptyCount = 0; }
                    
                    char c = p.type.name().charAt(0);
                    if (p.type == Type.KNIGHT) c = 'N'; 
                    if (p.color == BLACK) c = Character.toLowerCase(c);
                    sb.append(c);
                }
            }
            if (emptyCount > 0) sb.append(emptyCount);
            if (row < 7) sb.append('/');
        }
        
        sb.append(turnColor == WHITE ? " w " : " b ");
        sb.append("KQkq - 0 1"); 
        return sb.toString();
    }

    private void makeAIMove() {
        try { 
            Thread.sleep(500); 
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        String fen = getFEN(piece, currentColor); 
        String bestMove = stockfish.getBestMove(fen, 1000); 
        
        if (!bestMove.equals("none") && bestMove.length() >= 4) {
            int fromCol = bestMove.charAt(0) - 'a'; 
            int fromRow = 8 - Character.getNumericValue(bestMove.charAt(1)); 
            int toCol = bestMove.charAt(2) - 'a'; 
            int toRow = 8 - Character.getNumericValue(bestMove.charAt(3)); 
            
            Piece movingPiece = null;
            for (Piece p : piece) {
                if (p.col == fromCol && p.row == fromRow) {
                    movingPiece = p;
                    break;
                }
            }
            
            if (movingPiece != null) {
                // 1. Capture opponent piece if present
                Piece captured = null;
                for (Piece p : piece) {
                    // Standard capture
                    if (p.col == toCol && p.row == toRow && p.color != currentColor) {
                        captured = p;
                        break;
                    } 
                    // En Passant capture: target square is empty, but a pawn is on target col/source row
                    if (movingPiece.type == Type.PAWN && Math.abs(fromCol - toCol) == 1 
                        && p.col == toCol && p.row == fromRow && p.type == Type.PAWN && p.color != currentColor) {
                        captured = p;
                        break;
                    }
                }
                if (captured != null) {
                    piece.remove(captured);
                }
                
                // 2. Handle Castling (Rook moves separately)
                if (movingPiece.type == Type.KING && Math.abs(fromCol - toCol) == 2) {
                    int rookFromCol = (toCol == 6) ? 7 : 0; 
                    int rookToCol = (toCol == 6) ? 5 : 3; 
                    Piece rook = null;
                    for(Piece p : piece) {
                        if(p.col == rookFromCol && p.row == fromRow && p.type == Type.ROOK) {
                            rook = p;
                            break;
                        }
                    }
                    if(rook != null) {
                        rook.col = rookToCol;
                        rook.updatePosition();
                    }
                }
                
                // 3. Move the piece
                movingPiece.preCol = movingPiece.col;
                movingPiece.preRow = movingPiece.row;
                movingPiece.col = toCol;
                movingPiece.row = toRow;
                movingPiece.updatePosition();
                
                
                // 4. Handle Promotion
                if (bestMove.length() == 5 && movingPiece.type == Type.PAWN) {
                    char promoChar = bestMove.charAt(4);
                    Type promoType = (promoChar == 'r') ? Type.ROOK :
                                     (promoChar == 'b') ? Type.BISHOP :
                                     (promoChar == 'n') ? Type.KNIGHT : Type.QUEEN;
                    
                    Piece newPiece = switch (promoType) {
                        case QUEEN -> new Queen(currentColor, toCol, toRow);
                        case ROOK -> new Rook(currentColor, toCol, toRow);
                        case BISHOP -> new Bishop(currentColor, toCol, toRow);
                        case KNIGHT -> new Knight(currentColor, toCol, toRow);
                        default -> null;
                    };

                    if (newPiece != null) {
                        piece.remove(movingPiece);
                        piece.add(newPiece);
                    }
                }
                
                copyPieces(piece, simPieces);

                if (isKingInCheck()) {
                    if (isCheckMate()) gameover = true;
                } else {
                    if (isStalemate()) stalemate = true;
                }
                
                changePlayer();
            }
        }
        
        aiIsMoving = false;
        repaint();
    }
    
    // ---------------- Check/Mate/Stalemate Logic ----------------
    
    
    private boolean tryMove(Piece piece, int targetCol, int targetRow) {
        // Backup states
        int preCol = piece.col;
        int preRow = piece.row;
        Piece hittingP = piece.hittingP;
        boolean moved = piece.moved;
        boolean twoStepped = piece.twoStepped;

        // Simulate move
        piece.col = targetCol;
        piece.row = targetRow;
        
        // Remove hitting piece from simPieces if a capture occurred
        int hittingPIndex = -1;
        if (hittingP != null) {
            hittingPIndex = simPieces.indexOf(hittingP);
            if (hittingPIndex != -1) simPieces.remove(hittingPIndex);
        }
        
        // Check if the King is still in check
        boolean illegalMove = isIllegal(getKing(false)); 

        // Revert move
        piece.col = preCol;
        piece.row = preRow;
        piece.hittingP = hittingP;
        piece.moved = moved;
        piece.twoStepped = twoStepped;
        
        // Re-add captured piece
        if (hittingPIndex != -1) simPieces.add(hittingPIndex, hittingP);

        return !illegalMove;
    }

    // Checks if the move is legal (i.e., doesn't result in the King being attacked)
    private boolean isValidMoveForCheckAndIllegal(Piece piece, int targetCol, int targetRow) {
        if (piece.canMove(targetCol, targetRow)) {
            return tryMove(piece, targetCol, targetRow);
        }
        return false;
    }

    public boolean isKingInCheck() {
        Piece king = getKing(false);
        // Check active piece (during human move)
        if (activeP != null && activeP.canMove(king.col, king.row)) {
            checkingP = activeP;
            return true;
        }
        // Check all opponent pieces
        for (Piece p : simPieces) {
            if (p.color != currentColor) { 
                if (p.canMove(king.col, king.row)) {
                    checkingP = p;
                    return true;
                }
            }
        }
        checkingP = null;
        return false;
    }

    public boolean isCheckMate() {
        if (!isKingInCheck()) return false;
        
        // Check if any piece can make a legal move to escape check
        for (Piece piece : simPieces) {
            if (piece.color == currentColor) {
                for (int targetCol = 0; targetCol < 8; targetCol++) {
                    for (int targetRow = 0; targetRow < 8; targetRow++) {
                        if (isValidMoveForCheckAndIllegal(piece, targetCol, targetRow)) {
                            return false; // Found a move that resolves the check
                        }
                    }
                }
            }
        }
        return true; // No legal moves found to escape check
    }

    public boolean isStalemate() {
        if (isKingInCheck()) return false; 
        
        // Check if the current player has ANY legal move
        for (Piece piece : simPieces) {
            if (piece.color == currentColor) {
                for (int targetCol = 0; targetCol < 8; targetCol++) {
                    for (int targetRow = 0; targetRow < 8; targetRow++) {
                        if (isValidMoveForCheckAndIllegal(piece, targetCol, targetRow)) {
                            return false; // Found a legal move, not stalemate
                        }
                    }
                }
            }
        }
        return true; // No legal moves found, and not in check
    }

    // ---------------- Utility Methods ----------------

    private Piece getKing(boolean opponent) {
        for (Piece p : simPieces) {
            if (p.type == Type.KING) {
                if (opponent && p.color != currentColor) return p;
                if (!opponent && p.color == currentColor) return p;
            }
        }
        return null;
    }

    private void promoting() {
        if (mouse.pressed) {
            for (Piece p : promoPieces) {
                if (p.col == mouse.x / Board.sqSize && p.row == mouse.y / Board.sqSize) {
                    Piece newPiece = switch (p.type) {
                        case ROOK -> new Rook(currentColor, activeP.col, activeP.row);
                        case KNIGHT -> new Knight(currentColor, activeP.col, activeP.row);
                        case BISHOP -> new Bishop(currentColor, activeP.col, activeP.row);
                        case QUEEN -> new Queen(currentColor, activeP.col, activeP.row);
                        default -> null;
                    };
                    
                    if (newPiece != null) {
                        simPieces.remove(activeP.getIndex());
                        simPieces.add(newPiece);
                        copyPieces(simPieces, piece);
                        
                        activeP = null;
                        promotion = false;
                        
                        changePlayer();
                        
                        if (currentColor == BLACK) {
                            aiIsMoving = true;
                            new Thread(this::makeAIMove).start();
                        }
                    }
                }
            }
        }
    }

    private boolean isIllegal(Piece king) {
        if (king.type != Type.KING) return false;
        // Check if any opponent piece can attack the King's current square
        for (Piece p : simPieces) {
             if (p.color != king.color && p.canMove(king.col, king.row)) {
                 return true;
             }
        }
        return false;
    }

    private void simulate() {
        canMove = false;
        validSquare = false;
        copyPieces(piece, simPieces);
        
        // Reset castling rook position for simulation
        if (castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }
        
              activeP.x = mouse.x - Board.half;
        activeP.y = mouse.y - Board.half;
        activeP.col = activeP.getCol(activeP.x);
        activeP.row = activeP.getRow(activeP.y);
        
        if (activeP.canMove(activeP.col, activeP.row)) {
            
            if (activeP.hittingP != null) simPieces.remove(activeP.hittingP.getIndex());

            // Check if the move is legal (doesn't leave king in check)
            if (!isIllegal(getKing(false))) { 
                canMove = true;
                validSquare = true;
            }
        }
    }
    
   
    private void changePlayer() {
        currentColor = (currentColor == WHITE ? BLACK : WHITE);
        activeP = null;
        for (Piece p : piece) if (p.color == currentColor) p.twoStepped = false;
    }

    private boolean canPromote() {
        if (activeP != null && activeP.type == Type.PAWN) {
            if ((currentColor == WHITE && activeP.row == 0) || (currentColor == BLACK && activeP.row == 7)) {
                promoPieces.clear();
                promoPieces.add(new Rook(currentColor, 9, 2));
                promoPieces.add(new Knight(currentColor, 9, 3));
                promoPieces.add(new Bishop(currentColor, 9, 4));
                promoPieces.add(new Queen(currentColor, 9, 5));
                return true;
            }
        }
        return false;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        
        board.draw(g2);
        
        for (Piece p : simPieces) {
            if (p != activeP) p.draw(g2);
        }

        if (activeP != null) {
            if (canMove) {
                // Highlight valid target square (now relies on validSquare which is set in simulate)
                g2.setColor(Color.white);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                g2.fillRect(activeP.col * Board.sqSize, activeP.row * Board.sqSize, Board.sqSize, Board.sqSize);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
            activeP.draw(g2);
        }

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font("Book Antiqua", Font.PLAIN, 40));
        g2.setColor(Color.white);

        if (promotion) {
            g2.drawString("Promote to:", 840, 150);
            for (Piece p : promoPieces)
                g2.drawImage(p.image, p.getX(p.col), p.getY(p.row), Board.sqSize, Board.sqSize, null);
        } else {
            if (currentColor == WHITE) {
                g2.drawString("White's turn", 840, 550);
                if (checkingP != null && checkingP.color == BLACK) {
                    g2.setColor(Color.red);
                    g2.drawString("The King", 840, 600);
                    g2.drawString("is in Check", 840, 650);
                }
            } else {
                g2.drawString("Black's turn (AI)", 840, 250);
                if (checkingP != null && checkingP.color == WHITE) {
                    g2.setColor(Color.red);
                    g2.drawString("The King", 840, 300);
                    g2.drawString("is in Check", 840, 350);
                }
            }
        }
        
        if (currentColor == BLACK && aiIsMoving && !gameover && !stalemate) {
            g2.setFont(new Font("Arial", Font.BOLD, 40));
            g2.setColor(Color.YELLOW);
            g2.drawString("AI Thinking...", 840, 50);
        }

        // Final Game Status Display
        if (gameover) {
            String s = currentColor == WHITE ? "Black wins" : "White wins";
            g2.setFont(new Font("Arial", Font.PLAIN, 90));
            g2.setColor(Color.green);
            g2.drawString(s, 200, 420);
            stockfish.stopEngine();
        }

        if (stalemate) {
            g2.setFont(new Font("Arial", Font.PLAIN, 90));
            g2.setColor(Color.lightGray);
            g2.drawString("Stalemate", 200, 420);
            stockfish.stopEngine();
        }
    }
}
