package piece;
import main.GamePanel;
import main.Type;
public class King extends Piece{
    public King(int color,int col,int row) {
        super(color,col,row);
        type=Type.KING;
        if(color==GamePanel.WHITE) {
            image=getImage("w-king");
        }else {
            image=getImage("b-king");
        }
    }
    public boolean canMove(int targetCol,int targetRow) {
        if(isWithinBoard(targetCol,targetRow)) {
            // Standard King move: 1 square in any direction
            if((Math.abs(targetCol-preCol)+Math.abs(targetRow-preRow)==1)||(Math.abs(targetCol-preCol)==1&&Math.abs(targetRow-preRow)==1)) {
                if(isValidSquare(targetCol,targetRow))
                return true; 
            }
        }
         
        // Castling logic
        if(moved==false) {
            // Right Castling
            if(targetCol==preCol+2&&targetRow==preRow&&pieceIsOnStraightLine(targetCol,targetRow)==false) {
                for(Piece piece:GamePanel.simPieces) {
                    if(piece.col==preCol+3&&piece.row==preRow&&piece.type==Type.ROOK&&piece.moved==false) {
                        GamePanel.castlingP=piece;
                        return true;
                    }
                }
            }
         // Left Castling 
         if(targetCol == preCol - 2 && targetRow == preRow) { // No need for pieceIsOnStraightLine here, it's checked below
             // 1. Check if King path is clear
             if (pieceIsOnStraightLine(targetCol, targetRow) == false) {
                 // 2. Check for the Rook at A-file (preCol-4)
                 for(Piece piece : GamePanel.simPieces) {
                     if(piece.col == preCol - 4 && piece.row == preRow && piece.type == Type.ROOK && piece.moved == false) {
                         // 3. Check if B-file (preCol-3) is also empty for Queen-side castling
                         if(getHittingP(preCol - 3, preRow) == null) {
                             GamePanel.castlingP = piece; // Set the rook to be moved
                             return true;
                         }
                     }
                 }
             }
         }
         // ...
        }
        return false;
    }
}