package lyc.compiler.files;

import java.io.FileWriter;
import java.io.IOException;
import lyc.compiler.utils.IntermediateCode;

public class IntermediateCodeGenerator implements FileGenerator {

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        fileWriter.write("=============================================\n");
        fileWriter.write("     CODIGO INTERMEDIO - POLACA INVERSA\n");
        fileWriter.write("=============================================\n\n");

        int lineNumber = 1;
        for (String line : IntermediateCode.getLines()) {
            fileWriter.write(String.format("%4d: %s%n", lineNumber++, line));
        }

        fileWriter.write("\n=============================================\n");
    }
}
