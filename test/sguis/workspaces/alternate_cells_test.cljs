(ns sguis.workspaces.alternate-cells-test
  (:require
    [cljs.test :as t
     :include-macros true
     :refer [are is testing]]
    [nubank.workspaces.core :as ws]
    [sguis.workspaces.cells :refer [->render
                                    cells-ui]]
    [sguis.workspaces.test-utils :as u]
    [reagent.core :as r]
    [clojure.string :as str]))

(defn new-table
  [cells]
  {:rows    10
   :columns 10
   :chain #{}
   :cells   (into {} (for [[k v] cells] [k {:input v}]))})

(ws/deftest
  eval-cell-test
  (are [expected actual] (= expected actual)
    1 (->render (new-table {:A1 "1"}) "A1")
    4 (->render (new-table {:A1 "2"
                            :A2 "= Add(A1,2)"}) "A2")
    9 (->render (new-table {:A0 "= sum(A1:A9)"
                            :A1 "1"
                            :A2 "1"
                            :A3 "1"
                            :A4 "1"
                            :A5 "1"
                            :A6 "1"
                            :A7 "1"
                            :A8 "1"
                            :A9 "1"}) "A0")
    10 (->render (new-table {:A0 "1"
                             :A1 "A0"
                             :A2 "A1"
                             :A3 "A1"
                             :A4 "A1"
                             :A5 "A1"
                             :A6 "A1"
                             :A7 "A1"
                             :A8 "A1"
                             :A9 "A1"
                             :B0 "= sum (A0:A9)"}) "B0")
    "Cyclic dependency found A0" (->render (new-table {:A0 "A0"}) "A0")
    "" (->render (new-table {}) "Div of B5 and C5 =")
    "Elephant10" (->render (new-table {:B1 "Elephant"
                                       :B2 "10"
                                       :B3 "= sum (B1,B2)"}) "B3")
    "" (->render {} nil)
    1 (->render {:chain #{}
                 :cells {:A1 {:input "1"}}} "A1")
    "A" (->render {:chain #{}
                   :cells {:A1 {:input "A"}}} "A1")
    "" (->render {:chain #{}
                  :cells {:A2 {:input "A1"}}} "A1")
    "Cyclic dependency found A1" (->render {:chain #{}
                                            :cells {:A1 {:input "A1"}}} "A1")
    "2, 3" (->render {:chain #{}
                      :cells {:A1 {:input "A2:A3"}
                              :A2 {:input "2"}
                              :A3 {:input "3"}}} "A1")
    "Cyclic dependency found A1, Cyclic dependency found A3"
    (->render {:chain #{}
               :cells {:A1 {:input "A1:A3"}
                       :A2 {:input "2"}
                       :A3 {:input "A3"}}} "A1")
    "Cyclic dependency found A1" (->render {:chain #{}
                                            :cells {:A1 {:input "A1,A2"}
                                                    :A2 {:input "2"}}} "A1")
    11 (->render {:chain #{}
                  :cells {:A1 {:input "= Add (A2:A5)"}
                          :A3 {:input "5"}
                          :A2 {:input "6"}}} "A1")
    17 (->render {:chain #{}
                  :cells {:A1 {:input "= Add (A3,= mul (2,A2))"}
                          :A3 {:input "5"}
                          :A2 {:input "6"}}} "A1")
    19 (->render {:chain #{}
                  :cells {:A1 {:input "= Add (A3,= mul (2,= add (1,A2)))"}
                          :A3 {:input "5"}
                          :A2 {:input "6"}}} "A1")
    21 (->render {:chain #{}
                  :cells {:A1 {:input "= Add (A3,= mul (2,= add (A2,2)))"}
                          :A3 {:input "5"}
                          :A2 {:input "6"}}} "A1")
    16 (->render {:chain #{}
                  :cells {:A1 {:input "= Add (= add (A0,A3),= add (A3,A2))"}
                          :A3 {:input "5"}
                          :A2 {:input "6"}}} "A1")
    (* 2 5 (+ 2 5) (+ 5 6)) (->render {:chain #{}
                                       :cells {:A1 {:input "= mul (2,A3,= add (A0,A3),= add (A3,A2))"}
                                               :A3 {:input "5"}
                                               :A2 {:input "6"}
                                               :A0 {:input "2"}}} "A1")
    (* 2 5 (+ (* 2 2) 5) (+ 5 6)) (->render {:chain #{}
                                             :cells {:A1 {:input "= mul (2,A3,= add (=mul(A0,2),A3),= add (A3,A2))"}
                                                     :A3 {:input "5"}
                                                     :A2 {:input "6"}
                                                     :A0 {:input "2"}}} "A1")
    (* (+ (* (+ 2 6) 2) 5) 2) (->render {:chain #{}
                                         :cells {:A1 {:input "=mul (= add (= mul (= add (A0,A2),2),A3),2)"}
                                                 :A3 {:input "5"}
                                                 :A2 {:input "6"}
                                                 :A0 {:input "2"}}} "A1")
    "6, 5" (->render {:chain #{}
                      :cells {:A1 {:input "A3:A2"}
                              :A3 {:input "5"}
                              :A2 {:input "6"}}} "A1")
    5 (->render {:chain #{}
                 :cells {:A1 {:input "= add (A2,3)"}
                         :A2 {:input "2"}}} "A1")))

