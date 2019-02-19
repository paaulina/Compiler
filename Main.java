import java.io.*;

public class Main {
    public static void main(String[] args) {
        if (args.length == 2) {
            try (Reader in = new FileReader(args[0]);
                 PrintWriter out = new PrintWriter(args[1])) {
                CodeGenerator codeGen = new CodeGenerator();
                parser parser = new parser(new Lexer(in), codeGen);
                parser.parse();
                out.print(codeGen.result);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CompilationError e) {
                System.out.println(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
