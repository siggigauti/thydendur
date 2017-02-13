; Notkun: (main)
; Fyrir:  Ekkert
; Eftir:  Búið er að skrifa 100!
(define (main)
  (writeln (++ "100!=" (fact 100)))
)

; Notkun: (fact n)
; Fyrir:  n er heiltala, n>=0
; Gildi:  Heiltalan n! sem BigInteger
(define (fact n)
  (if (== n 0)
      (bigInteger 1)
      (* n (fact (- n 1)))
  )
)
