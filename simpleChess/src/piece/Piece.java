package piece;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import main.Board;
import main.GamePanel;
import main.Type;

public class Piece {
    public Type type;
    public BufferedImage image;
    public int x, y;
    public int col, row, preRow, preCol;
    public int color;
    public Piece hittingP;
    public boolean moved = false;
    public boolean twoStepped;

    public Piece(int color, int col, int row) {
        this.color = color;
        this.col = col;
        this.row = row;
        this.preCol = col;
        this.preRow = row;
        x = getX(col);
        y = getY(row);
    }

    public BufferedImage getImage(String ImagePath) {
        BufferedImage image = null;
        try {
            // *** CRITICAL CORRECTION: Using /pieces/ to match the resource structure ***
            image = ImageIO.read(getClass().getResourceAsStream("/pieces/" + ImagePath + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }
    
    public int getX(int col) { return col * Board.sqSize; }
    public int getY(int row) { return row * Board.sqSize; }
    public int getCol(int x) { return (x + Board.half) / Board.sqSize; }
    public int getRow(int y) { return (y + Board.half) / Board.sqSize; }
    
    public void resetPosition() {
        this.col = this.preCol;
        this.row = this.preRow;
        this.x = getX(this.col);
        this.y = getY(this.row);
    }
    
    public void updatePosition() {
        if (type == Type.PAWN) {
            if (Math.abs(row - preRow) == 2) twoStepped = true;
        }
        x = getX(col);
        y = getY(row);
        preCol = getCol(x);
        preRow = getRow(y);
        moved = true;
    }
    
    public boolean isWithinBoard(int targetCol, int targetRow) {
        return targetCol >= 0 && targetCol <= 7 && targetRow >= 0 && targetRow <= 7;
    }

    public Piece getHittingP(int targetCol, int targetRow) {
         for (Piece piece : GamePanel.simPieces) {
             if (piece.col == targetCol && piece.row == targetRow && piece != this) {
                 return piece;
             }
         }
         return null;
    }

    public int getIndex() {
         for (int i = 0; i < GamePanel.simPieces.size(); i++) {
             if (GamePanel.simPieces.get(i) == this) return i;
         }
         return -1;
    }
    
    public boolean isSameSquare(int targetCol, int targetRow) {
        return targetCol == preCol && targetRow == preRow;
    }
    
    public boolean isValidSquare(int targetCol, int targetRow) {
         hittingP = getHittingP(targetCol, targetRow);
         if (hittingP == null) return true;
         if (hittingP.color != this.color) return true;
         hittingP = null;
         return false;
    }
    
 // In piece.Piece.java - Only the corrected methods shown

 // ➡️ CRITICAL FIX: This method should return TRUE if the path is BLOCKED.
 public boolean pieceIsOnDiagonalLine(int targetCol, int targetRow) {
     if (Math.abs(targetCol - preCol) != Math.abs(targetRow - preRow)) return false; // Not a diagonal move

     int colOffset = (targetCol > preCol) ? 1 : -1;
     int rowOffset = (targetRow > preRow) ? 1 : -1;
     int checkCol = preCol + colOffset;
     int checkRow = preRow + rowOffset;
     
     while (checkCol != targetCol) {
         for (Piece piece : GamePanel.simPieces) {
             if (piece.col == checkCol && piece.row == checkRow) {
                 return true; // PATH IS BLOCKED
             }
         }
         checkCol += colOffset;
         checkRow += rowOffset;
     }
     return false; // PATH IS CLEAR
 }

 // ➡️ CRITICAL FIX: This method should return TRUE if the path is BLOCKED.
 public boolean pieceIsOnStraightLine(int targetCol, int targetRow) {
     if (preRow == targetRow) { // Horizontal movement
         int colOffset = (targetCol > preCol) ? 1 : -1;
         for (int c = preCol + colOffset; c != targetCol; c += colOffset) {
             for (Piece piece : GamePanel.simPieces) {
                 if (piece.col == c && piece.row == targetRow) {
                     return true; // PATH IS BLOCKED
                 }
             }
         }
     } else if (preCol == targetCol) { // Vertical movement
         int rowOffset = (targetRow > preRow) ? 1 : -1;
         for (int r = preRow + rowOffset; r != targetRow; r += rowOffset) {
             for (Piece piece : GamePanel.simPieces) {
                 if (piece.col == targetCol && piece.row == r) {
                     return true; // PATH IS BLOCKED
                 }
             }
         }
     } else {
         return false; // Not a straight line move (or same square), path cannot be blocked straight
     }
     return false; // PATH IS CLEAR
 }


 public boolean canMove(int targetCol, int targetRow) {
     return isWithinBoard(targetCol, targetRow); 
 }

    public void draw(Graphics2D g2) {
        g2.drawImage(image, x, y, Board.sqSize, Board.sqSize, null);
    }
    
    public Piece copy() {
        Piece copy = null;
        try {
            copy = this.getClass()
                    .getDeclaredConstructor(int.class, int.class, int.class)
                    .newInstance(this.color, this.col, this.row);
            copy.moved = this.moved;
            copy.twoStepped = this.twoStepped;
            copy.type = this.type;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return copy;
    }
}