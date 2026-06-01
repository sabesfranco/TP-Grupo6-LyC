package lyc.compiler.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class IntermediateCode {

    private static List<String> lines = new ArrayList<>();
    private static Stack<Integer> pilaSaltos = new Stack<>();
    private static Stack<Integer> pilaWhile = new Stack<>();

    private IntermediateCode() {}

    public static void clear() {
        lines.clear();
        pilaSaltos.clear();
        pilaWhile.clear();
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

    // condicion en postfija + salto, guardamos en la pila para completar despues
    public static void emitBranch(String cond) {
        addLine(cond.trim());
        String salto = BranchHelper.getBranch(cond);
        int idx = addLine(salto + " ?");
        pilaSaltos.push(idx);
    }

    public static void closeIfWithoutElse() {
        int idx = pilaSaltos.pop();
        patchLine(idx, lines.size() + 1);
    }

    public static void closeIfThenForElse() {
        int idx = pilaSaltos.pop();
        patchLine(idx, lines.size() + 1);
        int bi = addLine("BI ?");
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
        patchLine(idx, lines.size() + 1);
        addLine("FIN");
    }
}
