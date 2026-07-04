package lyc.compiler.files;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import lyc.compiler.utils.IntermediateCode;

public class AsmCodeGenerator implements FileGenerator {
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
                        ".STACK 200h ;Bytes en el Stack\n\n");
    }

    private void generateDataSection() throws IOException {
        asmBuffer.write(".DATA\n"); // TODO
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
