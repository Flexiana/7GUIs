(ns sguis.workspaces.eval-cell-test
  (:require
   [cljs.test :as t
    :include-macros true
    :refer [are is]]
   [nubank.workspaces.core :as ws]
   [sci.core :refer [init]]
   [sguis.workspaces.eval-cell :refer [range-cells-get
                                       input->raw-ast
                                       eval-sheets-raw-ast
                                       dependency-buildn
                                       add-eval-tree
                                       eval-cell
                                       get-data-rec]]))

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
  (let [env         {:sci-ctx (init {})
                     :cells   {:A0 {:input        "=add(B0,B1)",
                                    :raw-ast      '(+ :B0 :B1),
                                    :ast          '(+ nil nil),
                                    :output       0,
                                    :dependencies '(:B0 :B1)}
                               :B0 {:input "1"}
                               :B2 {:input "=add(B0,B2)"}}}
        env1        {:cells {:A1 {:input "=add(A3,mul(2,A2))"}
                             :A3 {:input "5"}
                             :A2 {:input "6"}}}
        env-reverse {:cells {:A0 {:input        "=mul(B0,B1)",
                                  :raw-ast      '(* :B0 :B1),
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
                                   :cells   {:A0 {:input "=add(B0,B1)"}
                                             :B0 {:input "1"}
                                             :B2 {:input "=add(B0,3)"}}}
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

(ws/deftest get-data-rec-test
  (let [cells   {:A1 {:input        "=add(A3,mul(2,A2))",
                      :raw-ast      '(+ :A3 (* 2 :A2)),
                      :dependencies (:A3 :A2)},
                 :A3 {:input "5", :raw-ast 5, :ast 5, :output 5},
                 :A2 {:input "6", :raw-ast 6, :ast 6, :output 6}}
        raw-ast '(+ :A3 (* 2 :A2))]
    (is (= '(+ 5 (* 2 6)) (get-data-rec cells raw-ast)))))

(ws/deftest nested-exp-eval-cell-test
  (let [env-composed {:sci-ctx (init {})
                      :cells   {:A1 {:input "=add(A3,mul(2,A2))"}
                                :A3 {:input "5"}
                                :A2 {:input "6"}}}]
    (is (= 17 (get-in (eval-cell env-composed :A1) [:cells :A1 :output])))))

(ws/deftest  update-updates-deep-dependent-cells
  (let [env {:sci-ctx (init {})
             :cells   {:B1 {:input "3"}
                       :B2 {:input "=B1"}
                       :B3 {:input "=add(B1,B2)"}}}]
    (is (= 6 (get-in (eval-cell env :B3) [:cells :B3 :output])))))
