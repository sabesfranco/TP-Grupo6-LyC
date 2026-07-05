package lyc.compiler.files;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import lyc.compiler.utils.IntermediateCode;
import lyc.compiler.ParserSym;
import lyc.compiler.model.SymbolEntry;
import lyc.compiler.model.SymbolType;
import lyc.compiler.model.SymbolTable;

public class AsmCodeGenerator implements FileGenerator {

    private static final int MAX_TEXT_SIZE = 50;
    private static BufferedWriter asmBuffer;

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
            asmBuffer.write("\n\n");
        }
    }

    private void generateCodeSection() throws IOException {
        List<String> lines = IntermediateCode.getLines();
        for (int i = 0; i < lines.size(); i++) {
            asmBuffer.write("\n" + lines.get(i)); // TODO
        }
    }

    private void generateFooter() throws IOException {
        asmBuffer.write("\nend_main:\n\tMOV EAX, 4C00h\n\tINT 21h\n\nEND main");
    }
}
