package piece;
import main.GamePanel;
import main.Type;
public class Queen extends Piece {
    public Queen(int color,int col,int row) {
        super(color,col,row);
        type=Type.QUEEN;
        if(color==GamePanel.WHITE) {
            image=getImage("w-queen");
        }else {
            image=getImage("b-queen");
        }
    }
    public boolean canMove(int targetCol,int targetRow) {
        if(isWithinBoard(targetCol,targetRow)) {
            // Straight move
            if((Math.abs(targetCol-preCol)*Math.abs(targetRow-preRow)==0)){
                if((pieceIsOnStraightLine(targetCol,targetRow)==false))
                if(isValidSquare(targetCol,targetRow)&&isSameSquare(targetCol,targetRow)==false) {
                    return true;
                }
            }
            // Diagonal move
            else if((Math.abs(targetCol-preCol)==Math.abs(targetRow-preRow))){
                if(pieceIsOnDiagonalLine(targetCol,targetRow)==false) {
                    if(isValidSquare(targetCol,targetRow)&&isSameSquare(targetCol,targetRow)==false) {
                        return true;
                    }
                }
            }
        }return false;
    }
}