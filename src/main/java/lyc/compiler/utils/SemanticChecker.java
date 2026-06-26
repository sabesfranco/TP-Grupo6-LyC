package lyc.compiler.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SemanticChecker {

    private static final Map<String, String> variables = new HashMap<>();
    private static final Set<String> declaredInInit = new HashSet<>();
    private static boolean hasErrors = false;
    private static boolean activo = true;

    private SemanticChecker() {}

    public static void setActivo(boolean valor) {
        activo = valor;
    }

    public static void declare(String name, String type) {
        if (!activo) return;
        if (declaredInInit.contains(name)) {
            error("Variable '" + name + "' already declared");
            return;
        }
        declaredInInit.add(name);
        variables.put(name, type);
    }

    public static void checkExists(String name) {
        if (!activo) return;
        if (!variables.containsKey(name)) {
            error("Variable '" + name + "' not declared");
        }
    }

    public static void checkAssignmentTypeCompatibility(String varName, String exprType) {
        if (!activo) return;
        if (!variables.containsKey(varName)) {
            return;
        }
        String targetType = variables.get(varName);
        if (targetType == null || exprType == null || "-".equals(exprType)) {
            return;
        }
        if (targetType.equals(exprType) || "Float".equals(targetType) && "Int".equals(exprType)) {
            return;
        }
        error("Type incompatibility: cannot assign type " + exprType + " to variable of type " + targetType);
    }

    public static void checkConditionTypeCompatibility(String exprType1, String exprType2) {
        if (!activo) return;
        if (exprType1.equals(exprType2)) return;
        error("Type incompatibility in condition: cannot compare " + exprType1 + " with " + exprType2);
    }

    public static void checkDivisionByZero(String operandPostfix) {
        if (!activo) return;
        if (operandPostfix == null) {
            return;
        }
        String trimmed = operandPostfix.trim();
        if (trimmed.matches("-?\\d+(\\.\\d+)?")) {
            try {
                if (Double.parseDouble(trimmed) == 0.0) {
                    error("Division by zero");
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public static String inferPostfixType(String postfix) {
        if (postfix == null || postfix.isBlank()) {
            return "-";
        }
        String[] tokens = postfix.trim().split("\\s+");
        if (tokens.length == 1) {
            return inferAtomType(tokens[0]);
        }
        return inferExpressionTypeFromPostfix(tokens);
    }

    private static String inferExpressionTypeFromPostfix(String[] tokens) {
        boolean hasFloat = false;
        for (String token : tokens) {
            if (isOperator(token)) {
                continue;
            }
            String type = inferAtomType(token);
            if ("Float".equals(type)) {
                hasFloat = true;
            }
            if ("String".equals(type)) {
                return "String";
            }
        }
        return hasFloat ? "Float" : "Int";
    }

    private static boolean isOperator(String token) {
        return Set.of("+", "-", "*", "/", "MOD", "DIV", "<", "<=", ">", ">=", "==", "!=", "AND", "OR", "NOT")
                .contains(token);
    }

    private static String inferAtomType(String token) {
        if (variables.containsKey(token)) {
            return variables.get(token);
        }
        if (token.startsWith("\"")) {
            return "String";
        }
        if (token.contains(".")) {
            return "Float";
        }
        try {
            Integer.parseInt(token);
            return "Int";
        } catch (NumberFormatException e) {
            return "-";
        }
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
        variables.clear();
        declaredInInit.clear();
        hasErrors = false;
    }
}
