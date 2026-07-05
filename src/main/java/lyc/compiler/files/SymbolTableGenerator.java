package lyc.compiler.files;

import lyc.compiler.model.SymbolEntry;
import lyc.compiler.model.SymbolTable;

import java.io.FileWriter;
import java.io.IOException;

public class SymbolTableGenerator implements FileGenerator {

    private static final int FORMAT_LENGTH = 50 + 10 + 52 + 6 + 13;

    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        fileWriter.write("\n" + "=".repeat(FORMAT_LENGTH) + "\n");
        fileWriter.write(centerText("SYMBOLS TABLE", FORMAT_LENGTH) + "\n");
        fileWriter.write("=".repeat(FORMAT_LENGTH) + "\n");

        String format = "| %-50s | %-10s | %-52s | %-6s |\n";
        fileWriter.write(String.format(format, "NAME", "TYPE", "VALUE", "LENGTH"));
        fileWriter.write("-".repeat(FORMAT_LENGTH) + "\n");

        for (SymbolEntry entry : SymbolTable.getInstance().getAll()) {
            String name = entry.getName();
            String type = entry.getTypeLabel();
            String value = entry.getValue() != null ? entry.getValue() : "-";
            String length = entry.getLength() >= 0 ? String.valueOf(entry.getLength()) : "-";
            fileWriter.write(String.format(format, name, type, value, length));
        }

        fileWriter.write("=".repeat(FORMAT_LENGTH) + "\n");
        fileWriter.write("Total symbols: " + SymbolTable.getInstance().getAll().size() + "\n");
        fileWriter.write("=".repeat(FORMAT_LENGTH) + "\n\n");
    }
}
