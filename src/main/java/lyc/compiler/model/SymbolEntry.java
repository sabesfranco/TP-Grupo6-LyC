package lyc.compiler.model;

public class SymbolEntry {

    private final String name;
    private final SymbolType type;
    private String value;
    private Integer length;

    public SymbolEntry(String name, SymbolType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getLength() {
        if (length == null) {
            return -1;
        }
        return length;
    }

    public void setValue(String value) {
        this.value = value;
        if (type == SymbolType.STRING && value != null) {
            if (value.startsWith("\"") && value.endsWith("\"")) {
                length = value.length() - 2;
            } else {
                length = value.length();
            }
        }
    }

    public String getTypeLabel() {
        if (type == SymbolType.INT) return "Int";
        if (type == SymbolType.FLOAT) return "Float";
        if (type == SymbolType.STRING) return "String";
        return "-";
    }

    public String getConstantTypeLabel() {
        if (type == SymbolType.INT) return "CTE_INTEGER";
        if (type == SymbolType.FLOAT) return "CTE_FLOAT";
        if (type == SymbolType.STRING) return "CTE_STRING";
        return getTypeLabel();
    }

    public boolean isConstant() {
        return value != null;
    }
}
