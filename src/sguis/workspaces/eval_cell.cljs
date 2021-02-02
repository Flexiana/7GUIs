(ns sguis.workspaces.eval-cell
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [sci.core :refer [parse-next
                              eval-form
                              init]]
            [sguis.workspaces.validator :as valid]
            [cljs.test :as t
             :include-macros true
             :refer [are is testing]]
            [nubank.workspaces.core :as ws]
            [clojure.string :as str]
            [clojure.edn :as edn]))
(def sci-eval
  (partial eval-form (init {})))

(def kw->op
  {:add      '+
   :subtract '-
   :div      '/
   :mul      '*
   :mod      'mod
   :sum      '+
   :prod     '*})

(defn input->raw-ast [s]
  (cond (str/starts-with? s "=")             (if (= 3 (count (str/split s #"\s")))
                                               (let [[_eq op d] (str/split s #"\s" 3)
                                                     opkw       (kw->op (keyword op))]
                                                 `(~opkw ~@(map (fn [x]
                                                                  (if (symbol? x)
                                                                    (keyword x)
                                                                    x))
                                                                (edn/read-string d))))
                                               (let [[_eq d] (str/split s #"\s" 2)]
                                                 (keyword d)))
        (valid/numeric? (edn/read-string s)) (edn/read-string s)
        :else                                s))

(defn raw-ast->ast [env r]
  (cond (seq? r)     (->> r
                          (map (fn [v]
                                 (if (keyword? v)
                                   (get-in env [:cells v :output])
                                   v))))
        (keyword? r) (get-in env [:cells r :output])
        :else        r))

(ws/deftest input->ast-test
  (let [env {:cells {;;simple num
                     :A1 {:input   "1"
                          :raw-ast 1
                          :ast     1
                          :output  1}
                     ;; simple op
                     :A2 {:input   "= add (1,2)"
                          :raw-ast '(+ 1 2)
                          :ast     '(+ 1 2)
                          :output  3}
                     ;; simple text
                     :A3 {:input   "abc"
                          :raw-ast "abc"
                          :ast     "abc"
                          :output  "abc"}
                     ;; simple ref
                     :A4 {:input   "=A1"
                          :raw-ast :A1
                          :ast     1
                          :output  1}
                     ;; op with ref
                     :A5 {:input   "= add (A1,A2)"
                          :raw-ast '(+ :A1 :A2)
                          :ast     '(+ 1 2)
                          :output  3}}}]

    (is (= 1 (input->raw-ast "1")))
    (is (= 1 (raw-ast->ast env 1)))
    (is (= 1 (->> "1"
                  input->raw-ast
                  (raw-ast->ast env)
                  sci-eval)))

    (is (= :A1 (input->raw-ast "= A1")))
    (is (= 1 (raw-ast->ast env :A1)))
    (is (= 1 (->> "= A1"
                  input->raw-ast
                  (raw-ast->ast env)
                  sci-eval)))

    (is (= "abc" (input->raw-ast "abc")))
    (is (= "abc" (raw-ast->ast env "abc")))
    (is (= "abc" (->> "abc"
                      input->raw-ast
                      (raw-ast->ast env)
                      sci-eval)))


    (is (= '(+ 1 2) (input->raw-ast "= add (1,2)")))
    (is (= '(+ 1 2) (raw-ast->ast env '(+ 1 2))))
    (is (= 3 (->> "= add (1,2)"
                  input->raw-ast
                  (raw-ast->ast env)
                  sci-eval)))

    (is (= '(+ :A1 :A2) (input->raw-ast "= add (A1,A2)")))
    (is (= '(+ 1 3) (raw-ast->ast env '(+ :A1 :A2))))
    (is (= 4 (->> "= add (A1,A2)"
                  input->raw-ast
                  (raw-ast->ast env)
                  sci-eval)))

    (is (= '(+ 1 2) (raw-ast->ast env '(+ :A1 2))))
    (is (= 3 (->> "= add (A1,2)"
                  input->raw-ast
                  (raw-ast->ast env)
                  sci-eval)))))
