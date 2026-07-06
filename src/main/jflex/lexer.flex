package lyc.compiler;

import java_cup.runtime.Symbol;
import lyc.compiler.ParserSym;
import lyc.compiler.model.*;
import static lyc.compiler.constants.Constants.*;

%%

%public
%class Lexer
%unicode
%cup
%line
%column
%throws CompilerException
%eofval{
  return symbol(ParserSym.EOF);
%eofval}

%state COMMENT
%state COMMENT_C

%{
  private Symbol symbol(int type) {
    return new Symbol(type, yyline, yycolumn);
  }
  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline, yycolumn, value);
  }
%}


LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
Identation =  [ \t\f]

Plus = "+"
Mult = "*"
Sub = "-"
Div = "/"
OpenBracket = "("
CloseBracket = ")"
Letter = [a-zA-Z]
Digit = [0-9]

WhiteSpace = {LineTerminator} | {Identation}
Identifier = {Letter} ({Letter}|{Digit})*
IntegerConstant = {Digit}+

Init = "init"
FloatType = "Float"
IntType = "Int"
StringType = "String"
If = "if"
Else = "else"
While = "while"
WhileSpecial = "WHILE"
Read = "read"
Write = "write"
And = "AND"
Or = "OR"
Not = "NOT"
In = "IN"
Do = "DO"
EndWhile = "ENDWHILE"
Mod = "MOD"
DivInt = "DIV"
OpenSquareBracket = "["
CloseSquareBracket = "]"

Assig        = ":=" | "=" | "=:"
OpenBrace       = "{"
CloseBrace      = "}"
Colon           = ":"
Comma           = ","
Eq              = "=="
Le              = "<="
Ge              = ">="
Lt              = "<"
Gt              = ">"
Ne              = "!="
FloatConstant           = {Digit}+ "." {Digit}* | "." {Digit}+
StringConstant  = \"[^\"\r\n]*\" | \u201C[^\u201D\r\n]*\u201D

%%


<YYINITIAL> {
  /* comments */
  "#+"                                      { yybegin(COMMENT); }
  "/*"                                      { yybegin(COMMENT_C); }

  /* keywords */
  {Init}                                    { return symbol(ParserSym.INIT); }
  {FloatType}                               { return symbol(ParserSym.FLOAT_TYPE); }
  {IntType}                                 { return symbol(ParserSym.INT_TYPE); }
  {StringType}                              { return symbol(ParserSym.STRING_TYPE); }
  {If}                                      { return symbol(ParserSym.IF); }
  {Else}                                    { return symbol(ParserSym.ELSE); }
  {While}                                   { return symbol(ParserSym.WHILE); }
  {WhileSpecial}                            { return symbol(ParserSym.WHILE_SPECIAL); }
  {Read}                                    { return symbol(ParserSym.READ); }
  {Write}                                   { return symbol(ParserSym.WRITE); }
  {And}                                     { return symbol(ParserSym.AND); }
  {Or}                                      { return symbol(ParserSym.OR); }
  {Not}                                     { return symbol(ParserSym.NOT); }
  {In}                                      { return symbol(ParserSym.IN); }
  {Do}                                      { return symbol(ParserSym.DO); }
  {EndWhile}                                { return symbol(ParserSym.ENDWHILE); }
  {Mod}                                     { return symbol(ParserSym.MOD); }
  {DivInt}                                  { return symbol(ParserSym.DIV_INT); }

  /* identifiers */
  {Identifier}                              {
    String val = yytext();
    if (val.length() > MAX_LENGTH) throw new InvalidLengthException("Identificador demasiado largo: " + val);
    return symbol(ParserSym.IDENTIFIER, val);
  }

  /* constants - float antes que integer, negativo antes que SUB */
  {FloatConstant}                           { 
    try {
        double value = Double.parseDouble(yytext());
        if (value > Float.MAX_VALUE) {
            throw new InvalidIntegerException("Constante flotante fuera de rango: " + yytext());
        }
    } catch (NumberFormatException ex) {
        throw new InvalidIntegerException("Constante flotante invalida: " + yytext());
    }
    return symbol(ParserSym.FLOAT_CONSTANT, yytext());
  }
  {IntegerConstant}                         {
    try {
      long val = Long.parseLong(yytext());
      if (val > Short.MAX_VALUE) throw new InvalidIntegerException("Constante entera fuera de rango: " + yytext());
    } catch (NumberFormatException e) {
      throw new InvalidIntegerException("Constante entera invalida: " + yytext());
    }
    return symbol(ParserSym.INTEGER_CONSTANT, yytext());
  }
  {StringConstant}                          {
    String val = yytext();
    if (val.length() - 2 > 50) throw new InvalidLengthException("String demasiado largo: " + val);
    return symbol(ParserSym.STRING_CONSTANT, val);
  }

  /* comparators - dobles antes que simples */
  {Eq}                                      { return symbol(ParserSym.EQ); }
  {Le}                                      { return symbol(ParserSym.LE); }
  {Ge}                                      { return symbol(ParserSym.GE); }
  {Ne}                                      { return symbol(ParserSym.NE); }
  {Lt}                                      { return symbol(ParserSym.LT); }
  {Gt}                                      { return symbol(ParserSym.GT); }

  /* operators */
  {Plus}                                    { return symbol(ParserSym.PLUS); }
  {Sub}                                     { return symbol(ParserSym.SUB); }
  {Mult}                                    { return symbol(ParserSym.MULT); }
  {Div}                                     { return symbol(ParserSym.DIV); }
  {Assig}                                   { return symbol(ParserSym.ASSIG); }
  {OpenBracket}                             { return symbol(ParserSym.OPEN_BRACKET); }
  {CloseBracket}                            { return symbol(ParserSym.CLOSE_BRACKET); }
  {OpenSquareBracket}                       { return symbol(ParserSym.OPEN_SQUARE_BRACKET); }
  {CloseSquareBracket}                      { return symbol(ParserSym.CLOSE_SQUARE_BRACKET); }
  {OpenBrace}                               { return symbol(ParserSym.OPEN_BRACE); }
  {CloseBrace}                              { return symbol(ParserSym.CLOSE_BRACE); }
  {Colon}                                   { return symbol(ParserSym.COLON); }
  {Comma}                                   { return symbol(ParserSym.COMMA); }

  /* whitespace */
  {WhiteSpace}                              { /* ignore */ }
}

<COMMENT> {
  "+#"                                      { yybegin(YYINITIAL); }
  [^+\r\n@]+                                 { /* ignore */ }
  "+"                                       { /* ignore */ }
  {LineTerminator}                          { /* ignore */ }
}

<COMMENT_C> {
  "*/"                                      { yybegin(YYINITIAL); }
  [^*\r\n@]+                                 { /* ignore */ }
  "*"                                       { /* ignore */ }
  {LineTerminator}                          { /* ignore */ }
}


/* error fallback */
[^]                              { throw new UnknownCharacterException(yytext()); }
