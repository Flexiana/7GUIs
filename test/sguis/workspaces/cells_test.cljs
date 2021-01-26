(ns sguis.workspaces.cells-test
  (:require [sguis.workspaces.cells :refer [eval-cell
                                            cells-ui
                                            cells-start]]
            [cljs.test :as t
             :include-macros true
             :refer [are is testing]]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [sguis.workspaces.test-utils :as u]
            [clojure.string :as str]))

(ws/deftest eval-cell-test
  (are [expected actual] (= expected actual)
    "10" (eval-cell {:cells {:A2 "2" :B8 "8"}} "Sum of A2:B8 =")
    "0" (eval-cell {} "Sum of A2:B8 =")
    "4" (eval-cell {} "Sum of 4 and A2:B8 =")
    "3" (eval-cell {} "Add 1 and 2 =")
    "NaN" (eval-cell {} "Div of B5 and C5 =")
    "a" (eval-cell {} "a")
    "20" (eval-cell {:cells {:A7 "10"
                             :G0 "10"}} "Add A7 and G0 =")
    "" (eval-cell {} nil)))

(ws/deftest ui-tests
  (let [*test-state (r/atom cells-start)]
    (u/with-mounted-component
      [cells-ui *test-state]
      (fn [comp]
        (let [tbody #(.getByTestId % "tbody")
              thead #(.getByTestId % "thead")
              cell (fn [comp id] (.getByTestId comp id))
              input (fn [comp id] (.getByTestId comp (str "input_:" id)))
              form (fn [comp id] (.getByTestId comp (str "form_:" id)))]
          (testing "Initial render"
            (is (= (into [""] (map str (range 0 100))) (mapv str/trim (mapv #(.-innerText %) (.-children (tbody comp))))))
            (is (= (conj (map char (range 65 91)) "") (str/split (first (mapv #(.-innerText %) (.-children (thead comp)))) #"\t"))))
          (testing "Change value"
            (u/double-click-element! (cell comp "A1"))
            (u/input-element! (input comp "A1") "Elephant")
            (u/submit (form comp "A1"))
            (is (= "Elephant" (.-innerText (cell comp "A1"))))))))))
