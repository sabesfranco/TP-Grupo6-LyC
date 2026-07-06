package lyc.compiler.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import lyc.compiler.model.SymbolEntry;
import lyc.compiler.model.SymbolType;
import lyc.compiler.model.SymbolTable;

public final class SemanticChecker {

    private static Set<String> arithmeticOperators = Set.of("+", "-", "*", "/", "MOD", "DIV");
    private static final Map<String, String> variables = new HashMap<>();
    private static boolean hasErrors = false;
    private static boolean activo = true;

    private SemanticChecker() {}

    public static void setActivo(boolean valor) {
        activo = valor;
    }

    public static void declare(String name) {
        if (!activo) return;
        SymbolEntry entry = findEntryByName(name);
        if (entry != null) {
            error("Variable '" + name + "' already declared");
            return;
        }
    }

    public static SymbolEntry checkExists(String name) {
        if (!activo) return null;
        SymbolEntry entry = findEntryByName(name);
        if (entry == null) {
            error("Variable '" + name + "' not declared");
        }
        return entry;
    }

    public static void checkAssignmentTypeCompatibility(SymbolEntry entry, SymbolType exprType) {
        if (!activo) return;
        SymbolType targetType = entry.getType();
        if (targetType == exprType || (SymbolType.FLOAT == targetType && SymbolType.INT == exprType)) {
            return;
        }
        error("Type incompatibility: cannot assign type " + exprType + " to variable " + entry.getName() + " of type " + targetType);
    }

    public static void checkConditionTypeCompatibility(SymbolType exprType1, SymbolType exprType2) {
        if (!activo) return;
        if (exprType1 == exprType2) return;
        error("Type incompatibility in condition: cannot compare " + exprType1 + " with " + exprType2);
    }

    public static void checkDivisionByZero(String operandPostfix) {
        if (!activo) return;
        SymbolEntry entry = findEntryByName(operandPostfix);
        if (entry.getType() != SymbolType.STRING && entry.getValue() != null) {
            if (Double.parseDouble(entry.getValue()) == 0.0) {
                error("Division by zero");
            }
        }
    }

    public static SymbolType inferPostfixType(String expr) {
        if (!activo) return null;
        List<String> cells = Arrays.asList(expr.split(" "));
        SymbolEntry firstEntry = findEntryByName(cells.get(0));
        SymbolType type = firstEntry.getType();
        for (int i = 1; i < cells.size(); i++) {
            String cell = cells.get(i);
            if (!isArithmeticOperator(cell)) {
                SymbolEntry entry = findEntryByName(cells.get(i));
                if (entry.getType() != null) {
                    if (entry.getType() == SymbolType.STRING) {
                        error("Type incompatibility: cannot apply operation between " + firstEntry.getNameLabel() + " and " + entry.getNameLabel());
                        return null;
                    }
                    if (entry.getType() == SymbolType.FLOAT) {
                        type = SymbolType.FLOAT;
                    }
                }
            }
        }
        return type;
    }

    private static boolean isArithmeticOperator(String text) {
        return arithmeticOperators.contains(text);
    }

    private static SymbolEntry findEntryByName(String name) {
        Collection<SymbolEntry> entries = SymbolTable.getInstance().getAll();
        for (SymbolEntry e : entries) {
            if (e.getName().equals(name)) {
                return e;
            }
        }
        return null;
    }

    private static SymbolEntry findEntryByValue(String value) {
        Collection<SymbolEntry> entries = SymbolTable.getInstance().getAll();
        for (SymbolEntry e : entries) {
            if (e.getValue().equals(value)) {
                return e;
            }
        }
        return null;
    }

    private static void error(String message) {
        if (!activo) return;
        System.err.println("[SEMANTIC ERROR] " + message);
        hasErrors = true;
    }

    public static boolean hasErrors() {
        return hasErrors;
    }

    public static void clear() {
        hasErrors = false;
    }
}
