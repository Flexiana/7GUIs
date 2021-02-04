(ns sguis.workspaces.eval-cell
  (:require [clojure.string :as str]
            [sci.core :refer [eval-form]]
            [clojure.edn :as edn]
            [instaparse.core :refer [transform] :refer-macros [defparser]]))

(def kw->op
  {:add      '+
   :subtract '-
   :div      '/
   :mul      '*
   :mod      'mod
   :sum      '+
   :prod     '*})

#_:clj-kondo/ignore
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
  (cond (nil? input)       ""
        (str/blank? input) ""
        :else              (excel-like input)))

#_:clj-kondo/ignore
(defn input->raw-ast [input]
  (let [[err parsed-input] (try
                             [false (parse-input input)]
                             (catch :default ex
                               [ex]))]
    (if-not err
      (transform
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
        :formula identity}  parsed-input)
      err)))

(defn raw-ast->dependencies [raw-ast]
  (cond
    (coll? raw-ast)    (mapcat raw-ast->dependencies raw-ast)
    (keyword? raw-ast) [raw-ast]
    :else              nil))

(defn eval-sheets-raw-ast [{:keys [cells]
                            :as   env}]
  (reduce (fn [env cell-id]
            (if-let [input (get-in cells [cell-id :input])]
              (let [raw-ast (input->raw-ast input)
                    deps    (not-empty (raw-ast->dependencies raw-ast))
                    env-new (assoc-in env
                                      [:cells cell-id :raw-ast]
                                      raw-ast)]
              (if deps
                (assoc-in env-new [:cells cell-id :dependencies]
                          deps)
                env-new))
              env))
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
  (let [[err result] (try
                       [false (dependency-buildn (eval-sheets-raw-ast env) init-key)]
                       (catch :default ex
                       [ex]))]
    (if-not err
      (merge env {:eval-tree (dependency-buildn (eval-sheets-raw-ast env) init-key)})
      (assoc-in env [:cells init-key :output] (ex-message err)))))

(defn get-data-rec [cells raw-ast]
  (letfn [(get-data-rec* [cells v]
            (cond (keyword? v) (-> cells
                                   (get v)
                                   :output)
                  (seq? v)     (map #(get-data-rec* cells %) v)
                  :else        v))]
    (map #(get-data-rec* cells %) raw-ast)))

(defn ast-element-evaluator [sci-ctx env raw-ast cells cell-id]
  (cond (keyword? raw-ast) (let [{:keys [output]} (get cells raw-ast)
                                 env-after        (assoc-in env [:cells cell-id :ast] output)
                                 current-output   (eval-form sci-ctx output)]
                             (assoc-in env-after [:cells cell-id :output]
                                       current-output))
        (seq? raw-ast)     (let [form   (get-data-rec cells raw-ast)
                                 output (eval-form sci-ctx form)]
                             (assoc-in env [:cells cell-id :output] output))
        :else              (let [output (eval-form sci-ctx raw-ast)]
                             (update-in env [:cells cell-id] assoc
                                        :ast raw-ast
                                        :output output))))
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
