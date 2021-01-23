(ns sguis.workspaces.cells-test
  (:require [sguis.workspaces.cells :refer [eval-cell]]
            [cljs.test :as t
             :include-macros true
             :refer [are]]
            [nubank.workspaces.core :as ws]))

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
