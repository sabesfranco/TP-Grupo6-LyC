package lyc.compiler.utils;

public class BranchHelper {

    private BranchHelper() {}

    public static int getFirstComparatorPosition(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '<' || c == '>' || c == '=') {
                return i;
            }
            if (c == '!' && i + 1 < text.length() && text.charAt(i + 1) == '=') {
                return i;
            }
        }
        return -1;
    }

    public static String extractComparator(String simpleCondition) {
        if (simpleCondition.endsWith("==") || simpleCondition.endsWith("!=") 
                || simpleCondition.endsWith("<=") || simpleCondition.endsWith(">=")) {
            return simpleCondition.substring(simpleCondition.length() - 2);
        }
        return simpleCondition.substring(simpleCondition.length() - 1);
    }

    public static String getBranchInstruction(String comparator) {
        if (comparator.equals("<")) return "BGE";
        if (comparator.equals("<=")) return "BGT";
        if (comparator.equals(">")) return "BLE";
        if (comparator.equals(">=")) return "BLT";
        if (comparator.equals("==")) return "BNE";
        if (comparator.equals("!=")) return "BEQ";
        return null;
    }

    public static String getOppositeBranchInstruction(String instruction) {
        if (instruction.equals("BGE")) return "BLT";
        if (instruction.equals("BGT")) return "BLE";
        if (instruction.equals("BLE")) return "BGT";
        if (instruction.equals("BLT")) return "BGE";
        if (instruction.equals("BNE")) return "BEQ";
        if (instruction.equals("BEQ")) return "BNE";
        return null;
    }
}
