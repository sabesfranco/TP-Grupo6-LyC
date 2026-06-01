package lyc.compiler.files;

import lyc.compiler.model.SymbolEntry;
import lyc.compiler.model.SymbolTable;

import java.io.FileWriter;
import java.io.IOException;

public class SymbolTableGenerator implements FileGenerator {

    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        fileWriter.write("\n" + "=".repeat(110) + "\n");
        fileWriter.write(centerText("SYMBOLS TABLE", 110) + "\n");
        fileWriter.write("=".repeat(110) + "\n");

        String format = "| %-35s | %-13s | %-35s | %-8s |\n";
        fileWriter.write(String.format(format, "NAME", "TYPE", "VALUE", "LENGTH"));
        fileWriter.write("-".repeat(110) + "\n");

        for (SymbolEntry entry : SymbolTable.getInstance().getAll()) {
            String name = entry.getName();
            String type = entry.isConstant() ? entry.getConstantTypeLabel() : entry.getTypeLabel();
            String value = entry.getValue() != null ? entry.getValue() : "-";
            String length = entry.getLength() >= 0 ? String.valueOf(entry.getLength()) : "-";
            fileWriter.write(String.format(format, name, type, value, length));
        }

        fileWriter.write("=".repeat(110) + "\n");
        fileWriter.write("Total symbols: " + SymbolTable.getInstance().getAll().size() + "\n");
        fileWriter.write("=".repeat(110) + "\n\n");
    }
}
