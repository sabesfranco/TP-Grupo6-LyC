package lyc.compiler.files;

import lyc.compiler.model.SymbolEntry;
import lyc.compiler.model.SymbolTable;
import lyc.compiler.model.SymbolType;
import lyc.compiler.utils.IntermediateCode;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class AsmCodeGenerator implements FileGenerator {

    private Map<String, String> asmLabels = new HashMap<>();
    private int labelCounter = 0;
    private int auxSlot = 0;

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        List<String> lines = IntermediateCode.getLines();
        Collection<SymbolEntry> symbols = SymbolTable.getInstance().getAll();

        StringBuilder asm = new StringBuilder();

        asm.append(".MODEL SMALL\n");
        asm.append(".386\n");
        asm.append(".STACK 200h\n\n");

        asm.append(".DATA\n");
        generateDataSegment(asm, symbols);
        asm.append("\n");

        asm.append(".CODE\n");
        asm.append("START:\n");
        asm.append("    MOV AX, @DATA\n");
        asm.append("    MOV DS, AX\n\n");

        generateCode(asm, lines);

        asm.append("\nEND_PROGRAM:\n");
        asm.append("    MOV AX, 4C00h\n");
        asm.append("    INT 21h\n\n");

        generateAuxiliaryFunctions(asm);

        asm.append("END START\n");

        fileWriter.write(asm.toString());
    }

    private void generateDataSegment(StringBuilder asm, Collection<SymbolEntry> symbols) {
        asm.append("    @aux DW ?\n");
        asm.append("    @aux2 DW ?\n");
        asm.append("    @result DW ?\n");
        asm.append("    @i DW ?\n");
        asm.append("    @newline DB 13, 10, '$'\n");
        asm.append("    @buffer DB 50, 50 DUP ('$'), '$'\n");

        for (SymbolEntry entry : symbols) {
            String varName = sanitizeName(entry.getName());

            if (entry.isConstant()) {
                if (entry.getConstantTypeLabel().contains("STRING")) {
                    String value = entry.getValue();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    asm.append("    ").append(varName).append(" DB \"")
                       .append(escapeString(value)).append("\", '$'\n");
                } else if (entry.getConstantTypeLabel().contains("INT")) {
                    int intValue = Integer.parseInt(entry.getValue());
                    intValue = clampWord(intValue);
                    asm.append("    ").append(varName).append(" DW ")
                       .append(intValue).append("\n");
                } else if (entry.getConstantTypeLabel().contains("FLOAT")) {
                    String value = entry.getValue();
                    try {
                        double d = Double.parseDouble(value);
                        int intValue = clampWord((int) d);
                        asm.append("    ").append(varName).append(" DW ")
                           .append(intValue).append("\n");
                    } catch (NumberFormatException e) {
                        asm.append("    ").append(varName).append(" DW 0\n");
                    }
                }
            } else {
                if (entry.getTypeLabel().equals("String")) {
                    asm.append("    ").append(varName).append(" DB 100 DUP ('$'), '$'\n");
                } else {
                    asm.append("    ").append(varName).append(" DW ?\n");
                }
            }
        }
    }

    private int clampWord(int value) {
        if (value > 32767) return 32767;
        if (value < -32768) return -32768;
        return value;
    }

    private void generateCode(StringBuilder asm, List<String> lines) {
        // First pass: collect all 1-based line numbers that are branch targets
        Set<Integer> labelTargets = new HashSet<>();
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith("BI ")) {
                try { labelTargets.add(Integer.parseInt(t.substring(3).trim())); }
                catch (NumberFormatException ignored) {}
            } else if (isPureBranchLine(t)) {
                try { labelTargets.add(Integer.parseInt(t.split("\\s+")[1])); }
                catch (NumberFormatException ignored) {}
            } else if (isInlineBranchLine(t)) {
                String[] tokens = t.split("\\s+");
                for (int k = 0; k < tokens.length - 1; k++) {
                    if (isBranchInstruction(tokens[k])) {
                        try { labelTargets.add(Integer.parseInt(tokens[k + 1])); }
                        catch (NumberFormatException ignored) {}
                        break;
                    }
                }
            }
        }

        // Second pass: generate ASM
        for (int i = 0; i < lines.size(); i++) {
            int lineNum = i + 1; // 1-based
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            // Emit label before this line if it's a branch target
            if (labelTargets.contains(lineNum)) {
                asm.append(getOrCreateLabel(lineNum)).append(":\n");
            }

            if (line.equals("ET") || line.equals("FIN")) {
                // ET/FIN need their own label for BI targets (e.g., BI 47 -> L47)
                // Only emit if not already emitted above
                if (!labelTargets.contains(lineNum)) {
                    asm.append(getOrCreateLabel(lineNum)).append(":\n");
                }
            } else if (line.endsWith(":=")) {
                processAssignment(asm, line);
            } else if (line.endsWith(" write")) {
                processWrite(asm, line);
            } else if (line.endsWith(" read")) {
                processRead(asm, line);
            } else if (isInlineBranchLine(line)) {
                processBranchInstruction(asm, line);
            } else if (isPureBranchLine(line)) {
                // Condition is on the previous non-empty line
                String prevLine = "";
                for (int k = i - 1; k >= 0; k--) {
                    String p = lines.get(k).trim();
                    if (!p.isEmpty()) { prevLine = p; break; }
                }
                processBranchInstruction(asm, prevLine + " " + line);
            } else if (line.startsWith("BI ")) {
                String target = line.split("\\s+")[1];
                asm.append("    JMP ").append(getOrCreateLabel(Integer.parseInt(target))).append("\n");
            }
            // Standalone condition lines (e.g., "a b >") are picked up by the next branch line
        }
    }

    private boolean isPureBranchLine(String line) {
        return line.matches("^(BEQ|BNE|BLT|BLE|BGT|BGE)\\s+\\d+$");
    }

    private boolean isInlineBranchLine(String line) {
        // Branch instruction appears in the middle (with content before it)
        return line.contains(" BEQ ") || line.contains(" BNE ") || line.contains(" BLT ") ||
               line.contains(" BLE ") || line.contains(" BGT ") || line.contains(" BGE ");
    }

    private void processAssignment(StringBuilder asm, String line) {
        // Handle both "expr var :=" and "expr var:=" (WHILE IN uses no space)
        String withoutAssign;
        if (line.endsWith(" :=")) {
            withoutAssign = line.substring(0, line.length() - 3).trim();
        } else {
            withoutAssign = line.substring(0, line.length() - 2).trim();
        }

        int lastSpace = withoutAssign.lastIndexOf(' ');
        String var, expr;
        if (lastSpace == -1) {
            var = withoutAssign;
            expr = "";
        } else {
            var = withoutAssign.substring(lastSpace + 1);
            expr = withoutAssign.substring(0, lastSpace).trim();
        }

        if (expr.isEmpty()) return;

        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            asm.append("    ; Asignacion de string literal a ").append(var).append(" (omitida)\n");
            return;
        }

        String result = evaluatePostfixExpression(asm, expr);
        asm.append("    MOV AX, ").append(result).append("\n");
        asm.append("    MOV ").append(sanitizeName(var)).append(", AX\n");
    }

    private void processWrite(StringBuilder asm, String line) {
        String expr = line.substring(0, line.lastIndexOf("write")).trim();

        if (expr.startsWith("\"")) {
            String varName = sanitizeName(expr);
            asm.append("    LEA DX, ").append(varName).append("\n");
            asm.append("    MOV AH, 09h\n");
            asm.append("    INT 21h\n");
            asm.append("    LEA DX, @newline\n");
            asm.append("    MOV AH, 09h\n");
            asm.append("    INT 21h\n");
        } else if (expr.contains(" ")) {
            String result = evaluatePostfixExpression(asm, expr);
            asm.append("    MOV AX, ").append(result).append("\n");
            asm.append("    CALL PRINT_NUMBER\n");
        } else {
            String varName = resolveOperand(expr);
            asm.append("    MOV AX, ").append(varName).append("\n");
            asm.append("    CALL PRINT_NUMBER\n");
        }
    }

    private void processRead(StringBuilder asm, String line) {
        String[] tokens = line.split("\\s+");
        String var = tokens[tokens.length - 2];
        String varName = sanitizeName(var);

        asm.append("    LEA DX, @buffer\n");
        asm.append("    MOV AH, 0Ah\n");
        asm.append("    INT 21h\n");
        asm.append("    CALL STRING_TO_NUMBER\n");
        asm.append("    MOV ").append(varName).append(", AX\n");
    }

    private void processBranchInstruction(StringBuilder asm, String line) {
        String[] tokens = line.split("\\s+");
        String branchInst = null;
        int branchIdx = -1;

        for (int i = tokens.length - 2; i >= 0; i--) {
            if (isBranchInstruction(tokens[i])) {
                branchInst = tokens[i];
                branchIdx = i;
                break;
            }
        }

        if (branchInst == null) return;

        String target = tokens[branchIdx + 1];
        String label = getOrCreateLabel(Integer.parseInt(target));
        String[] condTokens = Arrays.copyOfRange(tokens, 0, branchIdx);

        // Special case: "op1 op2 CMP" - direct equality comparison (WHILE IN construct)
        if (condTokens.length == 3 && condTokens[2].equals("CMP")) {
            loadValue(asm, "AX", condTokens[0]);
            asm.append("    MOV BX, AX\n");
            loadValue(asm, "AX", condTokens[1]);
            asm.append("    CMP BX, AX\n");
            asm.append("    ").append(convertBranchInstruction(branchInst)).append(" ").append(label).append("\n");
            return;
        }

        String condition = String.join(" ", condTokens);
        evaluateCondition(asm, condition, branchInst, target);
    }

    private void evaluateCondition(StringBuilder asm, String condition, String branchInst, String target) {
        String[] tokens = condition.split("\\s+");
        Stack<String> stack = new Stack<>();
        auxSlot = 0;

        for (String token : tokens) {
            if (isArithmeticOperator(token)) {
                String op2 = stack.pop();
                String op1 = stack.pop();

                loadValue(asm, "AX", op1);
                asm.append("    MOV BX, AX\n");
                loadValue(asm, "AX", op2);

                if (token.equals("+")) {
                    asm.append("    ADD BX, AX\n");
                    asm.append("    MOV AX, BX\n");
                } else if (token.equals("-")) {
                    asm.append("    SUB BX, AX\n");
                    asm.append("    MOV AX, BX\n");
                } else if (token.equals("*")) {
                    asm.append("    IMUL BX\n");
                } else if (token.equals("/")) {
                    asm.append("    MOV CX, AX\n");
                    asm.append("    MOV AX, BX\n");
                    asm.append("    CWD\n");
                    asm.append("    IDIV CX\n");
                } else if (token.equals("MOD")) {
                    asm.append("    MOV CX, AX\n");
                    asm.append("    MOV AX, BX\n");
                    asm.append("    CWD\n");
                    asm.append("    IDIV CX\n");
                    asm.append("    MOV AX, DX\n");
                } else if (token.equals("DIV")) {
                    asm.append("    MOV CX, AX\n");
                    asm.append("    MOV AX, BX\n");
                    asm.append("    CWD\n");
                    asm.append("    IDIV CX\n");
                }

                asm.append("    MOV @aux, AX\n");
                stack.push("@aux");

            } else if (isLogicalOperator(token)) {
                if (token.equals("AND")) {
                    String op2 = stack.pop();
                    String op1 = stack.pop();
                    asm.append("    MOV AX, ").append(op1).append("\n");
                    asm.append("    CMP AX, 0\n");
                    asm.append("    JE AND_FALSE_").append(labelCounter).append("\n");
                    asm.append("    MOV AX, ").append(op2).append("\n");
                    asm.append("    CMP AX, 0\n");
                    asm.append("    JE AND_FALSE_").append(labelCounter).append("\n");
                    asm.append("    MOV @aux, 1\n");
                    asm.append("    JMP AND_END_").append(labelCounter).append("\n");
                    asm.append("AND_FALSE_").append(labelCounter).append(":\n");
                    asm.append("    MOV @aux, 0\n");
                    asm.append("AND_END_").append(labelCounter).append(":\n");
                    stack.push("@aux");
                    labelCounter++;

                } else if (token.equals("OR")) {
                    String op2 = stack.pop();
                    String op1 = stack.pop();
                    asm.append("    MOV AX, ").append(op1).append("\n");
                    asm.append("    CMP AX, 0\n");
                    asm.append("    JNE OR_TRUE_").append(labelCounter).append("\n");
                    asm.append("    MOV AX, ").append(op2).append("\n");
                    asm.append("    CMP AX, 0\n");
                    asm.append("    JNE OR_TRUE_").append(labelCounter).append("\n");
                    asm.append("    MOV @aux, 0\n");
                    asm.append("    JMP OR_END_").append(labelCounter).append("\n");
                    asm.append("OR_TRUE_").append(labelCounter).append(":\n");
                    asm.append("    MOV @aux, 1\n");
                    asm.append("OR_END_").append(labelCounter).append(":\n");
                    stack.push("@aux");
                    labelCounter++;

                } else if (token.equals("NOT")) {
                    String op = stack.pop();
                    asm.append("    MOV AX, ").append(op).append("\n");
                    asm.append("    CMP AX, 0\n");
                    asm.append("    JE NOT_TRUE_").append(labelCounter).append("\n");
                    asm.append("    MOV @aux, 0\n");
                    asm.append("    JMP NOT_END_").append(labelCounter).append("\n");
                    asm.append("NOT_TRUE_").append(labelCounter).append(":\n");
                    asm.append("    MOV @aux, 1\n");
                    asm.append("NOT_END_").append(labelCounter).append(":\n");
                    stack.push("@aux");
                    labelCounter++;
                }

            } else if (isComparisonOperator(token)) {
                String op2 = stack.pop();
                String op1 = stack.pop();

                // Alternate between @aux and @aux2 to avoid overwriting during AND/OR
                String auxVar = (auxSlot++ % 2 == 0) ? "@aux" : "@aux2";

                loadValue(asm, "AX", op1);
                asm.append("    MOV BX, AX\n");
                loadValue(asm, "AX", op2);
                asm.append("    CMP BX, AX\n");

                String setInst = getSetInstruction(token);
                asm.append("    ").append(setInst).append(" CMP_TRUE_").append(labelCounter).append("\n");
                asm.append("    MOV ").append(auxVar).append(", 0\n");
                asm.append("    JMP CMP_END_").append(labelCounter).append("\n");
                asm.append("CMP_TRUE_").append(labelCounter).append(":\n");
                asm.append("    MOV ").append(auxVar).append(", 1\n");
                asm.append("CMP_END_").append(labelCounter).append(":\n");
                stack.push(auxVar);
                labelCounter++;

            } else {
                stack.push(token);
            }
        }

        if (!stack.isEmpty()) {
            String finalResult = stack.pop();
            asm.append("    MOV AX, ").append(finalResult).append("\n");
            asm.append("    CMP AX, 0\n");
            String asmBranch = convertBranchInstruction(branchInst);
            String label = getOrCreateLabel(Integer.parseInt(target));
            asm.append("    ").append(asmBranch).append(" ").append(label).append("\n");
        }
    }

    private String evaluatePostfixExpression(StringBuilder asm, String expr) {
        String[] tokens = expr.split("\\s+");
        Stack<String> stack = new Stack<>();

        for (String token : tokens) {
            if (isArithmeticOperator(token)) {
                String op2 = stack.pop();
                String op1 = stack.pop();

                loadValue(asm, "AX", op1);
                asm.append("    MOV BX, AX\n");
                loadValue(asm, "AX", op2);

                if (token.equals("+")) {
                    asm.append("    ADD BX, AX\n");
                    asm.append("    MOV AX, BX\n");
                } else if (token.equals("-")) {
                    asm.append("    SUB BX, AX\n");
                    asm.append("    MOV AX, BX\n");
                } else if (token.equals("*")) {
                    asm.append("    IMUL BX\n");
                } else if (token.equals("/")) {
                    asm.append("    MOV CX, AX\n");
                    asm.append("    CMP CX, 0\n");
                    asm.append("    JE DIV_SKIP_").append(labelCounter).append("\n");
                    asm.append("    MOV AX, BX\n");
                    asm.append("    CWD\n");
                    asm.append("    IDIV CX\n");
                    asm.append("    JMP DIV_END_").append(labelCounter).append("\n");
                    asm.append("DIV_SKIP_").append(labelCounter).append(":\n");
                    asm.append("    MOV AX, 0\n");
                    asm.append("DIV_END_").append(labelCounter).append(":\n");
                    labelCounter++;
                } else if (token.equals("MOD")) {
                    asm.append("    MOV CX, AX\n");
                    asm.append("    CMP CX, 0\n");
                    asm.append("    JE DIV_SKIP_").append(labelCounter).append("\n");
                    asm.append("    MOV AX, BX\n");
                    asm.append("    CWD\n");
                    asm.append("    IDIV CX\n");
                    asm.append("    MOV AX, DX\n");
                    asm.append("    JMP DIV_END_").append(labelCounter).append("\n");
                    asm.append("DIV_SKIP_").append(labelCounter).append(":\n");
                    asm.append("    MOV AX, 0\n");
                    asm.append("DIV_END_").append(labelCounter).append(":\n");
                    labelCounter++;
                } else if (token.equals("DIV")) {
                    asm.append("    MOV CX, AX\n");
                    asm.append("    CMP CX, 0\n");
                    asm.append("    JE DIV_SKIP_").append(labelCounter).append("\n");
                    asm.append("    MOV AX, BX\n");
                    asm.append("    CWD\n");
                    asm.append("    IDIV CX\n");
                    asm.append("    JMP DIV_END_").append(labelCounter).append("\n");
                    asm.append("DIV_SKIP_").append(labelCounter).append(":\n");
                    asm.append("    MOV AX, 0\n");
                    asm.append("DIV_END_").append(labelCounter).append(":\n");
                    labelCounter++;
                }

                asm.append("    MOV @aux, AX\n");
                stack.push("@aux");

            } else {
                stack.push(resolveOperand(token));
            }
        }

        return stack.isEmpty() ? "0" : stack.pop();
    }

    private String resolveOperand(String operand) {
        if (operand.startsWith("\"") || isNumericLiteral(operand)) {
            return sanitizeName(operand);
        }
        return operand;
    }

    private void loadValue(StringBuilder asm, String register, String value) {
        value = value.trim();

        if (value.equals("@aux") || value.equals("@aux2") || value.equals("@result") || value.equals("@i")) {
            asm.append("    MOV ").append(register).append(", ").append(value).append("\n");
        } else if (value.startsWith("\"") && value.endsWith("\"")) {
            String varName = sanitizeName(value);
            asm.append("    LEA ").append(register).append(", ").append(varName).append("\n");
        } else if (isNumericLiteral(value)) {
            String varName = sanitizeName(value);
            asm.append("    MOV ").append(register).append(", ").append(varName).append("\n");
        } else {
            String varName = sanitizeName(value);
            asm.append("    MOV ").append(register).append(", ").append(varName).append("\n");
        }
    }

    private boolean isNumericLiteral(String value) {
        return value.matches("-?\\d+(\\.\\d*)?") || value.matches("-?\\.\\d+");
    }

    private boolean isArithmeticOperator(String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") ||
               token.equals("/") || token.equals("MOD") || token.equals("DIV");
    }

    private boolean isLogicalOperator(String token) {
        return token.equals("AND") || token.equals("OR") || token.equals("NOT");
    }

    private boolean isComparisonOperator(String token) {
        return token.equals("==") || token.equals("<") || token.equals(">") ||
               token.equals("<=") || token.equals(">=") || token.equals("!=");
    }

    private boolean isBranchInstruction(String token) {
        return token.equals("BEQ") || token.equals("BNE") || token.equals("BLT") ||
               token.equals("BLE") || token.equals("BGT") || token.equals("BGE");
    }

    private String getSetInstruction(String compOp) {
        switch (compOp) {
            case "==": return "JE";
            case "!=": return "JNE";
            case "<":  return "JL";
            case "<=": return "JLE";
            case ">":  return "JG";
            case ">=": return "JGE";
            default:   return "JE";
        }
    }

    private String convertBranchInstruction(String branchInst) {
        switch (branchInst) {
            case "BEQ": return "JE";
            case "BNE": return "JNE";
            case "BLT": return "JL";
            case "BLE": return "JLE";
            case "BGT": return "JG";
            case "BGE": return "JGE";
            default:    return "JMP";
        }
    }

    private String getOrCreateLabel(int lineNumber) {
        String key = "L" + lineNumber;
        if (!asmLabels.containsKey(key)) {
            asmLabels.put(key, key);
        }
        return key;
    }

    private String sanitizeName(String name) {
        if (name == null || name.isEmpty()) return "_empty";

        String sanitized = name.replaceAll("[^a-zA-Z0-9_@]", "_");

        if (sanitized.matches("^\\d.*")) {
            sanitized = "_" + sanitized;
        }

        if (sanitized.length() > 30) {
            sanitized = sanitized.substring(0, 30);
        }

        return sanitized;
    }

    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "")
                  .replace("\r", "");
    }

    private void generateAuxiliaryFunctions(StringBuilder asm) {
        asm.append("; Funcion para imprimir numeros\n");
        asm.append("PRINT_NUMBER PROC\n");
        asm.append("    PUSH AX\n");
        asm.append("    PUSH BX\n");
        asm.append("    PUSH CX\n");
        asm.append("    PUSH DX\n");
        asm.append("    \n");
        asm.append("    MOV BX, 10\n");
        asm.append("    XOR CX, CX\n");
        asm.append("    CMP AX, 0\n");
        asm.append("    JGE PN_POSITIVE\n");
        asm.append("    PUSH AX\n");
        asm.append("    MOV DL, '-'\n");
        asm.append("    MOV AH, 02h\n");
        asm.append("    INT 21h\n");
        asm.append("    POP AX\n");
        asm.append("    NEG AX\n");
        asm.append("PN_POSITIVE:\n");
        asm.append("    XOR DX, DX\n");
        asm.append("    DIV BX\n");
        asm.append("    PUSH DX\n");
        asm.append("    INC CX\n");
        asm.append("    CMP AX, 0\n");
        asm.append("    JNE PN_POSITIVE\n");
        asm.append("PN_PRINT:\n");
        asm.append("    POP DX\n");
        asm.append("    ADD DL, '0'\n");
        asm.append("    MOV AH, 02h\n");
        asm.append("    INT 21h\n");
        asm.append("    LOOP PN_PRINT\n");
        asm.append("    \n");
        asm.append("    LEA DX, @newline\n");
        asm.append("    MOV AH, 09h\n");
        asm.append("    INT 21h\n");
        asm.append("    \n");
        asm.append("    POP DX\n");
        asm.append("    POP CX\n");
        asm.append("    POP BX\n");
        asm.append("    POP AX\n");
        asm.append("    RET\n");
        asm.append("PRINT_NUMBER ENDP\n\n");

        asm.append("; Funcion para convertir string a numero\n");
        asm.append("STRING_TO_NUMBER PROC\n");
        asm.append("    PUSH BX\n");
        asm.append("    PUSH CX\n");
        asm.append("    PUSH DX\n");
        asm.append("    \n");
        asm.append("    XOR AX, AX\n");
        asm.append("    XOR BX, BX\n");
        asm.append("    LEA SI, @buffer + 2\n");
        asm.append("STN_LOOP:\n");
        asm.append("    MOV BL, [SI]\n");
        asm.append("    CMP BL, 13\n");
        asm.append("    JE STN_END\n");
        asm.append("    CMP BL, '0'\n");
        asm.append("    JL STN_END\n");
        asm.append("    CMP BL, '9'\n");
        asm.append("    JG STN_END\n");
        asm.append("    SUB BL, '0'\n");
        asm.append("    MOV CX, 10\n");
        asm.append("    MUL CX\n");
        asm.append("    ADD AX, BX\n");
        asm.append("    INC SI\n");
        asm.append("    JMP STN_LOOP\n");
        asm.append("STN_END:\n");
        asm.append("    POP DX\n");
        asm.append("    POP CX\n");
        asm.append("    POP BX\n");
        asm.append("    RET\n");
        asm.append("STRING_TO_NUMBER ENDP\n\n");
    }
}
