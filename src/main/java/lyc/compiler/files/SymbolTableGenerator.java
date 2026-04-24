package lyc.compiler.files;

import lyc.compiler.model.SymbolEntry;
import lyc.compiler.model.SymbolTable;

import java.io.FileWriter;
import java.io.IOException;

public class SymbolTableGenerator implements FileGenerator {

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        String headerFmt = "%-50s | %-20s | %-50s | %-10s%n";
        fileWriter.write(String.format(headerFmt, "NAME", "TYPE", "VALUE", "LENGTH"));
        fileWriter.write("-".repeat(140) + System.lineSeparator());
        for (SymbolEntry entry : SymbolTable.getInstance().getAll()) {
            fileWriter.write(entry.toString() + System.lineSeparator());
        }
    }
}