(ns financeiro.filtros-aceitacao-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [cheshire.core :as json]
            [financeiro.auxiliares :refer :all]
            [clj-http.client :as http]
            [financeiro.db :as db]))

(def transacoes-aleatorias
  '({:valor 7.0M :tipo "despesa" :rotulos ["sorvete" "entretenimento"]}
    {:valor 88.0M :tipo "despesa" :rotulos ["livro" "educação"]}
    {:valor 106.0M :tipo "despesa" :rotulos ["curso" "educação"]}
    {:valor 8000.0M :tipo "receita" :rotulos ["salário"]}
    {:valor 9000.0M :tipo "receita"}))

(against-background [(before :facts [(iniciar-servidor porta-padrao)
                                     (db/limpar)])
                     (after :facts (parar-servidor))]

                    (fact "Não existem receitas" :aceitacao
                          (json/parse-string (conteudo "/receitas") true) => {:transacoes '()})

                    (fact "Não existem despesas" :aceitacao
                          (json/parse-string (conteudo "/despesas") true) => {:transacoes '()})

                    (fact "Não existem transacoes" :aceitacao
                          (json/parse-string (conteudo "/transacoes") true) => {:transacoes '()}))

; doseq percorre a coleção e a cada transação insere
; doseq altera a coleção
(against-background [(before :facts [(iniciar-servidor porta-padrao)
                                     (doseq [transacao transacoes-aleatorias] (db/registrar transacao))])
                     (after :facts [(db/limpar)
                                    (parar-servidor)])]

                    (fact "Existem	3	despesas" :aceitacao
                          (count (:transacoes (json/parse-string (conteudo "/despesas") true))) => 3)

                    (fact "Existem 2	receita" :aceitacao
                          (count (:transacoes (json/parse-string (conteudo "/receitas") true))) => 2)

                    (fact "Existem	5	transações" :aceitacao
                          (count (:transacoes (json/parse-string (conteudo "/transacoes") true))) => 5)

                    (fact "Existe	1	receita	com	rótulo	'salário'"
                          (count (:transacoes (json/parse-string
                                                (conteudo "/transacoes?rotulos=salário") true))) => 1)
                    (fact "Existem	2	despesas	com	rótulo	'livro'	ou	'curso'"
                          (count (:transacoes (json/parse-string
                                                (conteudo "/transacoes?rotulos=livro&rotulos=curso")
                                                true))) => 2)
                    (fact "Existem	2	despesas	com	rótulo	'educação'"
                          (count (:transacoes (json/parse-string (conteudo "/transacoes?rotulos=educação") true))) => 2))

;(against-background [(before :facts [(iniciar-servidor porta-padrao)
;                                     (doseq [transacao transacoes-aleatorias] (db/registrar transacao))])
;                     (after :facts [(db/limpar)
;                                    (parar-servidor)])]
;
;                    (fact "Passando rotulo vazio como filtro, deve retornar transações que não tem rótulo definido"
;                          (:transacoes (json/parse-string (conteudo "/transacoes?rotulo=") true)) => {:valor 9000.0M :tipo "receita"})
;                    )