/**
	JFlex lexgreiningardæmi byggt á lesgreini fyrir NanoLisp.
	Höfundur: Snorri Agnarsson, janúar 2017

	Þennan lesgreini má þýða og keyra með skipununum
		java -jar JFlex-1.6.0.jar nanolexer.jflex
		javac NanoLexer.java
		java NanoLexer inntaksskrá > úttaksskrá
	Einnig má nota forritið 'make', ef viðeigandi 'makefile'
	er til staðar:
		make test
 */

import java.io.*;

%%

%public
%class NanoLexer
%unicode
%byaccj

%{

// Skilgreiningar á tókum (tokens):
final static int ERROR = -1;
final static int IF = 1001;
final static int NAME = 1003;
final static int LITERAL = 1004;
final static int OPNAME = 1005;
final static int WHILE = 1006;
final static int ELSE = 1007;
final static int ELSEIF = 1008;
final static int RETURN = 1009;

// Breyta sem mun innihalda les (lexeme):
public static String lexeme;

// Þetta keyrir lexgreininn:
public static void main( String[] args ) throws Exception
{
	NanoLexer lexer = new NanoLexer(new FileReader(args[0]));
	int token = lexer.yylex();
	while( token!=0 )
	{
		System.out.println(""+token+": \'"+lexeme+"\'");
		token = lexer.yylex();
	}
}

%}

  /* Reglulegar skilgreiningar */

  /* Regular definitions */

_DIGIT=[0-9]
_FLOAT={_DIGIT}+\.{_DIGIT}+([eE][+-]?{_DIGIT}+)?
_INT={_DIGIT}+
_STRING=\"([^\"\\]|\\b|\\t|\\n|\\f|\\r|\\\"|\\\'|\\\\|(\\[0-3][0-7][0-7])|\\[0-7][0-7]|\\[0-7])*\"
_CHAR=\'([^\'\\]|\\b|\\t|\\n|\\f|\\r|\\\"|\\\'|\\\\|(\\[0-3][0-7][0-7])|(\\[0-7][0-7])|(\\[0-7]))\'
_DELIM=[()]
_OPNAME??
_NAME=([:letter:]|[\+\-*/!%&=><\:\^\~&|?]|{_DIGIT})+

%%

  /* Lesgreiningarreglur */

{_DELIM} {
	lexeme = yytext();
	return yycharat(0);
}

{_STRING} | {_FLOAT} | {_CHAR} | {_INT} | null | true | false {
	lexeme = yytext();
	return LITERAL;
}

"if" {
	lexeme = yytext();
	return IF;
}

"while" {
	lexme = yytext();
	return WHILE;
}

"else" {
	lexme = yytext();
	return ELSE;
}

"elseif" {
	lexme = yytext();
	return ELSEIF;
}

"return" {
	lexme = yytext();
	return RETURN;
}

{_OPNAME} {
	lexme = yytext();
	return OPNAME;
}

{_NAME} {
	lexeme = yytext();
	return NAME;
}

";".*$ {
}

[ \t\r\n\f] {
}

. {
	lexeme = yytext();
	return ERROR;
}
