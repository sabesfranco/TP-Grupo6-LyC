package lyc.compiler.files;

import lyc.compiler.model.SymbolEntry;
import lyc.compiler.model.SymbolTable;

import java.io.FileWriter;
import java.io.IOException;

public class SymbolTableGenerator implements FileGenerator {

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        fileWriter.write(String.format("%-20s %-10s %-20s %s%n",
                "NAME", "TYPE", "VALUE", "LENGTH"));
        fileWriter.write("-".repeat(65) + System.lineSeparator());
        for (SymbolEntry entry : SymbolTable.getInstance().getAll()) {
            fileWriter.write(entry.toString() + System.lineSeparator());
        }
    }
}