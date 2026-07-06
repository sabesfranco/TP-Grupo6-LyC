package lyc.compiler.files;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import lyc.compiler.utils.IntermediateCode;
import lyc.compiler.ParserSym;
import lyc.compiler.model.SymbolEntry;
import lyc.compiler.model.SymbolType;
import lyc.compiler.model.SymbolTable;

public class AsmCodeGenerator implements FileGenerator {

    private static final int MAX_TEXT_SIZE = 50;
    private static BufferedWriter asmBuffer;
    private static Set<Integer> labels = new HashSet<Integer>();

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        asmBuffer = new BufferedWriter(fileWriter);
        generateHeader();
        generateDataSection();
        generateCodeSection();
        generateFooter();
        asmBuffer.flush();
    }
    
    private void generateHeader() throws IOException {
        asmBuffer.write(
                "include number.asm\ninclude macros2.asm\n\n" +
                        ".MODEL LARGE ;Modelo de Memoria\n" +
                        ".386 ;Tipo de Procesador\n" +
                        ".STACK 200h ;Bytes en el Stack\n\n" + 
                        "MAXTEXTSIZE equ " + MAX_TEXT_SIZE + "\n\n");
    }

    private void generateDataSection() throws IOException {
        asmBuffer.write(".DATA\n");
        String format = "\t%-" + MAX_TEXT_SIZE + "s %-3s %s\n";
        for (SymbolEntry entry : SymbolTable.getInstance().getAll()) {
            SymbolType type = entry.getType();
            String name = entry.getName();
            String typeToDefine = (type == SymbolType.STRING)? "db" : "dd";
            String valueToDefine = "";
            if(type == SymbolType.STRING) {
                if(entry.isConstant()) {
                    valueToDefine = entry.getValue() + ", '$'";
                } else {
                    valueToDefine = "MAXTEXTSIZE dup (?), '$'";
                }
            } else {
                valueToDefine = entry.isConstant()? entry.getValue() : "?";
            }
            asmBuffer.write(String.format(format, name, typeToDefine, valueToDefine));
        }
    }

    private SymbolEntry findEntryByName(String name) {
        Collection<SymbolEntry> entries = SymbolTable.getInstance().getAll();
        for (SymbolEntry e : entries) {
            if (e.getName().equals(name)) {
                return e;
            }
        }
        return null;
    }

    private void generateCodeSection() throws IOException {
        asmBuffer.write("\nSTART:\n");
        List<String> lines = IntermediateCode.getLines();

        Boolean assign = false;
        Boolean write = false;
        Boolean read = false;
        Boolean si_reg = false;

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            List<String> cells = Arrays.asList(lines.get(lineIndex).split(" "));
            for (int c = 0; c < cells.size(); c++) {
                String cell = cells.get(c);

                // Si la celda es un punto de salto, creamos la etiqueta.
                if (labels.remove(lineIndex)) {
                    asmBuffer.write("ETQ_" + lineIndex + ":\n");
                }

                SymbolEntry entry = findEntryByName(cell);
                if (entry != null) {
                    if (assign) {
                        switch (entry.getType()) {
                            case INT -> asmBuffer.write("\tFSTP " + entry.getName() + '\n');
                            case FLOAT -> asmBuffer.write("\tFSTP " + entry.getName() + '\n');
                            case STRING -> {
                                if (si_reg) {
                                    asmBuffer.write("\tmov DI, offset " + entry.getName() + '\n');
                                    asmBuffer.write("\tmov CX, 0\n");
                                    si_reg = false;
                                } else {
                                    asmBuffer.write("\tmov SI, offset " + entry.getName() + '\n');
                                    si_reg = true;
                                }
                                asmBuffer.write("\tSTRCPY 0\n");
                            }
                        }
                        assign = false;
                    } else if (write) { 
                        switch (entry.getType()) {
                           case INT -> asmBuffer.write("\tDisplayFloat " + entry.getName() + ", 2\n");
                           case FLOAT -> asmBuffer.write("\tDisplayFloat " + entry.getName() + ", 2\n");
                           case STRING -> asmBuffer.write("\tDisplayString " + entry.getName() + '\n');
                        }
                        write = false;
                    } else if (read) {
                        switch (entry.getType()) {
                            case INT -> asmBuffer.write("\tGetFloat " + entry.getName() + '\n');
                            case FLOAT -> asmBuffer.write("\tGetFloat " + entry.getName() + '\n');
                            case STRING -> asmBuffer.write("\tGetString " + entry.getName() + '\n');
                        }
                        read = false;
                    } else {
                        switch (entry.getType()) {
                            case INT -> asmBuffer.write("\tFLD " + entry.getName() + '\n');
                            case FLOAT -> asmBuffer.write("\tFLD " + entry.getName() + '\n');
                            case STRING -> {
                                if (si_reg) {
                                    asmBuffer.write("\tmov DI, offset " + entry.getName() + '\n');
                                    asmBuffer.write("\tmov CX, 0\n");
                                    si_reg = false;
                                } else {
                                    asmBuffer.write("\tmov SI, offset " + entry.getName() + '\n');
                                    si_reg = true;
                                }
                            }
                        }
                    }

                } else {
                    // Probamos si la celda es una etiqueta.
                try {
                    labels.add(Integer.parseInt(cell));
                    asmBuffer.write("ETQ_" + cell + '\n');
                } catch (NumberFormatException e) {
                    // Si es una etiqueta de iteracion.
                    if (cell.contains("LOOP") || cell.contains("FIND_IDX")) {
                        if (cell.startsWith(".")) {
                                asmBuffer.write(cell.replace(".", "") + ":\n");
                            } else {
                                asmBuffer.write(cell + "\n");
                            }
                        } else {
                            switch (cell) {
                                case "CMP" -> asmBuffer.write("\tFXCH\n\tFCOM\n\tFSTSW AX\n\tSAHF\n\tFFREE\n");
                                case "BLT" -> asmBuffer.write("\tJB ");
                                case "BLE" -> asmBuffer.write("\tJBE ");
                                case "BGT" -> asmBuffer.write("\tJA ");
                                case "BGE" -> asmBuffer.write("\tJAE ");
                                case "BEQ" -> asmBuffer.write("\tJE ");
                                case "BNE" -> asmBuffer.write("\tJNE ");
                                case "BI" -> asmBuffer.write("\tJMP ");
                                case "+" -> asmBuffer.write("\tFADD\n");
                                case "-" -> asmBuffer.write("\tFSUB\n");
                                case "*" -> asmBuffer.write("\tFMUL\n");
                                case "/" -> asmBuffer.write("\tFDIV\n");
                                case "=" -> assign = true;
                                case "offset" ->
                                    asmBuffer.write(
                                        "\tmov AX, [SI]\n\tmov WORD PTR [@elem], AX\n\tadd SI, 4\n");
                                case "write" -> write = true;
                                case "read" -> read = true;
                            }
                        }
                    }
                }
            }
        }
        
        for (Integer id : labels) {
            asmBuffer.write("ETQ_" + id + ":\n");
        }
    }

    private void generateFooter() throws IOException {
        asmBuffer.write("\n\tMOV EAX, 4C00h\n\tINT 21h\n\nEND START");
    }
}
