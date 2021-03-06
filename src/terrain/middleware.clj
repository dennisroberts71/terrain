(ns terrain.middleware
  (:require [clojure.string :as string]
            [terrain.auth.user-attributes :as user-attributes]))

(defn- add-context-path
  "Adds a context path to the start of a URI path if it's not present."
  [uri-path context-path]
  (if-not (re-find (re-pattern (str "^\\Q" context-path "\\E(?:/|$)")) uri-path)
    (str context-path uri-path)
    uri-path))

(defn wrap-context-path-adder
  "Middleware that adds a context path to the start of a URI path in a request if it's not present."
  [handler context-path]
  (fn [request]
    (handler (update request :uri add-context-path context-path))))

(defn wrap-query-param-remover
  "Middleware that removes query parameters."
  [handler query-param exceptions]
  (let [exception? (apply some-fn (map #(partial re-find %) exceptions))]
    (fn [request]
      (if (exception? (:uri request))
        (handler request)
        (-> request
            (update-in [:params] dissoc query-param (keyword query-param))
            (update-in [:query-params] dissoc query-param (keyword query-param))
            handler)))))

(defn wrap-fake-user
  "Middleware that configures fake authentication for development testing. If the fake user is falsey,
   the handler function is not wrapped at all."
  [handler fake-user]
  (if fake-user
    (fn [request]
      (binding [user-attributes/fake-user fake-user]
        (handler request)))
    handler))
