import java_cup.runtime.*;

%%

%public
%class Lexer
%unicode
%cup

%{
    private Symbol symbol(int type) {
        return new Symbol(type, yyline, yycolumn);
    }

    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline, yycolumn, value);
    }
%}

White = [ \t\f\r\n]
Num = [0-9]+
Pidentifier = [_a-z]+

%%
<YYINITIAL> {
      \[[^\]]*\] {
      }
      {White} {
      }
      {Num} {
          return symbol(sym.num, Long.valueOf(yytext()));
      }
      {Pidentifier} {
          return symbol(sym.pidentifier, yytext());
      }
      "(" {
          return symbol(sym.LEFT);
      }
      ")" {
          return symbol(sym.RIGHT);
      }
      "+" {
          return symbol(sym.PLUS);
      }
      "-" {
          return symbol(sym.MINUS);
      }
      "*" {
          return symbol(sym.TIMES);
      }
      "/" {
          return symbol(sym.DIVIDE);
      }
      "%" {
          return symbol(sym.MODULO);
      }

      "=" {
           return symbol(sym.EQ);
      }
      "!=" {
          return symbol(sym.NEQ);
      }
      ">" {
          return symbol(sym.GT);
      }
      "<" {
           return symbol(sym.LT);
      }
      ">=" {
           return symbol(sym.GE);
      }
      "<=" {
           return symbol(sym.LE);
      }
      "DECLARE" {
          return symbol(sym.DECLARE);
       }
      "IN" {
          return symbol(sym.IN);
      }
      "END" {
          return symbol(sym.END);
      }
      ":" {
          return symbol(sym.COLON);
      }
      ";" {
          return symbol(sym.SEMI);
      }
      ":=" {
          return symbol(sym.ASSIGN);
      }
      "IF" {
          return symbol(sym.IF);
      }
      "THEN" {
          return symbol(sym.THEN);
      }
      "ELSE" {
          return symbol(sym.ELSE);
      }
      "ENDIF" {
          return symbol(sym.ENDIF);
      }
      "WHILE" {
          return symbol(sym.WHILE);
      }
      "DO" {
          return symbol(sym.DO);
      }
      "ENDWHILE" {
          return symbol(sym.ENDWHILE);
      }
      "ENDDO" {
          return symbol(sym.ENDDO);
      }
      "FOR" {
          return symbol(sym.FOR);
      }
      "FROM" {
          return symbol(sym.FROM);
      }
      "TO" {
          return symbol(sym.TO);
      }
      "DOWNTO" {
          return symbol(sym.DOWNTO);
      }
      "ENDFOR" {
          return symbol(sym.ENDFOR);
      }
      "READ" {
          return symbol(sym.READ);
      }
      "WRITE" {
          return symbol(sym.WRITE);
      }

      <<EOF>> {
          return symbol(sym.EOF);
      }
      [^] {
          throw new RuntimeException("Błąd leksykalny");
      }

}
