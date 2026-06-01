package lyc.compiler.utils;

public class BranchHelper {

    private BranchHelper() {}

    // tabla del apunte: si falla la comparacion, que salto usar
    public static String getBranch(String condicion) {
        String[] partes = condicion.trim().split(" ");
        String op = partes[partes.length - 1];

        if (op.equals("<")) return "BGE";
        if (op.equals("<=")) return "BGT";
        if (op.equals(">")) return "BLE";
        if (op.equals(">=")) return "BLT";
        if (op.equals("==")) return "BNE";
        if (op.equals("!=")) return "BEQ";

        // AND, OR, NOT
        return "BEQ";
    }
}
