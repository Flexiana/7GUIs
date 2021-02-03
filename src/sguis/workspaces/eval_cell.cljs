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
            [clojure.edn :as edn]))

(def kw->op
  {:add      '+
   :subtract '-
   :div      '/
   :mul      '*
   :mod      'mod
   :sum      '+
   :prod     '*})

(defn input->raw-ast [input]
  (cond (nil? input)                             ""
        (str/starts-with? input "=")             (if (= 3 (count (str/split input #"\s")))
                                                   (let [[_eq op d] (str/split input #"\s" 3)
                                                         opkw       (kw->op (keyword op))]
                                                     `(~opkw ~@(map (fn [x]
                                                                      (if (symbol? x)
                                                                        (keyword x)
                                                                        x))
                                                                    (edn/read-string d))))
                                                   (let [[_eq d] (str/split input #"\s" 2)]
                                                     (keyword d)))
        (valid/numeric? (edn/read-string input)) (edn/read-string input)
        :else                                    input))

(defn raw-ast->dependencies [raw-ast]
  (cond
    (coll? raw-ast)    (mapcat raw-ast->dependencies raw-ast)
    (keyword? raw-ast) [raw-ast]
    :else              nil))

(defn raw-ast->ast [env r]
  (cond (seq? r)     (->> r
                          (map (fn [v]
                                 (if (keyword? v)
                                   (get-in env [:cells v :output])
                                   v))))
        (keyword? r) (get-in env [:cells r :output])
        :else        r))
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
    (distinct (next-deps-impl #{} [init-key]))))

(defn add-eval-tree [env init-key]
  (merge env {:eval-tree (dependency-buildn (eval-sheets-raw-ast env) init-key)}))

(defn eval-cell
  [env cell-id]
  (let [{:keys [sci-ctx eval-tree]
         :as   env-new} (-> env
                            eval-sheets-raw-ast
                            (add-eval-tree cell-id))
        rf              (fn [{:keys [cells]
                             :as   env} cell-id]
                          (let [{:keys [raw-ast]} (get cells cell-id)]
                            (cond (keyword? raw-ast) (let [{:keys [output]} (get cells raw-ast)
                                                           env-after        (assoc-in env [:cells cell-id :ast] output)
                                                           current-output   (eval-form sci-ctx output)]
                                                       (assoc-in env-after [:cells cell-id :output]
                                                                 current-output))
                                  (seq? raw-ast)     (let [form   (map (fn [data]
                                                                         (if (keyword? data)
                                                                           (-> cells (get data) :output)
                                                                           data))
                                                                       raw-ast)
                                                           output (eval-form sci-ctx form)]
                                                       (assoc-in env [:cells cell-id :output] output))
                                  :else              (let [output (eval-form sci-ctx raw-ast)]
                                                       (update-in env [:cells cell-id] assoc
                                                                  :ast raw-ast
                                                                  :output output)))))]
    (reduce rf env-new eval-tree)))

(ws/deftest input->ast-test
  (let [env {:sci-ctx (init {})
             :cells   {;;simple num
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

    (is (= :A1 (input->raw-ast "= A1")))
    (is (= 1 (raw-ast->ast env :A1)))

    (is (= "abc" (input->raw-ast "abc")))
    (is (= "abc" (raw-ast->ast env "abc")))


    (is (= '(+ 1 2) (input->raw-ast "= add (1,2)")))
    (is (= '(+ 1 2) (raw-ast->ast env '(+ 1 2))))

    (is (= '(+ :A1 :A2) (input->raw-ast "= add (A1,A2)")))
    (is (= '(+ 1 3) (raw-ast->ast env '(+ :A1 :A2))))

    (is (= '(+ 1 2) (raw-ast->ast env '(+ :A1 2))))))


(ws/deftest eval-sheets-raw-ast-test
  (let [env {:sci-ctx (init {})
             :cells   {:A0 {:input        "= add (B0,B1)",
                            :raw-ast      '(+ :B0 :B1),
                            :ast          '(+ nil nil),
                            :output       0,
                            :dependencies '(:B0 :B1)}
                       :B0 {:input "1"}
                       :B2 {:input "= add (B0,B2)"}}}]
    (is (= {:A0 {:input        "= add (B0,B1)",
                 :raw-ast      '(+ :B0 :B1),
                 :ast          '(+ nil nil),
                 :output       0,
                 :dependencies '(:B0 :B1)}
            :B0 {:input "1" :raw-ast 1}
            :B2 {:input        "= add (B0,B2)"
                 :raw-ast      '(+ :B0 :B2)
                 :dependencies '(:B0 :B2)}}
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
             :cells   {:A0 {:input        "= add (B0,B1)",
                            :raw-ast      '(+ :B0 :B1),
                            :ast          '(+ nil nil),
                            :output       0,
                            :dependencies '(:B0 :B1)}
                       :B0 {:input "1"}
                       :B2 {:input "= add (B0,B2)"}}}]
    (is (= "duplicated keys: :B2"
           (ex-message (try (dependency-buildn (eval-sheets-raw-ast env) :B2)
                            (catch :default ex
                              ex)))))))

(ws/deftest add-eval-tree-test
  (let [env                       {:sci-ctx (init {})
                                   :cells   {:A0 {:input "= add (B0,B1)",}
                                             :B0 {:input "1"}
                                             :B2 {:input "= add (B0,3)"}}                                                                                                                                                                                                                                                                                                                                                                                            }
        {:keys [eval-tree cells]} (add-eval-tree (eval-sheets-raw-ast env) :B2)]
    (is (= [:B0 :B2] eval-tree))
    (is (= {:A0 {:input        "= add (B0,B1)",
                 :raw-ast      '(+ :B0 :B1),
                 :dependencies '(:B0 :B1)},
            :B0 {:input "1", :raw-ast 1},
            :B2 {:input "= add (B0,3)", :raw-ast '(+ :B0 3), :dependencies '(:B0)}} cells))))

(ws/deftest eval-cell-test
  (let [env-simple-subs       {:sci-ctx (init {})
                               :cells   {:A0 {:input "= B0"}
                                         :B0 {:input "1"}}}
        evaluated-simple-subs (eval-cell env-simple-subs :A0)
        env-simple-op         {:sci-ctx (init {})
                               :cells   {:A0 {:input "= add (B0,B1)"}
                                         :B0 {:input "1"}
                                         :B1 {:input "10"}}}
        evaluated-simple-op   (eval-cell env-simple-op :A0)]
    (is (= 1 (get-in evaluated-simple-subs [:cells :A0 :output])))
    (is (= 11 (get-in evaluated-simple-op [:cells :A0 :output])))))
