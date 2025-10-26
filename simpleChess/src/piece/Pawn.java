package piece;

import main.GamePanel;
import main.Type;

public class Pawn extends Piece {
    public Pawn(int color,int col,int row) {
        super(color,col,row);
        type=Type.PAWN;
        if(color==GamePanel.WHITE) {
            image=getImage("w-pawn");
        }else {
            image=getImage("b-pawn");
        }
    }
 // In piece.Pawn.java
    public boolean canMove(int targetCol,int targetRow) {
        
        if(isWithinBoard(targetCol,targetRow) && isSameSquare(targetCol,targetRow) == false) {
            
            int moveValue = color == GamePanel.WHITE ? -1 : 1;
            hittingP = getHittingP(targetCol,targetRow);
            
            // 1. Movement by 1 square (No piece in target square)
            if(targetCol == preCol && targetRow == preRow + moveValue && hittingP == null) {
                return true;
            }
            
            // 2. Movement by 2 squares (Initial move only, no piece in the way)
            if(targetCol == preCol && targetRow == preRow + moveValue * 2 
                && hittingP == null 
                && moved == false 
                && pieceIsOnStraightLine(targetCol,targetRow) == false) { // ➡️ FIX: Needs to be FALSE for clear path
                return true;
            }
            
            // 3. Standard capture
            if(Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue 
                && hittingP != null && hittingP.color != this.color) {
                return true;
            }
            
            // 4. En Passant
            if(Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue && hittingP == null) {
                for(Piece piece : GamePanel.simPieces) {
                    // Check if opponent pawn is on the side, has twoStepped, and is capturable
                    if(piece.col == targetCol && piece.row == preRow && piece.type == Type.PAWN && piece.twoStepped == true) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}