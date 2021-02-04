(ns sguis.workspaces.eval-cell
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [sci.core :refer [eval-form
                              init]]
            [sguis.workspaces.validator :as valid]
            [cljs.test :as t
             :include-macros true
             :refer [are is testing]]
            [nubank.workspaces.core :as ws]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [instaparse.core :as insta :refer-macros [defparser]]))

(def kw->op
  {:add      '+
   :subtract '-
   :div      '/
   :mul      '*
   :mod      'mod
   :sum      '+
   :prod     '*})


(defparser excel-like
  "
formula = decimal / textual / (<'='> expr)
expr    = range / cell / decimal / app
app     = ident <'('> (expr <','>)* expr <')'>
range   = cell <':'> cell
cell    = #'[A-Za-z]\\d+'
textual = #'[^=].*'
ident   = #'[a-zA-Z_]\\w*'
decimal = #'-?\\d+(\\.\\d*)?'
")

(defn exsplit [k]
  (let [[_ k v] (re-matches #"([A-Z]+)([0-9]+)" (name k))]
    [k
     (edn/read-string v)]))
(defn range-cells-get
  [[a b]]
  (let [[colla vala]      (exsplit a)
        [collb valb]      (exsplit b)
        [valmin valmax]   (sort [(int vala) (int valb)])
        [collmin collmax] (sort [colla collb])]
    (for [collv (range (.charCodeAt collmin) (inc (.charCodeAt collmax)))
          v     (range valmin (inc valmax))]
      (keyword (str (char collv) v)))))
(defn parse-input [input]
  (excel-like input))
(defn input->raw-ast [input]
  (insta/transform
   {:decimal edn/read-string
    :ident   keyword
    :textual identity
    :cell    keyword
    :range   (fn [& args]
               (range-cells-get args))
    :app     (fn [kw & args]
               (concat [(get kw->op kw)] (if (seq? (first args))
                                           (flatten args)
                                           args)))
    :expr    (fn [& args] (first args))
    :formula identity} (parse-input input)))

(defn raw-ast->dependencies [raw-ast]
  (cond
    (coll? raw-ast)    (mapcat raw-ast->dependencies raw-ast)
    (keyword? raw-ast) [raw-ast]
    :else              nil))

(defn eval-sheets-raw-ast [{:keys [cells]
                            :as   env}]
  (reduce (fn [env cell-id]
            (let [raw-ast (input->raw-ast (get-in cells [cell-id :input]))
                  deps    (not-empty (raw-ast->dependencies raw-ast))
                  env-new (assoc-in env
                                    [:cells cell-id :raw-ast]
                                    raw-ast)]
              (if deps
                (assoc-in env-new [:cells cell-id :dependencies]
                          deps)
                env-new)))
          env
          (keys cells)))

(defn dependency-buildn
  [{:keys [cells]} init-key]
  (letfn [(next-deps-impl [seen deps]
            (let [seen-next (into seen deps)]
              (concat (mapcat #(let [next (get-in cells [% :dependencies])]
                                 (when-let [dupe (some seen next)]
                                   (-> (str "duplicated keys: " dupe)
                                       (ex-info {:cognitect.anomalies/category :cognitect.anomalies/conflict
                                                 :dupe                         dupe
                                                 :seen                         seen})
                                       (throw)))
                                 (next-deps-impl seen-next next))
                              deps)
                      deps)))]
    (let [reverse-dependent (keep (fn [[k v]]
                                    (when (some #(= init-key %) (:dependencies v))
                                      k)) cells)]
      (concat (distinct (next-deps-impl #{} [init-key])) reverse-dependent))))

(defn add-eval-tree [env init-key]
  (merge env {:eval-tree (dependency-buildn (eval-sheets-raw-ast env) init-key)}))

(defn ast-element-evaluator [sci-ctx env raw-ast cells cell-id]
  (letfn [(get-data-rec [cells data]
            (tap> data)
            (cond (keyword? data) (-> cells (get data) :raw-ast)
                  :else           data))]
    (cond (keyword? raw-ast) (let [{:keys [output]} (get cells raw-ast)
                                   env-after        (assoc-in env [:cells cell-id :ast] output)
                                   current-output   (eval-form sci-ctx output)]
                               (assoc-in env-after [:cells cell-id :output]
                                         current-output))
          (seq? raw-ast)     (let [form   (map (fn [data]
                                                 (get-data-rec cells data))
                                               raw-ast)
                                   output (eval-form sci-ctx form)]
                               (assoc-in env [:cells cell-id :output] output))
          :else              (let [output (eval-form sci-ctx raw-ast)]
                               (update-in env [:cells cell-id] assoc
                                          :ast raw-ast
                                          :output output)))))
(defn eval-cell
  [env cell-id]
  (let [{:keys [sci-ctx eval-tree]
         :as   env-new} (-> env
                            eval-sheets-raw-ast
                            (add-eval-tree cell-id))
        rf              (fn [{:keys [cells]
                             :as   env} cell-id]
                          (let [{:keys [raw-ast]} (get cells cell-id)]
                            (ast-element-evaluator sci-ctx env raw-ast cells cell-id)))]
    (reduce rf env-new eval-tree)))

(ws/deftest range-cells-get-test
  (are [expected actual] (= expected (range-cells-get actual))
    '(:A0 :A1)                                      [:A0 :A1]
    '(:A0 :A1 :A2)                                  [:A0 :A2]
    '(:A0 :A1 :B0 :B1)                              [:B0 :A1]
    '(:B0 :B1 :B2 :B3 :B4 :B5 :B6 :B7 :B8 :B9 :B10) [:B0 :B10]))

(ws/deftest parse-input->raw-ast-test
  (are [expected actual] (= expected (input->raw-ast actual))
    1                                                 "1"
    :A1                                               "=A1"
    "abc"                                             "abc"
    '(+ 1 2)                                          "=add(1,2)"
    '(+ :A1 :A2)                                      "=add(A1,A2)"
    '(+ :A3 (* 2 :A2))                                "=add(A3,mul(2,A2))"
    '(+ :A0 :A1 :A2 :A3)                              "=add(A0:A3)"
    '(* :B0 :B1 :B2 :B3 :B4 :B5 :B6 :B7 :B8 :B9 :B10) "=mul(B0:B10)"
    '(* :B0 :B1 2)                                    "=mul(B0:B1,2)"
    '(+ :B0 :B1)                                      "=add(B0:B1)"))

(ws/deftest eval-sheets-raw-ast-test
  (let [env {:sci-ctx (init {})
             :cells   {:A0 {:input        "=add(B0:B1)",
                            :raw-ast      '(+ :B0 :B1),
                            :ast          '(+ nil nil),
                            :output       0,
                            :dependencies '(:B0 :B1)}
                       :B0 {:input "1"}
                       :B2 {:input "=add(B0:B1)"}}}]
    (is (= {:A0 {:input        "=add(B0:B1)",
                 :raw-ast      '(+ :B0 :B1),
                 :ast          '(+ nil nil),
                 :output       0,
                 :dependencies '(:B0 :B1)}
            :B0 {:input "1" :raw-ast 1}
            :B2 {:input        "=add(B0:B1)"
                 :raw-ast      '(+ :B0 :B1)
                 :dependencies '(:B0 :B1)}}
           (:cells (eval-sheets-raw-ast env))))))

(ws/deftest dependencies-builder
  (let [env0            {:cells {:A0 {:dependencies [:A1]}
                                 :A1 {}}}
        env1            {:cells {:A0 {:dependencies [:A1]}
                                 :A1 {:dependencies [:A2]}
                                 :A2 {:dependencies []}}}
        env2            {:cells {:A0 {:dependencies [:A1]}
                                 :A1 {:dependencies [:A2]}
                                 :A2 {:dependencies [:A3]}
                                 :A3 {:dependencies []}}}
        env3            {:cells {:A0 {:dependencies [:A1]}
                                 :A1 {:dependencies [:A2]}
                                 :A2 {:dependencies [:A3]}
                                 :A3 {:dependencies [:A4]}
                                 :A4 {}}}
        duplicated-deps {:cells {:A0 {:dependencies [:A1 :A2]}
                                 :A1 {:dependencies [:A2]}
                                 :A2 {}}}
        nested-deps     {:cells {:A0 {:dependencies '(:B0 :B1)}
                                 :B0 {}
                                 :B1 {}
                                 :B2 {:dependencies '(:B0 :A0)}}}]
    (is (= [:A1 :A0] (dependency-buildn env0 :A0)))
    (is (= [:A2 :A1 :A0] (dependency-buildn env1 :A0)))
    (is (= [:A3 :A2 :A1 :A0] (dependency-buildn env2 :A0)))
    (is (= [:A4 :A3 :A2 :A1 :A0] (dependency-buildn env3 :A0)))
    (is (= [:A2 :A1 :A0] (dependency-buildn duplicated-deps :A0)))
    (is (= [:B0 :B1 :A0 :B2] (dependency-buildn nested-deps :B2)))))

(ws/deftest fix-loop-deps
  (let [looping-deps0 {:cells {:A0 {:dependencies [:A0]}}}
        looping-deps1 {:cells {:A0 {:dependencies [:A1]}
                               :A1 {:dependencies [:A2 :A0]}
                               :A2 {:dependencies []}}}]
    (is (= "duplicated keys: :A0" (ex-message
                                   (try (dependency-buildn looping-deps0 :A0)
                                        (catch :default ex
                                          ex)))))
    (is (= "duplicated keys: :A0" (ex-message
                                   (try (dependency-buildn looping-deps1 :A0)
                                        (catch :default ex
                                          ex)))))))

(ws/deftest dependencies-builder-from-sheets
  (let [env {:sci-ctx (init {})
             :cells   {:A0 {:input        "=add(B0,B1)",
                            :raw-ast      '(+ :B0 :B1),
                            :ast          '(+ nil nil),
                            :output       0,
                            :dependencies '(:B0 :B1)}
                       :B0 {:input "1"}
                       :B2 {:input "=add(B0,B2)"}}}
        env1  {:cells   {:A1 {:input "=add(A3,mul(2,A2))"}
                         :A3 {:input "5"}
                         :A2 {:input "6"}}}
        env-reverse  {:cells {:A0 {:input "=mul(B0,B1)",
                                   :raw-ast '(* :B0 :B1),
                                   :dependencies (:B0 :B1)},
                              :B0 {:input "8", :raw-ast 5},
                              :B1 {:input "10", :raw-ast 10}}}]
    (is (= "duplicated keys: :B2"
           (ex-message (try (dependency-buildn (eval-sheets-raw-ast env) :B2)
                            (catch :default ex
                              ex)))))
    (is (= '(:A3 :A2 :A1) (dependency-buildn (eval-sheets-raw-ast env1) :A1)))
    (is (= '(:A3 :A1) (dependency-buildn (eval-sheets-raw-ast env1) :A3)))
    (is (= '(:B0 :A0) (dependency-buildn (eval-sheets-raw-ast env-reverse) :B0)))))

(ws/deftest add-eval-tree-test
  (let [env                       {:sci-ctx (init {})
                                   :cells   {:A0 {:input "=add(B0,B1)",}
                                             :B0 {:input "1"}
                                             :B2 {:input "=add(B0,3)"}}                                                                                                                                                                                                                                                                                                                                                                                            }
        {:keys [eval-tree cells]} (add-eval-tree (eval-sheets-raw-ast env) :B2)]
    (is (= [:B0 :B2] eval-tree))
    (is (= {:A0 {:input        "=add(B0,B1)",
                 :raw-ast      '(+ :B0 :B1),
                 :dependencies '(:B0 :B1)},
            :B0 {:input "1", :raw-ast 1},
            :B2 {:input "=add(B0,3)", :raw-ast '(+ :B0 3), :dependencies '(:B0)}} cells))))

(ws/deftest eval-cell-test
  (let [env-simple-subs {:sci-ctx (init {})
                         :cells   {:A0 {:input "=B0"}
                                   :B0 {:input "1"}}}
        env-simple-op   {:sci-ctx (init {})
                         :cells   {:A0 {:input "=add(B0,B1)"}
                                   :B0 {:input "1"}
                                   :B1 {:input "10"}}}
        env-mul         {:sci-ctx (init {})
                         :cells   {:A0 {:input "=mul(B0,B1)"}
                                   :B0 {:input "5"}
                                   :B1 {:input "10"}}}
        env-ranged-op   {:sci-ctx (init {})
                         :cells   {:A0 {:input        "=mul(B0:B1)"
                                        :raw-ast      '(* :B0 :B1)
                                        :dependencies (:B0 :B1)}
                                   :B0 {:input "8"}
                                   :B1 {:input "10"}}}]
    (is (= 1 (get-in (eval-cell env-simple-subs :A0) [:cells :A0 :output])))
    (is (= 11 (get-in (eval-cell env-simple-op :A0) [:cells :A0 :output])))
    (is (= 50 (get-in (eval-cell env-mul :A0) [:cells :A0 :output])))
    (is (= 80 (get-in (eval-cell env-ranged-op :A0) [:cells :A0 :output])))))

(ws/deftest nested-exp-eval-cell-test
  (let [env-composed {:sci-ctx (init {})
                      :cells   {:A1 {:input "=add(A3,mul(2,A2))"}
                                :A3 {:input "5"}
                                :A2 {:input "6"}}}]
    (is (= 17 (get-in (eval-cell env-composed :A1) [:cells :A1 :output])))))
