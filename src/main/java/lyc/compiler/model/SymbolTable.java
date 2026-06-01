package lyc.compiler.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTable {

    private static final SymbolTable INSTANCE = new SymbolTable();
    private final Map<String, SymbolEntry> entries = new LinkedHashMap<>();

    private SymbolTable() {}

    public static SymbolTable getInstance() {
        return INSTANCE;
    }

    public void add(SymbolEntry entry) {
        entries.put(entry.getName(), entry);
    }

    public boolean contains(String name) {
        return entries.containsKey(name);
    }

    public Collection<SymbolEntry> getAll() {
        return entries.values();
    }

    public void clear() {
        entries.clear();
    }
}