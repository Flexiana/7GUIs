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

(defn texts-on-field [field]
  (mapv #(.-innerText %) (.-children field)))


(ws/deftest ui-tests
  (let [*test-state (r/atom cells-start)]
    (u/with-mounted-component
      [cells-ui *test-state]
      (fn [comp]
        (let [tbody #(.getByTestId % "tbody")
              thead #(.getByTestId % "thead")
              cell (fn [comp id] (.getByTestId comp id))
              input (fn [comp id] (.getByTestId comp (str "input_:" id)))
              form (fn [comp id] (.getByTestId comp (str "form_:" id)))
              insert (fn [comp id value]
                       (u/double-click-element! (cell comp id))
                       (u/input-element! (input comp id) value)
                       (u/submit! (form comp id)))]
          (testing "Initial render"
            (is (= (into [""] (map str (range 0 100))) (mapv str/trim (texts-on-field (tbody comp)))))
            (is (= (into [""] (map char (range 65 91))) (str/split (first (texts-on-field (thead comp))) #"\t"))))
          (testing "Change value"
            (insert comp "A1" "Elephant")
            (is (= "Elephant" (.-innerText (cell comp "A1")))))
          (testing "Sum of 3 and 4 = 7"
            (insert comp "B1" "3")
            (insert comp "B2" "4")
            (insert comp "B3" "Add B1 and B2 =")
            (is (= "7" (.-innerText (cell comp "B3")))))
          (testing "Sum of Elephant and 4 is NaN"
            (insert comp "B1" "Elephant")
            (insert comp "B2" "4")
            (insert comp "B3" "Add B1 and B2 =")
            (is (= "NaN" (.-innerText (cell comp "B3")))))
          (testing "Updates field updates dependent field"
            (insert comp "B1" "3")
            (insert comp "B2" "4")
            (insert comp "B3" "Add B1 and B2 =")
            (is (= "7" (.-innerText (cell comp "B3"))))
            (insert comp "B1" "5")
            (is (= "9" (.-innerText (cell comp "B3"))))))))))




