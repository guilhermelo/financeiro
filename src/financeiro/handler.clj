(ns financeiro.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults
                                              api-defaults]]
    ; habililta transformar o corpo da requisição em JSON
            [ring.middleware.json :refer [wrap-json-body]]
            [cheshire.core :as json]
            [financeiro.db :as db]
            [financeiro.transacoes :as transacoes]))

; Para gerar o artefato do projeto: lein ring uberjar

(defn como-json [conteudo & [status]]
  {:status  (or status 200)
   :headers {"Content-Type"
             "application/json; charset=utf-8"}
   :body    (json/generate-string conteudo)})

(defroutes app-routes
           (GET "/" [] "Hello World")
           (GET "/saldo" [] (como-json {:saldo (db/saldo)}))
           (POST "/transacoes" requisicao
             (if (transacoes/valida? (:body requisicao))
               (-> (db/registrar (:body requisicao))
                   (como-json 201))
               (como-json {:mensagem "Requisição inválida"} 422)))

           (GET "/transacoes" {filtros :params}
             (como-json {:transacoes
                         (if (empty? filtros)
                           (db/transacoes)
                           (db/transacoes-com-filtro filtros))}))

           (DELETE "/transacoes/:id" [id]
             (como-json (db/deletar id)))

           (GET "/receitas" []
             (como-json {:transacoes (db/transacoes-do-tipo "receita")}))

           (GET "/despesas" []
             (como-json {:transacoes (db/transacoes-do-tipo "despesa")}))

           (route/not-found "Not Found"))

; wrap-defaults pega a definição de rotas (app-routes)
; e define algumas configurações

; site-defaults é um mapa que descreve configurações padrão
; como habilitar cookies, utf-8 como codificação padrão, etc
;
(def app
  (-> (wrap-defaults app-routes api-defaults)
      (wrap-json-body {:keywords? true :bigdecimals? true})))
