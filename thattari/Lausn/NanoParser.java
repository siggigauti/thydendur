// Þýðandi fyrir ofureinfalt NanoLisp forritunarmál.
// Höfundur: Snorri Agnarsson, 2014-2016.

import java.io.Reader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Vector;

public class NanoParser
{
	// Eftirfarandi fastar standa fyrir allar þær
	// mögulegu gerðir af segðum sem milliþula
	// (intermediate code) getur innihaldið.
	// Þessar fjórar gerðir segða (ásamt þeim
	// möguleika að skrifa föll sem nota slíkar
	// segðir) duga reyndar til að hægt sé að
	// reikna hvað sem er reiknanlegt.
	enum CodeType
	{
		RETURN,
        ASSIGN,
        CALL,
        NAME,
        LITERAL,
        ELSEIF,
        ELSE,
        IF,
        WHILE
    };

    final static int SVIGIOPNAST = 40;
    final static int SVIGILOKAST = 41;
    final static int KOMMA = 44;
    final static int CURLYOPNAST = 123;
    final static int CURLYLOKAST = 125;
    final static int SEMIOMMA = 59;
    final static int EQUALSIGN = 61;

	// lex er lesgreinirinn.
    NanoLexer lex;
	// Inni í hverri fallsskilgreiningu inniheldur vars nöfnin
	// á viðföngunum í fallið (þ.e. leppunum eða breytunöfnunum
	// sem standa fyrir viðföngin), í sætum 1 og aftar.  Sæti
	// 0 inniheldur nafn fallsins sem verið er að skilgreina.
    String[] vars;

	// Notkun: NanoLisp n = new NanoLisp(l);
	// Fyrir:  l er lesgreinir.
	// Eftir:  n vísar á nýjan NanoLisp þýðanda sem þýðir inntakið
	//         sem l hefur.
    public NanoParser( NanoLexer lexer )
    {
        lex = lexer;
    }

    void program()
    {
        Vector<Object> res = new Vector<Object>();
        while( lex.getToken() == NanoLexer.NAME ) res.add(function());
        //return res.toArray();
    }

    Object[] function()
    {
        Vector<String> args = new Vector<String>();
        args.add(lex.over(NanoLexer.NAME));
        lex.over(NanoParser.SVIGIOPNAST);
        if( lex.getToken() != NanoParser.SVIGILOKAST ) {
            args.add(lex.over(NanoLexer.NAME));
            while( lex.getToken() != NanoParser.SVIGILOKAST) {
                lex.over(NanoParser.KOMMA);
                args.add(lex.over(NanoLexer.NAME));
            }
        }
        lex.over(NanoParser.SVIGILOKAST);
        lex.over(NanoParser.CURLYOPNAST);
        vars = new String[args.size()];
        args.toArray(vars);

        Object[] decls = declList();
        Object[] exprs = exprList();

        Object[] res = new Object[]{vars[0],vars.length-1,decls,exprs}; // decl fjöldi ásamt declPos falli?
        vars = null;
        lex.over(NanoParser.CURLYLOKAST);
        return res;
    }

    Object[] declList() {
        Vector<Object> res = new Vector<Object>();

        if( lex.getToken() == NanoLexer.VAR) {
            res.add(decl());
            lex.over(NanoParser.SEMIOMMA);
            while( lex.getToken() == NanoLexer.VAR ) {
                res.add(decl());
                lex.over(NanoParser.SEMIOMMA);
            }
        }

        return res.toArray();
    }

    Object[] decl() {
        Vector<Object> res = new Vector<Object>();

        lex.over(NanoLexer.VAR);
        res.add(lex.over(NanoLexer.NAME));
        while( lex.getToken() == NanoParser.KOMMA ) {
            lex.over(NanoParser.KOMMA);
            res.add(lex.over(NanoLexer.NAME));
        }

        return res.toArray();
    }

    Object[] exprList() {
        Vector<Object> res = new Vector<Object>();
        while( lex.getToken() != NanoParser.CURLYLOKAST ) {
            res.add(expr());
            lex.over(NanoParser.SEMIOMMA);
        }

        return res.toArray();
    }