(defn texts-on-field
  [field]
  (mapv #(.-innerText %) (.-children field)))

(ws/deftest ui-tests
  (let [*test-state (r/atom {:focused-cell nil
                             :edition      ""
                             :cells        {}
                             :chain        #{}
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
            (is (= (flatten [[""] (map str (range 0 100)) "Add row"]) (mapv str/trim (texts-on-field (tbody comp)))))
            (is (= (flatten [[""] (map char (range 65 91)) "Add column"]) (str/split (first (texts-on-field (thead comp))) #"\t"))))
          (testing "Change value"
            (insert comp "A1" "Elephant")
            (is (= "Elephant" (.-innerText (cell comp "A1")))))
          (testing "Sum of 3 and 4 = 7"
            (insert comp "B1" "3")
            (insert comp "B2" "4")
            (insert comp "B3" "= Add (B1,B2)")
            (is (= "7" (.-innerText (cell comp "B3")))))
          (testing "Sum of Elephant and 10 is Elephant10"
            (insert comp "B1" "Elephant")
            (insert comp "B2" "10")
            (insert comp "B3" "= Add (B1,B2)")
            (is (= "Elephant10" (.-innerText (cell comp "B3")))))
          (testing "Multiple multiplication with range"
            (insert comp "B1" "2")
            (insert comp "B2" "10")
            (insert comp "B3" "3")
            (insert comp "B4" "= Mul (B1:B3)")
            (is (= "60" (.-innerText (cell comp "B4")))))
          (testing "Multiple multiplication with series"
            (insert comp "B1" "2")
            (insert comp "B2" "10")
            (insert comp "B3" "3")
            (insert comp "B4" "= Mul (B1,B2,B3)")
            (is (= "60" (.-innerText (cell comp "B4")))))
          (testing "Cell update updates dependent cells"
            (insert comp "B1" "3")
            (insert comp "B2" "4")
            (insert comp "B3" "= Add (B1,B2)")
            (insert comp "B4" "= Mul(B1,B2)")
            (is (= "7" (.-innerText (cell comp "B3"))))
            (is (= "12" (.-innerText (cell comp "B4"))))
            (insert comp "B1" "5")
            (is (= "9" (.-innerText (cell comp "B3"))))
            (is (= "20" (.-innerText (cell comp "B4")))))
          (testing "Cell update updates deep dependent cells"
            (insert comp "B1" "3")
            (insert comp "B2" "B1")
            (insert comp "B3" "= Add(B1,B2)")
            (insert comp "B4" "= Mul(B1,B2)")
            (is (= "6" (.-innerText (cell comp "B3"))))
            (is (= "9" (.-innerText (cell comp "B4"))))
            (insert comp "B1" "5")
            (is (= "10" (.-innerText (cell comp "B3"))))
            (is (= "25" (.-innerText (cell comp "B4")))))
          (testing "Circular dependency"
            (insert comp "B1" "B1")
            (is (= "Cyclic dependency found B1" (.-innerText (cell comp "B1")))))
          (testing "Deep circular dependency"
            (insert comp "B1" "A1")
            (insert comp "A1" "B1")
            (is (= "Cyclic dependency found A1" (.-innerText (cell comp "B1"))))))))))
