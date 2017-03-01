import java.io.Reader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Vector;
import java.util.HashMap;

public class NanoParser
{
    NanoLexer lex;
	private static int varCount;
	private static HashMap<String,Integer> varTable;

	private static void addVar( String name )
	{
		if( varTable.get(name) != null )
			throw new Error("Variable "+name+" already exists");
		varTable.put(name,varCount++);
	}

	private static int findVar( String name )
	{
		Integer res = varTable.get(name);
		if( res == null )
			throw new Error("Variable "+name+" does not exist");
		return res;
	}

    public NanoParser( NanoLexer lexer )
    {
        lex = lexer;
    }

    Object[] program()
    {
        Vector<Object> res = new Vector<Object>();
        while( lex.getToken() == NanoLexer.NAME ) res.add(function());
        return res.toArray();
    }

    Object[] function()
    {
        varCount = 0;
        varTable = new HashMap<String,Integer>();
        String name = lex.over(NanoLexer.NAME);
        lex.over(NanoLexer.SVIGIOPNAST);
        if( lex.getToken() != NanoLexer.SVIGILOKAST ) {
            addVar(lex.over(NanoLexer.NAME));
            while( lex.getToken() != NanoLexer.SVIGILOKAST) {
                lex.over(NanoLexer.KOMMA);
                addVar(lex.over(NanoLexer.NAME));
            }
        }
        lex.over(NanoLexer.SVIGILOKAST);
        lex.over(NanoLexer.CURLYOPNAST);

        int decls = declList();
        Object[] exprs = exprList();

        Object[] res = new Object[]{name,varCount-decls,decls,exprs};
        lex.over(NanoLexer.CURLYLOKAST);
        return res;
    }

    int declList() {
        int count = 0;

        if( lex.getToken() == NanoLexer.VAR) {
            count += decl();
            lex.over(NanoLexer.SEMIOMMA);
            while( lex.getToken() == NanoLexer.VAR ) {
                count += decl();
                lex.over(NanoLexer.SEMIOMMA);
            }
        }

        return count;
    }

    int decl() {
        int innerCount = 0;

        lex.over(NanoLexer.VAR);
        addVar(lex.over(NanoLexer.NAME));
        innerCount++;
        while( lex.getToken() == NanoLexer.KOMMA ) {
            lex.over(NanoLexer.KOMMA);
            addVar(lex.over(NanoLexer.NAME));
            innerCount++;
        }

        return innerCount;
    }

    Object[] exprList() {
        Vector<Object> res = new Vector<Object>();
        while( lex.getToken() != NanoLexer.CURLYLOKAST ) {
            res.add(expr());
            lex.over(NanoLexer.SEMIOMMA);
        }
        return res.toArray();
    }

    Object[] expr() {
        switch( lex.getToken() ) {
        case NanoLexer.RETURN:
            lex.over(NanoLexer.RETURN);
            return new Object[]{"RETURN", expr()};
        case NanoLexer.NAME:
            if( lex.getToken2() == NanoLexer.EQUALSIGN ) {
                int tmp = findVar(lex.over(NanoLexer.NAME));
                lex.over(NanoLexer.EQUALSIGN);
                return new Object[]{"ASSIGN", tmp, expr()};
            }
        default:
            return binopexpr();
        }
    }

    Object[] binopexpr() {
        Object[] res = smallexpr();
        while( lex.getToken() == NanoLexer.OPNAME ) {
            Object opname = lex.over(NanoLexer.OPNAME);
            Object[] tmp = new Object[]{"CALL", opname, new Object[]{res, smallexpr()}};
            res = tmp;
        }

        return res;
    }

