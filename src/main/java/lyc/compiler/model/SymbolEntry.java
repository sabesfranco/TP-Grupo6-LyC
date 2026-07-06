package lyc.compiler.model;

public class SymbolEntry {

    private final String name;
    private final SymbolType type;
    private String value;
    private Integer length;

    private SymbolEntry(String name, SymbolType type, String value, Integer length) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.length = length;
    }

    public static SymbolEntry newStringConstantEntry(String value) {
        String name = value.substring(1, value.length() - 1).replaceAll("[^A-Za-z0-9_]", "_");
        return new SymbolEntry(
            "_" + name, 
            SymbolType.STRING, 
            value, 
            value.length() - 2
        );
    }
    
    public static SymbolEntry newIntConstantEntry(String value) {
        return new SymbolEntry(
            "_" + value, 
            SymbolType.INT, 
            value, 
            null
        );
    }

    public static SymbolEntry newFloatConstantEntry(String value) {
        String normalizedValue = null;
        if (value.startsWith(".")) {
            normalizedValue = "0" + value;
        } else if (value.endsWith(".")) {
            normalizedValue = value + "0";
        } else {
            normalizedValue = value;
        }
        return new SymbolEntry(
            "_" + normalizedValue.replace('.', '_'), 
            SymbolType.FLOAT, 
            normalizedValue, 
            null
        );
    }

    public static SymbolEntry newEntry(String name, SymbolType type) {
        return new SymbolEntry(
            name, 
            type, 
            null, 
            null
        );
    }

    public String getName() {
        return name;
    }

    public SymbolType getType() {
        return type;
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

    public String getTypeLabel() {
        if (type == SymbolType.INT) return isConstant()? "Int_cte" : "Int";
        if (type == SymbolType.FLOAT) return isConstant()? "Float_cte" : "Float";
        if (type == SymbolType.STRING) return isConstant()? "String_cte" : "String";
        return "-";
    }

    public boolean isConstant() {
        return name.charAt(0) == '_';
    }
}
