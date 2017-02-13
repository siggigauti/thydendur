nanolisp: NanoLisp.class NanoLispLexer.class NanoLispParser.class NanoLispParserVal.class test.masm
NanoLisp.class: NanoLisp.java
	javac -source 1.6 -target 1.6 NanoLisp.java
NanoLispLexer.class NanoLispParser.class NanoLispParserVal.class: NanoLispLexer.java NanoLispParser.java NanoLispParserVal.java
	javac -source 1.6 -target 1.6 NanoLispLexer.java NanoLispLexer.java NanoLispParser.java NanoLispParserVal.java
NanoLispLexer.java: nanolisp.jflex
	java -jar jflex-1.6.0.jar nanolisp.jflex
NanoLispParser.java NanoLispParserVal.java: nanolisp.byaccj
	./byacc -J -Jclass=NanoLispParser nanolisp.byaccj
test.masm: NanoLisp.class test.s
	java NanoLisp test.s > test.masm
zip:
	rm -Rf ../nanolisp.zip nanolisp.zip
	cd .. ;\
	zip nanolisp	nanolisp/NanoLisp.java nanolisp/makefile nanolisp/nanolisp.byaccj \
			nanolisp/nanolisp.jflex nanolisp/test.s nanolisp/byacc.exe \
			nanolisp/fact.s \
			nanolisp/jflex-1.6.0.jar nanolisp/morpho.jar nanolisp/morpho \
			nanolisp/morpho.bat;\
	mv nanolisp.zip nanolisp
clean:
	rm -Rf *~ NanoLisp*.class NanoLispLexer.java NanoLispParser.java NanoLispParserVal.java test.masm fact.mexe
test: nanolisp test.s fact.s
	java NanoLisp test.s | java -jar morpho.jar -c
	java -jar morpho.jar test
	java NanoLispParser test.s | java -jar morpho.jar -c
	java -jar morpho.jar test
	java NanoLisp fact.s | java -jar morpho.jar -c
	java -jar morpho.jar fact
	java NanoLispParser fact.s | java -jar morpho.jar -c
	java -jar morpho.jar fact
