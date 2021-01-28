(ns sguis.workspaces.cells-test
  (:require
    [cljs.test :as t
     :include-macros true
     :refer [are is testing]]
    [clojure.string :as str]
    [nubank.workspaces.core :as ws]
    [reagent.core :as r]
    [sguis.workspaces.cells :refer [eval-cell
                                    cells-ui
                                    cells-start]]
    [sguis.workspaces.test-utils :as u]))

(ws/deftest eval-cell-test
  (are [expected actual] (= expected actual)
    "10"  (eval-cell {:cells {:A2 "2" :B8 "8"}
                      :columns      10
                      :rows         10} "Sum of A2:B8 =")
    "0"   (eval-cell {:columns      10
                      :rows         10} "Sum of A2:B8 =")
    "4"   (eval-cell {:columns      10
                      :rows         10} "Sum of 4 and A2:B8 =")
    "3"   (eval-cell {} "Add 1 and 2 =")
    "NaN" (eval-cell {:columns      10
                      :rows         10} "Div of B5 and C5 =")
    "a"   (eval-cell {} "a")
    "NaN" (eval-cell {:cells {:B1 "Elephant"
                              :B2 "10"}
                      :columns      10
                      :rows         10} "Sum of B1 and B2 =")
    "20"  (eval-cell {:cells {:A7 "10"
                              :G0 "10"}
                      :columns      10
                      :rows         10} "Add A7 and G0 =")
    ""    (eval-cell {} nil)))

(defn texts-on-field
  [field]
  (mapv #(.-innerText %) (.-children field)))

(ws/deftest ui-tests
  (let [*test-state (r/atom {:focused-cell nil
                             :edition      ""
                             :cells        {}
                             :columns      26
                             :rows         100})]
    (u/with-mounted-component
      [cells-ui *test-state]
      (fn [comp]
        (let [tbody  #(.getByTestId % "tbody")
              thead  #(.getByTestId % "thead")
              cell   (fn [comp id] (.getByTestId comp id))
              input  (fn [comp id] (.getByTestId comp (str "input-" id)))
              form   (fn [comp id] (.getByTestId comp (str "form-" id)))
              insert (fn [comp id value]
                       (u/double-click-element! (cell comp id))
                       (u/input-element! (input comp id) value)
                       (u/submit! (form comp id)))]
          (testing "Initial render"
            (is (= (flatten [[""] (map str (range 0 100)) "Add row"])     (mapv str/trim (texts-on-field (tbody comp)))))
            (is (= (flatten [[""] (map char (range 65 91)) "Add column"]) (str/split (first (texts-on-field (thead comp))) #"\t"))))
          (testing "Change value"
            (insert comp "A1" "Elephant")
            (is (= "Elephant" (.-innerText (cell comp "A1")))))
          (testing "Sum of 3 and 4 = 7"
            (insert comp "B1" "3")
            (insert comp "B2" "4")
            (insert comp "B3" "Add B1 and B2 =")
            (is (= "7" (.-innerText (cell comp "B3")))))
          (testing "Sum of Elephant and 10 is NaN"
            (insert comp "B1" "Elephant")
            (insert comp "B2" "10")
            (insert comp "B3" "Add B1 and B2 =")
            (is (= "NaN" (.-innerText (cell comp "B3")))))
          (testing "Cell update updates dependent cells"
            (insert comp "B1" "3")
            (insert comp "B2" "4")
            (insert comp "B3" "Add B1 and B2 =")
            (insert comp "B4" "Mul B1 and B2 =")
            (is (= "7" (.-innerText (cell comp "B3"))))
            (is (= "12" (.-innerText (cell comp "B4"))))
            (insert comp "B1" "5")
            (is (= "9" (.-innerText (cell comp "B3"))))
            (is (= "20" (.-innerText (cell comp "B4"))))))))))
