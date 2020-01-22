(ns financeiro.saldo-aceitacao-test
  (:require
    ; diz para o jetty quais são as rotas disponíveis
    [midje.sweet :refer :all]
    [cheshire.core :as json]
    [financeiro.auxiliares :refer :all]
    [clj-http.client :as http]
    [financeiro.db :as db]))

; :aceitacao é o rótulo que o Midje vai identificar
; Para executar apenas o teste de aceitação, use: lein midje :filters :aceitacao
; Para executar todos os outros testes menos o de aceitação, use: lein midje :filters -aceitacao

;against-background é o setup do teste
; before :facts indica que a função iniciar-servidor deve
; ser aplicada antes de todos os facts
(against-background [(before :facts [(iniciar-servidor porta-padrao)
                                     (db/limpar)])
                     (after :facts (parar-servidor))]
                    (fact "O saldo inicial é 0" :aceitacao

                          ;;	json/parse-string	pode	receber	um	boolean	como	terceiro
                          ;;	argumento	e	caso	seja	_true_,	o	Cheshire	vai	utilizar
                          ;;	keywords	em	vez	de	String	como	chaves	no	mapa
                          (json/parse-string (conteudo "/saldo") true) => {:saldo 0})

                    (fact "O saldo é 10 quando a única transação é uma receita de 10" :aceitacao
                          (http/post (endereco-para "/transacoes")
                                     {:content-type :json
                                      :body         (json/generate-string {:valor 10
                                                                           :tipo  "receita"})})
                          (json/parse-string (conteudo "/saldo") true) => {:saldo 10})


                    (fact "O	saldo	é	1000	quando	criamos	duas	receitas	de	2000
									e	uma	despesa	da	3000" :aceitacao

                          (http/post (endereco-para "/transacoes") (receita 2000))
                          (http/post (endereco-para "/transacoes") (receita 2000))
                          (http/post (endereco-para "/transacoes") (despesa 3000))

                          (json/parse-string (conteudo "/saldo") true) => {:saldo 1000})

                    (fact "Rejeita	uma	transação	com	valor	negativo" :aceitacao
                          (let [resposta (http/post (endereco-para "/transacoes")
                                                    (receita -100))]
                            (:status resposta) => 422))

                    (fact "Rejeita	uma	transação	com	valor	que	não	é	um	número"
                          :aceitacao
                          (let [resposta (http/post (endereco-para "/transacoes")
                                                    (receita "mil"))]
                            (:status resposta) => 422))

                    (fact "Rejeita	uma	transação	sem	tipo" :aceitacao
                          (let [resposta (http/post (endereco-para "/transacoes")
                                                    (conteudo-como-json {:valor 70}))]
                            (:status resposta) => 422))

                    (fact "Rejeita	uma	transação	com	tipo	desconhecido" :aceitacao
                          (let [resposta (http/post (endereco-para "/transacoes")
                                                    (conteudo-como-json {:valor 70 :tipo  "investimento"}))]
                            (:status resposta) => 422)))


