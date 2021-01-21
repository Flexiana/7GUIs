(ns sguis.workspaces.circle-drawer-test
  (:require [sguis.workspaces.circle-drawer :refer [circles-ui
                                                    circles-start]]
            [cljs.test :as t
             :include-macros true
             :refer [is testing]]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [sguis.workspaces.test-utils :as u]))

(defn relative-click [svg-comp {:keys [x y]}]
  (let [dim (.getBoundingClientRect svg-comp)]
    {:clientX (+ (.-left dim) x)
     :clientY (+ (.-top dim) y)}))

(ws/deftest click-insert-circle-ui-test
  (let [svg-drawer              #(.getByTestId % "svg-drawer")
        #_#_go-flight-input     #(.getByTestId % "go-flight")
        #_#_return-flight-input #(.getByTestId % "return-flight")
        #_#_book-btn            #(.getByText % "Book!")
        #_#_book-msg            #(.getByTestId % "book-msg")
        #_#_reset-btn           #(.getByText % "Reset!")
        *circles                (r/atom circles-start)]
    (u/with-mounted-component [circles-ui *circles]
      (fn [comp]
        (testing "Creating circles"
          (let [circle-pos0 {:x 200
                             :y 200}
                svg-comp    (svg-drawer comp)]
            (u/click-element! svg-comp (relative-click svg-comp circle-pos0))
            (is (= circle-pos0
                   (-> @*circles :circles first (select-keys [:x :y]))))))))))
