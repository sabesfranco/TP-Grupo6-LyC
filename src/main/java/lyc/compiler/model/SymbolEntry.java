package lyc.compiler.model;

public class SymbolEntry {

    private final String name;
    private final SymbolType type;
    private String value;
    private int length;

    public SymbolEntry(String name, SymbolType type) {
        this.name = name;
        this.type = type;
        this.length = name.length();
    }

    public String getName() { return name; }
    public SymbolType getType() { return type; }
    public String getValue() { return value; }
    public int getLength() { return length; }

    public void setValue(String value) {
        this.value = value;
        if (value != null) this.length = value.length();
    }

    @Override
    public String toString() {
        return String.format("%-20s %-10s %-20s %d",
                name, type, value != null ? value : "-", length);
    }
}