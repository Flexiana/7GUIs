(ns sguis.workspaces.cells-test
  (:require
   [cljs.test :as t
    :include-macros true
    :refer [is testing]]
   [nubank.workspaces.core :as ws]
   [sguis.workspaces.cells :refer [cells-start
                                   cells-ui]]
   [sguis.workspaces.test-utils :as u]
   [reagent.core :as r]
   [clojure.string :as str]))

(defn texts-on-field
  [field]
  (mapv #(.-innerText %) (.-children field)))

(ws/deftest ui-tests
  (let [*test-state (r/atom cells-start)]
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
            (is (= (flatten [[""] (map str (range 0 5)) "Add row"]) (mapv str/trim (texts-on-field (tbody comp)))))
            (is (= (flatten [[""] (map char (range 65 75)) "Add column"]) (str/split (first (texts-on-field (thead comp))) #"\t"))))

          (testing "Change value"
            (insert comp "A1" "Elephant")
            (is (= "Elephant" (.-innerText (cell comp "A1")))))

          (testing "Sum of 3 and 4 = 7"
            (insert comp "D1" "3")
            (insert comp "D2" "4")
            (insert comp "D3" "=add(D1,D2)")
            (is (= "7" (.-innerText (cell comp "D3")))))

          (testing "Sum of Elephant and 10 is Elephant10"
            (insert comp "B1" "Elephant")
            (insert comp "B2" "10")
            (insert comp "B3" "=add(B1,B2)")
            (is (= "Elephant10" (.-innerText (cell comp "B3")))))

          (testing "Cell update updates dependent cells"
            (insert comp "B1" "3")
            (insert comp "B2" "4")
            (insert comp "B3" "=add(B1,B2)")
            (insert comp "B4" "=mul(B1,B2)")
            (is (= "7" (.-innerText (cell comp "B3"))))
            (is (= "12" (.-innerText (cell comp "B4"))))
            (insert comp "B1" "5")
            (is (= "9" (.-innerText (cell comp "B3"))))
            (is (= "20" (.-innerText (cell comp "B4")))))

          (testing "Cell update updates deep dependent cells"
            (insert comp "B1" "3")
            (insert comp "B2" "=B1")
            (insert comp "B3" "=add(B1,B2)")
            (insert comp "B4" "=mul(B1,B2)")
            (is (= "6" (.-innerText (cell comp "B3"))))
            (is (= "9" (.-innerText (cell comp "B4"))))
            (insert comp "B1" "5")
            (is (= "10" (.-innerText (cell comp "B3"))))
            (is (= "25" (.-innerText (cell comp "B4")))))

          (testing "Circular dependency"
            (insert comp "B1" "=B1")
            (is (= "duplicated keys: :B1" (.-innerText (cell comp "B1")))))

          (testing "Deep circular dependency"
            (insert comp "B1" "=C1")
            (insert comp "C1" "=B1")
            (is (= "" (.-innerText (cell comp "B1"))))))))))
