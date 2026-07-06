package lyc.compiler.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class IntermediateCode {

    private static List<String> lines = new ArrayList<>();
    private static Stack<Integer> pilaSaltos = new Stack<>();
    private static Stack<Integer> pilaWhile = new Stack<>();
    private static Stack<Integer> pilaWhileSpecial = new Stack<>();

    private IntermediateCode() {}

    public static void clear() {
        lines.clear();
        pilaSaltos.clear();
        pilaWhile.clear();
        pilaWhileSpecial.clear();
    }

    public static List<String> getLines() {
        return lines;
    }

    public static int addLine(String line) {
        lines.add(line);
        return lines.size() - 1;
    }

    private static void patchLine(int idx, int nroLinea) {
        String linea = lines.get(idx);
        lines.set(idx, linea.replace("?", String.valueOf(nroLinea)));
    }

    public static void emitAssign(String var, String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            addLine(var + " :=");
        } else {
            addLine(expr.trim() + " " + var + " :=");
        }
    }

    public static void emitRead(String var) {
        addLine(var + " read");
    }

    public static void emitWrite(String expr) {
        addLine(expr.trim() + " write");
    }
    
    private static String replaceComparatorWithCMP(String cond) {
        String comparator = BranchHelper.extractComparator(cond);
        return cond.substring(0, cond.length() - comparator.length()) + "CMP";
    }

    // condicion en postfija + salto, guardamos en la pila para completar despues
    public static void emitBranch(String cond) {
        String[] partes = cond.trim().split(" ");
        String ultimaParte = partes[partes.length - 1]; // comparador o conector lógico
        String branchInstruction = BranchHelper.getBranchInstruction(ultimaParte);

        if(branchInstruction != null) {
            // simple condition
            addLine(replaceComparatorWithCMP(cond));
            int idx = addLine(branchInstruction + " ?");
            pilaSaltos.push(idx);

        } else if (ultimaParte.equals("NOT")) {
            String updatedCond = cond.substring(0, cond.length() - 4);
            addLine(replaceComparatorWithCMP(updatedCond));
            branchInstruction = BranchHelper.getBranchInstruction(partes[partes.length - 2]);
            int idx = addLine(BranchHelper.getOppositeBranchInstruction(branchInstruction) + " ?");
            pilaSaltos.push(idx);

        } else if(ultimaParte.equals("AND")) {
            int firstComparatorPosition = BranchHelper.getFirstComparatorPosition(cond);
            String condition = cond.substring(0, firstComparatorPosition + 1);
            addLine(replaceComparatorWithCMP(condition));
            branchInstruction = BranchHelper.getBranchInstruction(BranchHelper.extractComparator(condition));
            addLine(branchInstruction + " ?");
           
            condition = cond.substring(firstComparatorPosition + 2, cond.length() - 4);
            addLine(replaceComparatorWithCMP(condition));
            branchInstruction = BranchHelper.getBranchInstruction(partes[partes.length - 2]);
            int idx = addLine(branchInstruction + " ?");
            pilaSaltos.push(idx);
            pilaSaltos.push(idx);

        } else if(ultimaParte.equals("OR")) {
            int firstComparatorPosition = BranchHelper.getFirstComparatorPosition(cond);
            String condition = cond.substring(0, firstComparatorPosition + 1);
            int idx = addLine(replaceComparatorWithCMP(condition));
            branchInstruction = BranchHelper.getBranchInstruction(BranchHelper.extractComparator(condition));
            addLine(BranchHelper.getOppositeBranchInstruction(branchInstruction) + " " + (idx + 5));
           
            condition = cond.substring(firstComparatorPosition + 2, cond.length() - 3);
            addLine(replaceComparatorWithCMP(condition));
            branchInstruction = BranchHelper.getBranchInstruction(partes[partes.length - 2]);
            idx = addLine(branchInstruction + " ?");
            pilaSaltos.push(idx);
        }
    }

    public static void closeIfWithoutElse() {
        int idx = pilaSaltos.pop();
        if(!pilaSaltos.isEmpty() && pilaSaltos.peek() == idx) {
            patchLine(idx - 2, lines.size() + 1);
        }
        patchLine(idx, lines.size() + 1);
    }

    public static void closeIfThenForElse() {
        int idx = pilaSaltos.pop();
        if(!pilaSaltos.isEmpty() && pilaSaltos.peek() == idx) {
            patchLine(idx - 2, lines.size() + 2);
        }
        int bi = addLine("BI ?");
        patchLine(idx, lines.size() + 1);
        pilaSaltos.push(bi);
    }

    public static void closeIfElse() {
        int bi = pilaSaltos.pop();
        patchLine(bi, lines.size() + 1);
    }

    public static void beginWhile() {
        pilaWhile.push(addLine("ET"));
    }

    public static void endWhile() {
        int et = pilaWhile.pop();
        addLine("BI " + (et + 1));
        int idx = pilaSaltos.pop();
        if(!pilaSaltos.isEmpty() && pilaSaltos.peek() == idx) {
            patchLine(idx - 2, lines.size() + 1);
        }
        patchLine(idx, lines.size() + 1);
        addLine("FIN");
    }
    
    public static void beginWhileSpecial() {
        pilaWhileSpecial.push(addLine("ET") + 1);
    }

    public static int peekWhileSpecial() {
        return pilaWhileSpecial.peek();
    }

    public static void endWhileSpecial() {
        int et = pilaWhileSpecial.pop();
        addLine("FIN");
    }
}