    Object[] expr() {
        switch( lex.getToken() ) {
        case NanoLexer.RETURN:
            lex.over(NanoLexer.RETURN);
            return new Object[]{CodeType.RETURN, expr()};
        case NanoLexer.NAME:
            if( lex.getToken2() == NanoParser.EQUALSIGN ) {
                    Object tmp = lex.over(NanoLexer.NAME); // var/decl pos?
                    lex.over(NanoParser.EQUALSIGN);
                    return new Object[]{CodeType.ASSIGN, tmp, expr()}; // Annars breyta tmp í Object[]
            }
        default:
            return binopexpr();
        }
    }

    Object[] binopexpr() {
        Object[] res = smallexpr();
        while( lex.getToken() != NanoParser.SEMIOMMA ) {
            Object opname = lex.over(NanoLexer.OPNAME);
            Object[] tmp = new Object[]{CodeType.CALL, opname, new Object[]{res, smallexpr()}}; // Nesting í lægi? eða frekar listi? Tilvísanir ef listi?
            res = tmp;
        }

        return res;
    }

    Object[] smallexpr() {
        switch( lex.getToken() ) {
        case NanoLexer.NAME:
            if( lex.getToken2() != NanoParser.SVIGIOPNAST ) {
                return new Object[]{CodeType.NAME, lex.over(NanoLexer.NAME)}; // var/decl pos frekar?
            } else {
                Object name = lex.over(NanoLexer.NAME);
                Vector<Object> argsTmp = new Vector<Object>();
                lex.over(NanoParser.SVIGIOPNAST);
                if( lex.getToken() != NanoParser.SVIGILOKAST ) {
                    argsTmp.add(expr());
                    while( lex.getToken() != NanoParser.SVIGILOKAST ) {
                        lex.over(NanoParser.KOMMA);
                        argsTmp.add(expr());
                    }
                }
                Object[] args = argsTmp.toArray();
                return new Object[]{CodeType.CALL, name, args};
            }
        case NanoLexer.OPNAME:
            return new Object[]{CodeType.CALL, lex.over(NanoLexer.OPNAME), smallexpr()};
        case NanoLexer.LITERAL:
            return new Object[]{CodeType.LITERAL, lex.over(NanoLexer.LITERAL)};
        case NanoParser.SVIGIOPNAST:
            lex.over(NanoParser.SVIGIOPNAST);
            Object[] res = expr();
            lex.over(NanoParser.SVIGILOKAST);
            return res; // Held þetta sé rétt. Skila einhverju CodeType til að gefa til kynna sviga?
        case NanoLexer.IF:
            lex.over(NanoLexer.IF);
            Object[] ifExpr = expr();
            Object[] ifBody = body();
            Vector<Object> elif = new Vector<Object>();
            Object[] elseBody = null; // virkar sennilega ekki?
            while( lex.getToken() == NanoLexer.ELSEIF ) {
                lex.over(NanoLexer.ELSEIF);
                Object[] elifExpr = expr();
                Object[] elifBody = body();
                elif.add(new Object[]{CodeType.ELSEIF, elifExpr, elifBody});
            }
            if( lex.getToken() == NanoLexer.ELSE ) {
                elseBody = new Object[]{CodeType.ELSE, expr()};
            }
            return new Object[]{CodeType.IF, ifExpr, ifBody, elif.toArray(), elseBody};
        case NanoLexer.WHILE:
            lex.over(NanoLexer.WHILE);
            Object[] resExpr = expr();
            Object[] resBody = body();
            return new Object[]{CodeType.WHILE, resExpr, resBody};
        default:
            throw new Error("Expected an expression, found "+lex.getLexeme());
        }
    }

    Object[] body() {
        Object[] res = new Object[]{};
        lex.over(NanoParser.CURLYOPNAST);
        while( lex.getToken() != NanoParser.CURLYLOKAST) {
            res = expr();
            lex.over(NanoParser.SEMIOMMA);
        }
        lex.over(NanoParser.CURLYLOKAST);
        return res;
    }

    public static void main( String[] args )
    throws IOException
    {
      NanoLexer lexer = new NanoLexer(new FileReader(args[0]));
      lexer.startLex();
      String name = args[0].substring(0,args[0].lastIndexOf('.'));
      NanoParser parser = new NanoParser(lexer);
      parser.program();
      if( lexer.getToken()!=0 ) throw new Error("Expected EOF, found "+lexer.getLexeme());
  }
}
