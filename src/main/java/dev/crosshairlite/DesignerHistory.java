package dev.crosshairlite;

import java.util.ArrayDeque;

/** One undo entry per stroke; a new edit invalidates redo. */
final class DesignerHistory {
    private final ArrayDeque<boolean[]> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    void snapshot(boolean[] drawing) {
        if(undo.size()>=32)undo.removeFirst();
        undo.addLast(drawing.clone());redo.clear();
    }
    boolean[] undo(boolean[] current) {
        if(undo.isEmpty())return current;
        redo.addLast(current.clone());return undo.removeLast();
    }
    boolean[] redo(boolean[] current) {
        if(redo.isEmpty())return current;
        undo.addLast(current.clone());return redo.removeLast();
    }
}
