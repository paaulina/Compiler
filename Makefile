
.PHONY: all clean

all: Main.class

clean:
	rm *.class Lexer.java parser.java sym.java

Lexer.java: Lexer.flex
	jflex-1.7.0/bin/jflex Lexer.flex

parser.java sym.java: Parser.cup
	java -jar java-cup-11b.jar Parser.cup

Command.class: Command.java Label.class CommandType.class
	javac -cp "java-cup-11b-runtime.jar:." $<

CodeGenerator.class: Command.java Command.class sym.class AboutSymbol.class CompilationError.class
	javac -cp "java-cup-11b-runtime.jar:." $<

parser.class: parser.java Lexer.class CodeGenerator.class
	javac -cp "java-cup-11b-runtime.jar:." $<

Main.class: Main.java parser.class
	javac -cp "java-cup-11b.jar:." $<

%.class: %.java
	javac -cp "java-cup-11b.jar:." $<
