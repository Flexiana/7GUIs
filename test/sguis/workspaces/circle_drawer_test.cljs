(ns sguis.workspaces.circle-drawer-test
  (:require [sguis.workspaces.circle-drawer :refer [circles-ui
                                                    circles-start]]
            [cljs.test :as t
             :include-macros true
             :refer [is testing]]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [sguis.workspaces.test-utils :as u]))

(ws/deftest click-insert-circle-ui-test
  let [#_#_flight-selector                           #(.getByTestId % "flight-selector")
       #_#_go-flight-input                           #(.getByTestId % "go-flight")
       #_#_return-flight-input                       #(.getByTestId % "return-flight")
       #_#_book-btn                                  #(.getByText % "Book!")
       #_#_book-msg                                  #(.getByTestId % "book-msg")
       #_#_reset-btn                                 #(.getByText % "Reset!")
       *circles (r/atom circles-start)]
  (u/with-mounted-component [circles-ui *circles]
    (fn [comp]
      #_(testing "Can book in future."
          (u/change-element! (flight-selector comp) {:target {:value "return-flight"}})
          (u/input-element! (go-flight-input comp) {:target {:value (unparse-date tomorrow)}})
          (u/input-element! (return-flight-input comp) {:target {:value (unparse-date future)}})
          (u/click-element! (book-btn comp))
          (is (= (format-msg @*booker) (.-textContent (book-msg comp))))
          (u/click-element! (reset-btn comp))))))
