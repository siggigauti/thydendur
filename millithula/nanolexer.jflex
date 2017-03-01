import java.io.*;

%%

%public
%class NanoLexer
%unicode
%byaccj

%{

// Tokens:
final static int ERROR = -1;
final static int IF = 1001;
final static int VAR = 1002;
final static int NAME = 1003;
final static int LITERAL = 1004;
final static int OPNAME = 1005;
final static int WHILE = 1006;
final static int ELSE = 1007;
final static int ELSEIF = 1008;
final static int RETURN = 1009;

final static int SVIGIOPNAST = 40;
final static int SVIGILOKAST = 41;
final static int KOMMA = 44;
final static int CURLYOPNAST = 123;
final static int CURLYLOKAST = 125;
final static int SEMIOMMA = 59;
final static int EQUALSIGN = 61;

// lexeme:
public static String lexeme = "SOF";
public static String lexeme2;
public static int token = -1;
public static int token2;

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

public int getToken() {
	return token2;
}

public int getToken2() {
	return token;
}

public String getLexeme() {
	return lexeme2;
}

public void advance() {
	token2 = token;
	lexeme2 = lexeme;

	if(token != 0) {
		try {
			token = yylex();
		} catch (Exception e) {

		}
	}
}

public String over(int tok) {
	if(token2 != tok) throw new Error("Wrong token");
	String res = lexeme2;
	advance();
	return res;
}

public void startLex() {
	advance();
	advance();
}

%}

  /* Reglulegar skilgreiningar */

  /* Regular definitions */

_DIGIT=[0-9]
_FLOAT={_DIGIT}+\.{_DIGIT}+([eE][+-]?{_DIGIT}+)?
_INT={_DIGIT}+
_STRING=\"([^\"\\]|\\b|\\t|\\n|\\f|\\r|\\\"|\\\'|\\\\|(\\[0-3][0-7][0-7])|\\[0-7][0-7]|\\[0-7])*\"
_CHAR=\'([^\'\\]|\\b|\\t|\\n|\\f|\\r|\\\"|\\\'|\\\\|(\\[0-3][0-7][0-7])|(\\[0-7][0-7])|(\\[0-7]))\'
_DELIM=[(){};=,]
_OPNAME=[\+\-*/!%&=><\:\^\~&|?]+
_NAME=([:letter:]|{_DIGIT})+

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
	lexeme = yytext();
	return WHILE;
}

"else" {
	lexeme = yytext();
	return ELSE;
}

"elseif" {
	lexeme = yytext();
	return ELSEIF;
}

"return" {
	lexeme = yytext();
	return RETURN;
}

"var" {
	lexeme = yytext();
	return VAR;
}

{_OPNAME} {
	lexeme = yytext();
	return OPNAME;
}

{_NAME} {
	lexeme = yytext();
	return NAME;
}

";;;".*$ {
}

[ \t\r\n\f] {
}

. {
	lexeme = yytext();
	return ERROR;
}
