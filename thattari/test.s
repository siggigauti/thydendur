; MiniLisp prófunarforrit.
; Höfundur: Snorri Agnarsson, janúar 2014.

; Notkun: (fibo n)
; Fyrir:  n er heiltala, n>=0
; Gildi:  n-ta Fibonacci talan
(define (fibo n)
  (if (< n 2)
      1
      (+ (fibo (- n 1)) (fibo (- n 2)))
  )
)

; Notkun: (main)
; Fyrir:  Ekkert
; Eftir:  Búið er að reikna og skrifa fibo(30)
(define (main)
  (writeln (++ "fibo(30)=" (fibo 30)))
)