    Object[] smallexpr() {
        switch( lex.getToken() ) {
        case NanoLexer.NAME:
            if( lex.getToken2() != NanoLexer.SVIGIOPNAST ) {
                return new Object[]{"NAME", findVar(lex.over(NanoLexer.NAME))};
            } else {
                String name = lex.over(NanoLexer.NAME);
                Vector<Object> argsTmp = new Vector<Object>();
                lex.over(NanoLexer.SVIGIOPNAST);
                if( lex.getToken() != NanoLexer.SVIGILOKAST ) {
                    argsTmp.add(expr());
                    while( lex.getToken() != NanoLexer.SVIGILOKAST ) {
                        lex.over(NanoLexer.KOMMA);
                        argsTmp.add(expr());
                    }
                }
                lex.over(NanoLexer.SVIGILOKAST);
                Object[] args = argsTmp.toArray();
                return new Object[]{"CALL", name, args};
            }
        case NanoLexer.OPNAME:
            return new Object[]{"CALL", lex.over(NanoLexer.OPNAME), smallexpr()};
        case NanoLexer.LITERAL:
            return new Object[]{"LITERAL", lex.over(NanoLexer.LITERAL)};
        case NanoLexer.SVIGIOPNAST:
            lex.over(NanoLexer.SVIGIOPNAST);
            Object[] res = expr();
            lex.over(NanoLexer.SVIGILOKAST);
            return res;
        case NanoLexer.IF:
            Vector<Object> resIf = new Vector<Object>();
            resIf.add("IF");
            lex.over(NanoLexer.IF);
            resIf.add(expr());
            resIf.add(body());
            while( lex.getToken() == NanoLexer.ELSEIF ) {
                lex.over(NanoLexer.ELSEIF);
                resIf.add(expr());
                resIf.add(body());
            }
            if( lex.getToken() == NanoLexer.ELSE ) {
                lex.over(NanoLexer.ELSE);
                resIf.add(body());
            }
            return resIf.toArray();
        case NanoLexer.WHILE:
            lex.over(NanoLexer.WHILE);
            Object[] resExpr = expr();
            Object[] resBody = body();
            return new Object[]{"WHILE", resExpr, resBody};
        default:
            throw new Error("Expected an expression, found "+lex.getLexeme());
        }
    }

    Object[] body() {
        Vector<Object> res = new Vector<Object>();
        lex.over(NanoLexer.CURLYOPNAST);
        while( lex.getToken() != NanoLexer.CURLYLOKAST) {
            res.add(expr());
            lex.over(NanoLexer.SEMIOMMA);
        }
        lex.over(NanoLexer.CURLYLOKAST);
        return res.toArray();
    }

    static void generateProgram( String filename, Object[] funs )
    {
        String programname = filename.substring(0,filename.indexOf('.'));
        System.out.println("\""+programname+".mexe\" = main in");
        System.out.println("!");
        System.out.println("{{");
        for( Object f: funs )
        {
            generateFunction((Object[])f);
        }
        System.out.println("}}");
        System.out.println("*");
        System.out.println("BASIS;");
    }

    static void generateFunction( Object[] fun )
    {
        System.out.println("#\"" + (String)fun[0] + "[f" + (int)fun[1] + "]\" =");
        System.out.println("  [");
        Object[] ex = (Object[]) fun[3];
        for(int i = 0; i < ex.length; i++) {
            generateExpr((Object[])ex[i]);
        }
        System.out.println("  ];");
    }

    //static int nextLab = 0; // ???

    static void generateExpr( Object[] e )
    {
        System.out.println("  " + (String)e[0]);
    }

    static void generateBody( Object[] bod )
    {

    }

    public static void main( String[] args )
    throws IOException
    {
      NanoLexer lexer = new NanoLexer(new FileReader(args[0]));
      lexer.startLex();
      String name = args[0].substring(0,args[0].lastIndexOf('.'));
      NanoParser parser = new NanoParser(lexer);
      Object[] code = parser.program();
      generateProgram(args[0], code);
      if( lexer.getToken()!=0 ) throw new Error("Expected EOF, found "+lexer.getLexeme());
  }
}
