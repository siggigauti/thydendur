/**
	JFlex lesgreinir fyrir NanoLisp.
	H�fundur: Snorri Agnarsson, jan�ar 2014

	�ennan lesgreini m� ���a me� skipununum
		java -jar JFlex.jar nanolisp.jflex
		javac NanoLispLexer.java
 */

%%

%public
%class NanoLispLexer
%unicode
%byaccj

%{

public NanoLispParser yyparser;

public NanoLispLexer( java.io.Reader r, NanoLispParser yyparser )
{
	this(r);
	this.yyparser = yyparser;
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
_NAME=([:letter:]|[\+\-*/!%&=><\:\^\~&|?]|{_DIGIT})+

%%

{_DELIM} {
	yyparser.yylval = new NanoLispParserVal(yytext());
	return yycharat(0);
}

{_STRING} | {_FLOAT} | {_CHAR} | {_INT} | null | true | false {
	yyparser.yylval = new NanoLispParserVal(yytext());
	return NanoLispParser.LITERAL;
}

"if" {
	return NanoLispParser.IF;
}

"define" {
	return NanoLispParser.DEFINE;
}

{_NAME} {
	yyparser.yylval = new NanoLispParserVal(yytext());
	return NanoLispParser.NAME;
}

";".*$ {
}

[ \t\r\n\f] {
}

. {
	return NanoLispParser.YYERRCODE;
}
