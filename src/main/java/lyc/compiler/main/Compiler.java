package lyc.compiler.main;

import lyc.compiler.Parser;
import lyc.compiler.factories.FileFactory;
import lyc.compiler.factories.ParserFactory;
import lyc.compiler.files.AsmCodeGenerator;
import lyc.compiler.files.FileOutputWriter;
import lyc.compiler.files.IntermediateCodeGenerator;
import lyc.compiler.files.SymbolTableGenerator;
import lyc.compiler.model.SymbolTable;
import lyc.compiler.utils.IntermediateCode;
import lyc.compiler.utils.SemanticChecker;

import java.io.IOException;
import java.io.Reader;

public final class Compiler {

    private Compiler(){}

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Filename must be provided as argument.");
            System.exit(0);
        }

        try (Reader reader = FileFactory.create(args[0])) {
            SymbolTable.getInstance().clear();
            IntermediateCode.clear();
            SemanticChecker.clear();

            Parser parser = ParserFactory.create(reader);
            parser.parse();

            if (SemanticChecker.hasErrors()) {
                System.err.println("\n[ERROR] Compilation failed due to semantic errors");
                System.exit(1);
            }

            FileOutputWriter.writeOutput("symbol-table.txt", new SymbolTableGenerator());
            FileOutputWriter.writeOutput("intermediate-code.txt", new IntermediateCodeGenerator());
            FileOutputWriter.writeOutput("final.asm", new AsmCodeGenerator());

            System.out.println("Compilation Successful");
        } catch (IOException e) {
            System.err.println("There was an error trying to read input file " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Compilation error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
