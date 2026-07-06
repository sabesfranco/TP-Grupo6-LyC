package lyc.compiler.files;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lyc.compiler.utils.IntermediateCode;
import lyc.compiler.model.SymbolEntry;
import lyc.compiler.model.SymbolType;
import lyc.compiler.model.SymbolTable;

public class AsmCodeGenerator implements FileGenerator {

    private static final int MAX_TEXT_SIZE = 50;

    private static final Set<String> BRANCH_OPS = new HashSet<>(
        Arrays.asList("BGE", "BGT", "BLE", "BLT", "BNE", "BEQ")
    );

    private BufferedWriter asmBuffer;
    private Set<Integer> labels = new HashSet<>();
    private int opId = 0;

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        asmBuffer = new BufferedWriter(fileWriter);
        opId = 0;
        labels.clear();
        generateHeader();
        generateDataSection();
        generateCodeSection();
        generateFooter();
        asmBuffer.flush();
    }

    private void generateHeader() throws IOException {
        asmBuffer.write(
            "include number.asm\ninclude macros.asm\n\n" +
            ".MODEL LARGE\n" +
            ".386\n" +
            ".STACK 200h\n\n" +
            "MAXTEXTSIZE equ " + MAX_TEXT_SIZE + "\n\n"
        );
    }

    private void generateDataSection() throws IOException {
        asmBuffer.write(".DATA\n");
        String format = "\t%-" + MAX_TEXT_SIZE + "s %-3s %s\n";

        asmBuffer.write(String.format(format, "@aux1", "dd", "?"));
        asmBuffer.write(String.format(format, "@aux2", "dd", "?"));
        asmBuffer.write(String.format(format, "@i", "dd", "?"));

        for (SymbolEntry entry : SymbolTable.getInstance().getAll()) {
            SymbolType type = entry.getType();
            String name = entry.getName();
            String typeToDefine = (type == SymbolType.STRING) ? "db" : "dd";
            String valueToDefine;

            if (type == SymbolType.STRING) {
                if (entry.isConstant()) {
                    valueToDefine = entry.getValue() + ", '$'";
                } else {
                    valueToDefine = "MAXTEXTSIZE dup (?), '$'";
                }
            } else {
                valueToDefine = entry.isConstant() ? entry.getValue() : "?";
            }

            asmBuffer.write(String.format(format, name, typeToDefine, valueToDefine));
        }
    }

    private SymbolEntry findEntryByName(String name) {
        for (SymbolEntry e : SymbolTable.getInstance().getAll()) {
            if (e.getName().equals(name)) {
                return e;
            }
        }
        return null;
    }

    private void generateCodeSection() throws IOException {
        List<String> lines = IntermediateCode.getLines();
        collectJumpTargets(lines);

        asmBuffer.write("\n.CODE\n\nSTART:\n");
        asmBuffer.write("\tmov AX, @DATA\n");
        asmBuffer.write("\tmov DS, AX\n");
        asmBuffer.write("\tmov ES, AX\n\n");

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            int lineNumber = lineIndex + 1;
            if (labels.contains(lineNumber)) {
                asmBuffer.write("ET_" + lineNumber + ":\n");
            }
            generateLine(lines.get(lineIndex).trim());
        }

        int endLine = lines.size() + 1;
        if (labels.contains(endLine)) {
            asmBuffer.write("ET_" + endLine + ":\n");
        }
    }

    private void collectJumpTargets(List<String> lines) {
        for (String rawLine : lines) {
            String line = rawLine.trim();
            String[] parts = line.split("\\s+");
            int len = parts.length;

            if (line.startsWith("BI ") && len == 2 && isNumeric(parts[1])) {
                labels.add(parseInt(parts[1]));
                continue;
            }

            if (len == 2 && BRANCH_OPS.contains(parts[0]) && isNumeric(parts[1])) {
                labels.add(parseInt(parts[1]));
                continue;
            }

            if (len >= 4 && BRANCH_OPS.contains(parts[len - 2]) && isNumeric(parts[len - 1])) {
                labels.add(parseInt(parts[len - 1]));
            }
        }
        labels.add(lines.size() + 1);
    }

    private void generateLine(String line) throws IOException {
        if (line.isEmpty() || line.equals("ET") || line.equals("FIN")) {
            return;
        }

        String[] parts = line.split("\\s+");
        int len = parts.length;
        String first = parts[0];
        String last = parts[len - 1];
        String secondLast = len >= 2 ? parts[len - 2] : "";

        if (first.equals("BI") && len == 2) {
            asmBuffer.write("\tJMP ET_" + last + "\n");
            return;
        }

        if (BRANCH_OPS.contains(first) && len == 2 && !last.equals("?")) {
            asmBuffer.write("\t" + branchToJump(first) + " ET_" + last + "\n");
            return;
        }

        if (BRANCH_OPS.contains(secondLast) && isNumeric(last)) {
            String[] exprParts = Arrays.copyOfRange(parts, 0, len - 2);
            generateComparison(exprParts);
            asmBuffer.write("\t" + branchToJump(secondLast) + " ET_" + last + "\n");
            return;
        }

        if (last.equals(":=")) {
            generateAssignment(parts);
            return;
        }

        if (last.equals("write")) {
            generateWrite(parts);
            return;
        }

        if (last.equals("read") && len == 2) {
            generateRead(first);
            return;
        }

        if (last.equals("CMP")) {
            generateComparison(parts);
        }
    }

    private boolean isCompilerVariable(String name) {
        return "@i".equals(name) || "@aux1".equals(name) || "@aux2".equals(name);
    }

    private void generateAssignment(String[] parts) throws IOException {
        String identificator = parts[parts.length - 2];
        String[] exprTokens = Arrays.copyOfRange(parts, 0, parts.length - 2);
        SymbolEntry entry = findEntryByName(identificator);

        if (entry != null && entry.getType() == SymbolType.STRING && exprTokens.length == 1) {
            asmBuffer.write("\tmov SI, offset " + exprTokens[0] + "\n");
            asmBuffer.write("\tmov DI, offset " + entry.getName() + "\n");
            asmBuffer.write("\tSTRCPY\n");
            return;
        }

        if (entry != null || isCompilerVariable(identificator)) {
            if (exprTokens.length == 1) {
                if (isNumeric(exprTokens[0])) {
                    asmBuffer.write("\tMOV @aux1, " + exprTokens[0] + "\n");
                    asmBuffer.write("\tFILD @aux1\n");
                } else {
                    asmBuffer.write("\tFLD " + exprTokens[0] + "\n");
                }
            } else {
                generateCodeForArithmeticOperation(exprTokens);
            }
            asmBuffer.write("\tFSTP " + identificator + "\n");
        }
    }

    private void generateWrite(String[] parts) throws IOException {
        String[] exprTokens = Arrays.copyOfRange(parts, 0, parts.length - 1);

        if (exprTokens.length == 1) {
            SymbolEntry entry = findEntryByName(exprTokens[0]);
            if (entry != null && entry.getType() == SymbolType.STRING) {
                asmBuffer.write("\tdisplayString " + exprTokens[0] + "\n");
                asmBuffer.write("\tnewLine 1\n");
                return;
            }
        }

        generateCodeForArithmeticOperation(exprTokens);
        asmBuffer.write("\tFSTP @aux1\n");
        SymbolEntry target = exprTokens.length == 1 ? findEntryByName(exprTokens[0]) : null;
        if (target != null && target.getType() == SymbolType.FLOAT) {
            asmBuffer.write("\tDisplayFloat @aux1, 2\n");
        } else {
            asmBuffer.write("\tDisplayInteger @aux1\n");
        }
        asmBuffer.write("\tnewLine 1\n");
    }

    private void generateRead(String var) throws IOException {
        SymbolEntry entry = findEntryByName(var);
        if (entry != null && entry.getType() == SymbolType.STRING) {
            asmBuffer.write("\tgetString " + var + "\n");
            asmBuffer.write("\tnewLine 1\n");
        } else if (entry != null && entry.getType() == SymbolType.FLOAT) {
            asmBuffer.write("\tGetFloat " + var + "\n");
            asmBuffer.write("\tnewLine 1\n");
        } else {
            asmBuffer.write("\tGetInteger " + var + "\n");
            asmBuffer.write("\tnewLine 1\n");
        }
    }

    private void generateComparison(String[] parts) throws IOException {
        String[] exprParts = Arrays.copyOfRange(parts, 0, parts.length - 1);
        generateCodeForArithmeticOperation(exprParts);
        asmBuffer.write("\tFXCH\n");
        asmBuffer.write("\tFCOMP\n");
        asmBuffer.write("\tFSTSW AX\n");
        asmBuffer.write("\tFFREE ST(0)\n");
        asmBuffer.write("\tSAHF\n");
    }

    /**
     * Evalua una expresion en polaca inversa dejando el resultado en la pila FPU.
     * '/' usa division flotante (FDIV).
     * 'DIV' usa division entera (cociente).
     * 'MOD' usa resto de division entera.
     */
    private void generateCodeForArithmeticOperation(String[] tokens) throws IOException {
        for (String tok : tokens) {
            switch (tok) {
                case "+":
                    asmBuffer.write("\tFADD\n");
                    break;
                case "-":
                    asmBuffer.write("\tFSUB\n");
                    break;
                case "*":
                    asmBuffer.write("\tFMUL\n");
                    break;
                case "/":
                    asmBuffer.write("\tFDIV\n");
                    break;
                case "DIV":
                    emitIntegerDivision(false);
                    break;
                case "MOD":
                    emitIntegerDivision(true);
                    break;
                default:
                    if (isNumeric(tok)) {
                        asmBuffer.write("\tMOV @aux1, " + tok + "\n");
                        asmBuffer.write("\tFILD @aux1\n");
                    } else {
                        asmBuffer.write("\tFLD " + tok + "\n");
                    }
                    break;
            }
        }
    }

    /**
     * Convierte los dos operandos del tope de la pila FPU a enteros y aplica IDIV.
     * En polaca inversa, para "a b DIV" queda st(0)=b y st(1)=a.
     */
    private void emitIntegerDivision(boolean modulo) throws IOException {
        int id = opId++;
        String zeroLabel = "DIV_ZERO_" + id;
        String endLabel = "DIV_END_" + id;

        asmBuffer.write("\tFISTP @aux2\n");
        asmBuffer.write("\tFISTP @aux1\n");
        asmBuffer.write("\tMOV EAX, @aux1\n");
        asmBuffer.write("\tCDQ\n");
        asmBuffer.write("\tMOV ECX, @aux2\n");
        asmBuffer.write("\tCMP ECX, 0\n");
        asmBuffer.write("\tJE " + zeroLabel + "\n");
        asmBuffer.write("\tIDIV ECX\n");
        if (modulo) {
            asmBuffer.write("\tMOV EAX, EDX\n");
        }
        asmBuffer.write("\tJMP " + endLabel + "\n");
        asmBuffer.write(zeroLabel + ":\n");
        asmBuffer.write("\tMOV EAX, 0\n");
        asmBuffer.write(endLabel + ":\n");
        asmBuffer.write("\tMOV @aux1, EAX\n");
        asmBuffer.write("\tFILD @aux1\n");
    }

    private String branchToJump(String branchOp) {
        switch (branchOp) {
            case "BGE": return "JAE";
            case "BGT": return "JA";
            case "BLE": return "JBE";
            case "BLT": return "JB";
            case "BNE": return "JNE";
            case "BEQ": return "JE";
            default: return "JMP";
        }
    }

    private void generateFooter() throws IOException {
        asmBuffer.write("\n\tMOV AX, 4C00h\n");
        asmBuffer.write("\tINT 21h\n\n");
        asmBuffer.write("END START\n");
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private int parseInt(String value) {
        return Integer.parseInt(value.trim());
    }
}
