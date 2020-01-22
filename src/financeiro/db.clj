(ns financeiro.db)

; o átomo será usado como banco de dados
(def registros (atom []))

(defn transacoes []
  @registros)

; declara um simbolo, nesse caso uma função, porém sem implementação
(defn registrar [transacao]
  (let [nova-transacao (merge transacao {:id (inc (count (transacoes)))})
        colecao-atualizada (swap! registros conj nova-transacao)]
    nova-transacao))

(defn teste [t]
  (prn t)
  (empty? t))

(defn deletar [transacao-id]

  (let [transacoes (transacoes)
        id (Integer/parseInt transacao-id)
        transacao-encontrada (filter #(= id (:id %)) transacoes)]

    (prn transacoes)

    (if transacao-encontrada
      (reset! registros (filter #(not= id (:id %)) transacoes))
      {})
    )
  )

(defn transacoes-do-tipo [tipo]
  (filter #(= tipo (:tipo %)) (transacoes)))

(defn	transacoes-com-filtro	[filtros]
  (let	[rotulos	(->>	(:rotulos	filtros)
                        ;;	^^^	a	macro	thread-last	está	de	volta
                        (conj	[])
                        (flatten)
                        (set))]
    (filter	#(some	rotulos	(:rotulos	%))	(transacoes))))


; reset substitui tudo o que estiver dentro do átomo pelo que for passado
; como argumento, nesse caso foi um vetor vazio: []
(defn limpar []
  (reset! registros []))


(defn- despesa? [transacao]
  (= (:tipo transacao) "despesa"))

(defn- calcular [acumulado transacao]
  (let [valor (:valor transacao)]
    (if (despesa? transacao)
      (- acumulado valor)
      (+ acumulado valor))))

(defn saldo []
  (reduce calcular 0 @registros))
