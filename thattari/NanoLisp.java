// Þýðandi fyrir ofureinfalt NanoLisp forritunarmál.
// Höfundur: Snorri Agnarsson, 2014-2016.


// Startlex(), advance(), over(t), nextToken(), nextNextToken(), lexeme()


import java.io.Reader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Vector;

public class NanoLisp
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
		IF, LITERAL, NAME, CALL
	};

	// Tilvik af klasanum LexCase er skilgreining
	// á einhverju lesgreinanlegu fyrirbæri, sem
	// má þá koma fyrir í löglegum forritstexta.
	// Lesgreinirinn í þessum þýðanda er skrifaður
	// á mjög frumstæðan hátt og aðferðin er ekki
	// til eftirbreytni í neinum þýðanda sem ætlaður
	// er til raunverulegrar notkunar.
	static class LexCase
	{
		final Pattern pat;
		final char token;
		int end;
		
		// Notkun: LexCase c = new LexCase(p,t);
		// Fyrir:  p er strengur sem inniheldur reglulega
		//         segð sem skilgreinir mál sem inniheldur
		//         þá strengi sem sem eiga að flokkast sem
		//         eitthvert tiltekið lesgreinanlegt
		//         fyrirbæri (les, lexeme). t er stafur
		//         sem er það tók (token) sem stendur
		//         fyrir þetta mengi strengja (þetta mál).
		// Eftir:  c vísar á hlut sem nota má til að
		//         bera kennsl á strengi í málinu.
		public LexCase( String p, char t )
		{
			pat = Pattern.compile(p,Pattern.MULTILINE);
			token = t;
		}

		// Notkun: boolean b = c.match(s,pos);
		// Fyrir:  s er strengur og pos er staðsetning
		//         innan s.
		// Eftir:  b er satt þþaa staðsetningin pos vísi
		//         á byrjun hlutstrengs sem er í málinu
		//         sem c skilgreinir.
		public boolean match( String s, int pos )
		{
			Matcher m = pat.matcher(s).region(pos,s.length());
			boolean res = m.lookingAt();
			if( res ) end = m.end();
			return res;
		}

		// Notkun: int i = c.end();
		// Fyrir:  Búið er að kalla c.match(s,pos) og fá
		//         sanna niðurstöðu.
		// Eftir:  i inniheldur staðsetningu innan s sem
		//         vísar á næsta staf á eftir þeim streng
		//         sem borið var kennsl á í match kallinu.
		public int end()
		{
			return end;
		}
	}

	// Hlutur af tagi Lexer er lesgreinirinn í þessum þýðanda.
	// Þetta er ákaflega frumstæður lesgreinir sem getur
	// lesgreint allt að 100000 stafa inntaksskrár og notar
	// frekar lélega aðferð til þess.
	static class Lexer
	{
		final LexCase[]
			cases =
				{ new LexCase("\\G\\(",'(')
				, new LexCase("\\G\\)",')')
				, new LexCase("\\G(\\s|(;.*$)|\\r|\\n)",'C')
				, new LexCase("\\G\\d+(\\.(\\d)+([eE][+\\-]?\\d+)?)?",'L')
				, new LexCase("\\G\\\"([^\\\\\"]|\\t|\\n|\\r|\\f)*\\\"",'L')
				, new LexCase("\\G\\\'([^\\\\\']|\\t|\\n|\\r|\\f)\\\'",'L')
				, new LexCase("\\G(\\p{Alpha}|[+\\-\\.*/<=>!\\?:$%_&~^0-9])+",'N')
				, new LexCase(".",'?')
				};
		final String input;
		int i;
		char token;
		String lexeme;
		// Fastayrðing gagna:
		//   input inniheldur forritstextann sem verið er að þýða.
		//   i er staðsetning næsta ólesna stafs í forritstextanum.
		//   token er stafur sem stendur fyrir næsta tók sem ekki
		//   er búið að vinna úr.  Það samsvarar stafarunu rétt fyrir
		//   framan staðsetninguna i.  lexeme er strengur sem
		//   inniheldur þá stafi úr input sem samsvara token.
		//   cases inniheldur skilgreiningar á þeim málum sem
		//   lesgreinirinn gerir greinarmun á.  Hvert stak í
		//   cases skilgreinir annars vegar eitthvert mál (mengi
		//   strengja) sem borið er kennsl á og hins vegar staf
		//   sem er samsvarandi tók.  Merking tókanna er:
		//     '(':  Strengurinn "("
		//     ')':  Strengurinn ")"
		//     'C':  Strengur sem er athugasemd eða bilstafur,
		//           sem skal því ekki skila áfram til þáttarans
		//     'L':  Strengur sem stendur fyrir lesfasta, þ.e.
		//           tölufasti, strengfasti, staffasti eða einn
		//           af lesföstunum true, false eða null.
		//     'N':  Strengur sem er löglegt breytunafn eða nafn
		//           á falli.
		//     'I':  Lykilorðið if.
		//     'D':  Lykilorðið define.
		//     '$':  Skrárlok, þ.e. endir inntaksins.
		//   Athugið að nokkur breytunöfn eru þess eðlis að þegar
		//   lesgreinirinn skilar tóki fyrir þau þá á hann að
		//   segja að þau séu lesfastar.  Þetta eru breytunöfnin
		//   true, false og null.  Lesgreinirinn þarf því að
		//   athuga hvort um þessi sérstöku breytunöfn er að ræða
		//   eftir að í ljós hefur komið að lesgreindi strengurinn
		//   er breytunafn.  Sama gildir um lykilorðin if og define.

		// Notkun: Lexer l = new Lexer(r);
		// Fyrir:  r er Reader sem inniheldur allt að 100000
		//         stafi.
		// Eftir:  l vísar á nýjan lesgreini sem lesgreinir
		//         innihaldið í r.  Lesgreinirinn er í upphafi
		//         staðsettur á fremsta lesi (lexeme) í r.
		public Lexer( Reader r ) throws IOException
		{
			StringBuffer b = new StringBuffer();
			char[] buf = new char[100000];
			for(;;)
			{
				int n = r.read(buf);
				if( n == -1 ) break;
				b.append(buf,0,n);
			}
			input = b.toString();
			i = 0;
			advance();
		}

		// Notkun: char c = l.getToken();
		// Eftir:  c er tókið (token) sem stendur fyrir
		//         það mál sem næsta les í l flokkast í.
		char getToken()
		{
			return token;
		}

		// Notkun: String s = l.getLexeme();
		// Eftir:  s er næsta les í l.
		String getLexeme()
		{
			return lexeme;
		}

		// Notkun: l.advance();
		// Eftir:  l hefur færst áfram á næsta les í
		//         inntakinu (sem ekki er athugasemd).
		void advance()
		{
			for(;;)
			{
				if( i>=input.length() )
				{
					token = '$';
					lexeme = "EOF";
					return;
				}
				for( int k=0 ; k!=cases.length ; k++ )
				{
					Matcher m = cases[k].pat.matcher(input);
					if( m.find(i) )
					{
						int j = m.end();
						token = cases[k].token;
						if( token=='C' )
						{
							i = j;
							break;
						}
						lexeme = input.substring(i,j);
						i = j;
						if( token=='N' )
						{
							if( lexeme.equals("if") )
								token = 'I';
							else if( lexeme.equals("define") )
								token = 'D';
							else if( lexeme.equals("null") )
								token = 'L';
							else if( lexeme.equals("true") )
								token = 'L';
							else if( lexeme.equals("false") )
								token = 'L';
						}
						return;
					}
				}
			}
		}

		// Notkun: String n = Lexer.tokenName(c);
		// Fyrir:  c er stafur sem er einn þeirra sem
		//         lexgreinirinn getur skilað sem tók.
		// Eftir:  n er nafnið á þessu tóki á mannlega
		//         læsilegu sniði.
		static String tokenName( char t )
		{
			switch( t )
			{
			case '(': return "(";
			case ')': return ")";
			case 'N': return "name";
			case 'L': return "literal";
			case 'D': return "define";
			case 'I': return "if";
			case '$': return "EOF";
			}
			return "?";
		}

		// Notkun: l.over(c);
		// Fyrir:  c er stafur sem stendur fyrir mögulegt tók.
		//         Næsta tók í l er c.
		// Eftir:  Búið er að færa lesgreininn eitt skref áfram
		//         eins og í advance().
		// Afbrigði:  Ef svo vill til að næsta tók í l er ekki
		//            c þá eru skrifuð villuboð og keyrslan stöðvast.
		String over( char tok )
		{
			if( token!=tok ) throw new Error("Expected "+tokenName(tok)+", found "+lexeme);
			String res = lexeme;
			advance();
			return res;
		}
	}

	// lex er lesgreinirinn.
	Lexer lex;
	// Inni í hverri fallsskilgreiningu inniheldur vars nöfnin
	// á viðföngunum í fallið (þ.e. leppunum eða breytunöfnunum
	// sem standa fyrir viðföngin), í sætum 1 og aftar.  Sæti
	// 0 inniheldur nafn fallsins sem verið er að skilgreina.
	String[] vars;

	// Notkun: NanoLisp n = new NanoLisp(l);
	// Fyrir:  l er lesgreinir.
	// Eftir:  n vísar á nýjan NanoLisp þýðanda sem þýðir inntakið
	//         sem l hefur.
	public NanoLisp( Lexer lexer )
	{
		lex = lexer;
	}

	// Notkun: int i = n.varPos(name);
	// Fyrir:  n er NanoLisp þýðandi og er að þýða stofn einhvers
	//         falls.  name er nafnið á einhverju viðfangi í fallið.
	// Eftir:  i er staðsetning viðfangsins í viðfangarunu fallsins
	//         þar sem fyrsta viðfang er talið vera í sæti 0.
	int varPos( String name )
	{
		for( int i=1 ; i!=vars.length ; i++ )
			if( vars[i].equals(name) ) return i-1;
		throw new Error("Variable "+name+" is not defined");
	}

	// Notkun: Object[] code = n.program();
	// Fyrir:  n er NanoLisp þýðandi og inntakið er löglegt
	//         NanoLisp forrit.
	// Eftir:  Búið er að þýða forritið og code vísar á nýtt
	//         fylki sem inniheldur milliþulurnar fyrir öll
	//         föllin í forritinu.
	// Afbrigði: Ef forritið er ekki löglegt þá eru skrifuð
	//           villuboð og keyrslan stöðvuð.
	Object[] program()
	{
		Vector<Object> res = new Vector<Object>();
		while( lex.getToken() == '(' ) res.add(fundecl());
		return res.toArray();
	}

	// Notkun: Object[] fun = n.fundecl();
	// Fyrir:  n er NanoLisp þýðandi sem er staðsettur í
	//         byrjun fallsskilgreiningar.
	// Eftir:  Búið er að lesa fallsskilgreininguna og fun vísar
	//         á nýtt fylki sem er milliþulan fyrir fallið.
	//         Þýðandinn er nú að horfa á næsta tákn í inntakinu
	//         fyrir aftan fallsskilgreininguna.
	Object[] fundecl()
	{
		lex.over('(');
		lex.over('D');
		lex.over('(');
		Vector<String> args = new Vector<String>();
		args.add(lex.over('N'));
		while( lex.getToken()!=')' ) args.add(lex.over('N'));
		lex.over(')');
		vars = new String[args.size()];
		args.toArray(vars);
		Object[] res = new Object[]{vars[0],vars.length-1,expr()};
		vars = null;
		lex.over(')');
		return res;
	}

	// Notkun: Object[] e = n.expr();
	// Fyrir:  n er NanoLisp þýðandi sem er staðsettur í
	//         byrjun segðar innan fallsskilgreiningar.
	// Eftir:  Búið er að lesa segðina og þýða hana.
	//         e vísar á milliþuluna fyrir segðina.
	//         Þýðandinn er nú að horfa á næsta tákn í inntakinu
	//         fyrir aftan segðina.
	Object[] expr()
	{
		Object[] res;
		switch( lex.getToken() )
		{
		case 'L':
			res = new Object[]{CodeType.LITERAL,lex.getLexeme()};
			lex.advance();
			return res;
		case 'N':
			res = new Object[]{CodeType.NAME,varPos(lex.getLexeme())};
			lex.advance();
			return res;
		case '(':
			lex.advance();
			switch( lex.getToken() )
			{
			case 'N':
				String name = lex.over('N');
				Vector<Object> args = new Vector<Object>();
				while( lex.getToken()!=')' ) args.add(expr());
				lex.advance();
				return new Object[]{CodeType.CALL,name,args.toArray()};
			case 'I':
				Object cond,thenexpr,elseexpr;
				lex.advance();
				cond = expr();
				thenexpr = expr();
				elseexpr = expr();
				lex.over(')');
				return new Object[]{CodeType.IF,cond,thenexpr,elseexpr};
			}
			throw new Error("Expected a name or the keyword 'if', found "+lex.getLexeme());
		default:
			throw new Error("Expected an expression, found "+lex.getLexeme());
		}
	}

	// Notkun: emit(line);
	// Fyrir:  line er lína í lokaþulu.
	// Eftir:  Búið er að skrifa línuna á aðalúttak.
	static void emit( String line )
	{
		System.out.println(line);
	}

	// Notkun: generateProgram(name,p);
	// Fyrir:  name er strengur, p er fylki fallsskilgreininga,
	//         þ.e. fylki af milliþulum fyrir föll.
	// Eftir:  Búið er að skrifa lokaþulu fyrir forrit sem
	//         samanstendur af föllunum þ.a. name er nafn
	//         forritsins.
	static void generateProgram( String name, Object[] p )
	{
		emit("\""+name+".mexe\" = main in");
		emit("!{{");
		for( int i=0 ; i!=p.length ; i++ ) generateFunction((Object[])p[i]);
		emit("}}*BASIS;");
	}

	// Notkun: generateFunction(f);
	// Fyrir:  f er milliþula fyrir fall.
	// Eftir:  Búið er að skrifa lokaþulu fyrir fallið á
	//         aðalúttak.
	static void generateFunction( Object[] f )
	{
		// f = {fname,argcount,expr}
		String fname = (String)f[0];
		int count = (Integer)f[1];
		emit("#\""+fname+"[f"+count+"]\" =");
		emit("[");
		generateExprR((Object[])f[2]);
		emit("];");
	}

	static int nextLab = 1;

	// Notkun: int i = newLab();
	// Eftir:  i er jákvæð heiltala sem ekki hefur áður
	//         verið skilað úr þessu falli.  Tilgangurinn
	//         er að búa til nýtt merki (label), sem er
	//         ekki það sama og neitt annað merki.
	static int newLab()
	{
		return nextLab++;
	}

	// Notkun: generateExpr(e);
	// Fyrir:  e er milliþula fyrir segð.
	// Eftir:  Búið er að skrifa lokaþulu fyrir segðina
	//         á aðalúttak.  Lokaþulan reiknar gildi
	//         segðarinnar og skilur gildið eftir í
	//         gildinu ac.
	static void generateExpr( Object[] e )
	{
		switch( (CodeType)e[0] )
		{
		case NAME:
			// e = {NAME,name}
			emit("(Fetch "+e[1]+")");
			return;
		case LITERAL:
			// e = {LITERAL,literal}
			emit("(MakeVal "+(String)e[1]+")");
			return;
		case IF:
			// e = {IF,cond,then,else}
			int labElse = newLab();
			int labEnd = newLab();
			generateJump((Object[])e[1],0,labElse);
			generateExpr((Object[])e[2]);
			emit("(Go _"+labEnd+")");
			emit("_"+labElse+":");
			generateExpr((Object[])e[3]);
			emit("_"+labEnd+":");
			return;
		case CALL:
			// e = {CALL,name,args}
			Object[] args = (Object[])e[2];
			int i;
			for( i=0 ; i!=args.length ; i++ )
				if( i==0 )
					generateExpr((Object[])args[i]);
				else
					generateExprP((Object[])args[i]);
			emit("(Call #\""+e[1]+"[f"+i+"]\" "+i+")");
			return;
		}
	}

	// Notkun: generateJump(e,labTrue,labTrue);
	// Fyrir:  e er milliþula fyrir segð, labTrue og
	//         labFalse eru heiltölur sem standa fyrir
	//         merki eða eru núll.
	// Eftir:  Búið er að skrifa lokaþulu fyrir segðina
	//         á aðalúttak.  Lokaþulan veldur stökki til
	//         merkisins labTrue ef segðina skilar sönnu,
	//         annars stökki til labFalse.  Ef annað merkið
	//         er núll þá er það jafngilt merki sem er rétt
	//         fyrir aftan þulu segðarinnar.
	static void generateJump( Object[] e, int labTrue, int labFalse )
	{
		switch( (CodeType)e[0] )
		{
		case LITERAL:
			String literal = (String)e[1];
			if( literal.equals("false") || literal.equals("null") )
			{
				if( labFalse!=0 ) emit("(Go _"+labFalse+")");
				return;
			}
			if( labTrue!=0 ) emit("(Go _"+labTrue+")");
			return;
		default:
			generateExpr(e);
			if( labTrue!=0 ) emit("(GoTrue _"+labTrue+")");
			if( labFalse!=0 ) emit("(GoFalse _"+labFalse+")");
		}
	}

	// Notkun: generateJumpP(e,labTrue,labFalse);
	// Fyrir:  e er milliþula fyrir segð, labTrue og
	//         labFalse eru heiltölur sem standa fyrir
	//         merki eða eru núll.
	// Eftir:  Þetta kall býr til lokaþulu sem er jafngild
	//         þulunni sem köllin
	//            emit("(Push)");
	//            generateJump(e,labTrue,labFalse);
	//         framleiða.  Þulan er samt ekki endilega sú
	//         sama og þessi köll framleiða því tilgangurinn
	//         er að geta framleitt betri þulu.
	static void generateJumpP( Object[] e, int labTrue, int labFalse )
	{
		switch( (CodeType)e[0] )
		{
		case LITERAL:
			String literal = (String)e[1];
			emit("(Push)");
			if( literal.equals("false") || literal.equals("null") )
			{
				if( labFalse!=0 ) emit("(Go _"+labFalse+")");
				return;
			}
			if( labTrue!=0 ) emit("(Go _"+labTrue+")");
			return;
		default:
			generateExprP(e);
			if( labTrue!=0 ) emit("(GoTrue _"+labTrue+")");
			if( labFalse!=0 ) emit("(GoFalse _"+labFalse+")");
		}
	}

	// Notkun: generateExpr(e);
	// Fyrir:  e er milliþula fyrir segð.
	// Eftir:  Þetta kall býr til lokaþulu sem er jafngild
	//         þulunni sem köllin
	//            generateExpr(e);
	//            emit("(Return)");
	//         framleiða.  Þulan er samt ekki endilega sú
	//         sama og þessi köll framleiða því tilgangurinn
	//         er að geta framleitt betri þulu.
	static void generateExprR( Object[] e )
	{
		switch( (CodeType)e[0] )
		{
		case NAME:
			// e = {NAME,name}
			emit("(FetchR "+e[1]+")");
			return;
		case LITERAL:
			// e = {LITERAL,literal}
			emit("(MakeValR "+(String)e[1]+")");
			return;
		case IF:
			// e = {IF,cond,then,else}
			int labElse = newLab();
			generateJump((Object[])e[1],0,labElse);
			generateExprR((Object[])e[2]);
			emit("_"+labElse+":");
			generateExprR((Object[])e[3]);
			return;
		case CALL:
			// e = {CALL,name,args}
			Object[] args = (Object[])e[2];
			int i;
			for( i=0 ; i!=args.length ; i++ )
				if( i==0 )
					generateExpr((Object[])args[i]);
				else
					generateExprP((Object[])args[i]);
			emit("(CallR #\""+e[1]+"[f"+i+"]\" "+i+")");
			return;
		}
	}

	// Notkun: generateExprP(e);
	// Fyrir:  e er milliþula fyrir segð.
	// Eftir:  Þetta kall býr til lokaþulu sem er jafngild
	//         þulunni sem köllin
	//            emit("(Push)");
	//            generateExpr(e);
	//         framleiða.  Þulan er samt ekki endilega sú
	//         sama og þessi köll framleiða því tilgangurinn
	//         er að geta framleitt betri þulu.
	static void generateExprP( Object[] e )
	{
		switch( (CodeType)e[0] )
		{
		case NAME:
			// e = {NAME,name}
			emit("(FetchP "+e[1]+")");
			return;
		case LITERAL:
			// e = {LITERAL,literal}
			emit("(MakeValP "+(String)e[1]+")");
			return;
		case IF:
			// e = {IF,cond,then,else}
			int labElse = newLab();
			int labEnd = newLab();
			generateJumpP((Object[])e[1],0,labElse);
			generateExpr((Object[])e[2]);
			emit("(Go _"+labEnd+")");
			emit("_"+labElse+":");
			generateExpr((Object[])e[3]);
			emit("_"+labEnd+":");
			return;
		case CALL:
			// e = {CALL,name,args}
			Object[] args = (Object[])e[2];
			int i;
			for( i=0 ; i!=args.length ; i++ ) generateExprP((Object[])args[i]);
			if( i==0 ) emit("(Push)");
			emit("(Call #\""+e[1]+"[f"+i+"]\" "+i+")");
			return;
		}
	}

	// Notkun (af skipanalínu):
	//        java NanoLisp forrit.s > forrit.masm
	// Fyrir: Skráin forrit.s inniheldur löglegt NanoLisp
	//        forit.
	// Eftir: Búið er að þýða forritið og skrifa lokaþuluna
	//        í skrána forrit.masm.  Sé sú lokaþula þýdd með
	//        skipuninni
	//           morpho -c forrit.masm
	//        þá verður til keyrsluhæfa Morpho skráin forrit.mexe.
	public static void main( String[] args )
		throws IOException
	{
		Lexer lexer = new Lexer(new FileReader(args[0]));
		String name = args[0].substring(0,args[0].lastIndexOf('.'));
		NanoLisp parser = new NanoLisp(lexer);
		Object[] intermediate = parser.program();
		if( lexer.getToken()!='$' ) throw new Error("Expected EOF, found "+lexer.getLexeme());
		generateProgram(name,intermediate);
	}
}
