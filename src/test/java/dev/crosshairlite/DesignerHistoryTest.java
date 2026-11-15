package dev.crosshairlite;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DesignerHistoryTest {
    @Test void undoRedoRestoresWholeStrokeAndNewStrokeClearsRedo() {
        DesignerHistory history=new DesignerHistory();boolean[] drawing=new boolean[1089];
        history.snapshot(drawing);drawing[544]=true;drawing[545]=true;
        drawing=history.undo(drawing);assertFalse(drawing[544]);assertFalse(drawing[545]);
        drawing=history.redo(drawing);assertTrue(drawing[544]);assertTrue(drawing[545]);
        drawing=history.undo(drawing);history.snapshot(drawing);drawing[0]=true;
        drawing=history.redo(drawing);assertTrue(drawing[0]);assertFalse(drawing[544]);
    }
    @Test void snapshotsAreIndependentAndHistoryIsBounded() {
        DesignerHistory history=new DesignerHistory();boolean[] drawing=new boolean[1089];
        for(int i=0;i<40;i++){history.snapshot(drawing);drawing[i]=true;}
        for(int i=0;i<40;i++)drawing=history.undo(drawing);
        assertTrue(drawing[7]);assertFalse(drawing[8]);
    }
}
