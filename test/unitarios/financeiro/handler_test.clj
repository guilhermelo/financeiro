(ns financeiro.handler-test
  (:require
            ; biblioteca padrão de teste
            ; [clojure.test :refer :all]
            ; biblioteca de teste midje
            [midje.sweet :refer :all]
            ; biblioteca de mockar requisições do ring
            [ring.mock.request :as mock]
            [cheshire.core :as json]
            ; namespace que vai ser testado
            [financeiro.handler :refer :all]
            [financeiro.db :as db]))


(facts "Da um Hello World na rota raiz"
       (let [response (app (mock/request :get "/"))]
         (fact "O status da resposta é 200"
               (:status response) => 200)

         (fact "O Texto do corpo é Hello World"
               (:body response) => "Hello World")))

(facts "Rota inválida"
       (fact "Rota inválida retornando 404"
             (let [response (app (mock/request :get "/invalid"))]
               (:status response) => 404)))

(facts "Saldo inicial é 0"
       ; criando um mock
       (against-background [(json/generate-string {:saldo 0}) => "{\"saldo\":0}"
                            (db/saldo) => 0]

                           (let [response (app (mock/request :get "/saldo"))]

                             (fact "O formato é application/json"
                                   (get-in response [:headers "Content-Type"]) => "application/json; charset=utf-8")

                             (fact "O status é 200"
                                   (:status response) => 200)

                             (fact "O texto do corpo é um json com saldo como chave e o valor"
                                   (:body response) => "{\"saldo\":0}"))))

(facts "Registra uma receita no valor de 10"
       (against-background (db/registrar {:valor 10 :tipo "receita"}) => {:id 1 :valor 10 :tipo "receita"})
       (let [response (app
                        (-> (mock/request :post "/transacoes")
                            (mock/json-body {:valor 10 :tipo "receita"})))]

         (fact "O status da resposta é 201"
               (:status response) => 201)

         (fact "O texto do corpo é um JSON com conteúdo enviado e um id"
               (:body response) => "{\"id\":1,\"valor\":10,\"tipo\":\"receita\"}")))

(facts "Existe	rota	para	lidar	com	filtro	de	transação	por	tipo"
       (against-background [(db/transacoes-do-tipo "receita") =>	'({:id	1	:valor	2000	:tipo	"receita"})
                            (db/transacoes-do-tipo "despesa") =>	'({:id	2	:valor	89	:tipo	"despesa"})
                            (db/transacoes) =>	'({:id	1	:valor	2000	:tipo	"receita"}
                                                  {:id	2	:valor	89	:tipo	"despesa"})]

         (fact "Filtro	por	receita"
               (let	[response	(app	(mock/request	:get	"/receitas"))]
                     (:status	response)	=>	200
                     (:body	response)	=>	(json/generate-string {:transacoes	'({:id	1	:valor	2000	:tipo	"receita"})})))

         (fact "Filtro	por	despesa"
               (let	[response	(app	(mock/request	:get	"/despesas"))]
                     (:status	response)	=>	200
                     (:body	response)	=>	(json/generate-string {:transacoes	'({:id	2	:valor	89	:tipo	"despesa"})})))

         (fact "Sem	filtro"
               (let	[response	(app	(mock/request	:get	"/transacoes"))]
                     (:status	response)	=>	200
                     (:body	response)	=>	(json/generate-string {:transacoes	'({:id	1	:valor	2000	:tipo	"receita"}
                                                                {:id	2	:valor	89	:tipo	"despesa"})})))))

(facts "Filtra	transações	por	parâmetros	de	busca	na	URL"

       (def	livro	{:id	1	:valor	88	:tipo	"despesa" :rotulos	["livro" "educação"]})
       (def	curso	{:id	2	:valor	106	:tipo	"despesa" :rotulos	["curso" "educação"]})
       (def	salario	{:id	3	:valor	8000	:tipo	"receita" :rotulos	["salário"]})

       (against-background [(db/transacoes-com-filtro	{:rotulos	["livro" "curso"]}) =>	[livro	curso]
                            (db/transacoes-com-filtro	{:rotulos	"salário"}) =>	[salario]]

         (fact "Filtro	múltiplos	rótulos"
               (let	[response	(app	(mock/request :get "/transacoes?rotulos=livro&rotulos=curso"))]
                 (:status	response)	=>	200
                 (:body	response)	=>	(json/generate-string {:transacoes [livro	curso]})))

         (fact "Filtro	com	único	rótulo"
               (let	[response	(app	(mock/request :get"/transacoes?rotulos=salário"))]
                 (:status	response)	=>	200
                 (:body	response)	=>	(json/generate-string {:transacoes	[salario]})))))

(fact "Deletando uma transação"
      (let [response (app (mock/request :delete "/transacoes/1"))]
        (:status response) => 200
        (:body response) => (json/generate-string {:id 1 :tipo "receita"})))

;
;;(deftest test-app
;;  (testing "main route"
;;    (let [response (app (mock/request :get "/"))]
;;      (is (= (:status response) 200))
;;      (is (= (:body response) "Hello World"))))
;;
;;  (testing "not-found route"
;;    (let [response (app (mock/request :get "/invalid"))]
;;      (is (= (:status response) 404)))))
