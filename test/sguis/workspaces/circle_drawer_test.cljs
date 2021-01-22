(ns sguis.workspaces.circle-drawer-test
  (:require [sguis.workspaces.circle-drawer :refer [circles-ui
                                                    circles-start]]
            [cljs.test :as t
             :include-macros true
             :refer [is testing]]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [sguis.workspaces.test-utils :as u]
            ["@testing-library/react" :as rtl]))

(defn relative-click [svg-comp {:keys [x y]}]
  (let [dim (.getBoundingClientRect svg-comp)]
    {:clientX (+ (.-left dim) x)
     :clientY (+ (.-top dim) y)}))

(ws/deftest circle-ui-test
  (let [circle-pos0   {:id 0
                       :x  200
                       :y  200
                       :r  50}
        circle-pos1   {:id 1
                       :x  500
                       :y  500
                       :r  50}
        circle-pos2   {:id 2
                       :x  30
                       :y  30
                       :r  50}
        svg-drawer    #(.getByTestId % "svg-drawer")
        slider-label  #(.getByText % #"Changing circle at")
        radius-slider #(.getByTestId % "radius-slider")
        circle-0      #(.getByTestId % "circle-0")
        circle-2      #(.getByTestId % "circle-2")
        undo-btn      #(.getByText % "Undo")
        redo-btn      #(.getByText % "Redo")
        *circles      (r/atom circles-start)]
    (u/with-mounted-component [circles-ui *circles]
      (fn [comp]
        (let [svg-comp (svg-drawer comp)]
          (testing "Creating circles"
            (mapv #(u/click-element! svg-comp (relative-click svg-comp %)) [circle-pos0
                                                                            circle-pos1
                                                                            circle-pos2])
            (is (= [circle-pos0
                    circle-pos1
                    circle-pos2]
                   (->> @*circles :circles))))

          (testing "Change circle selection"
            (is (and (= circle-pos2
                        (-> @*circles :selection))))
            (u/click-element! (circle-0 comp))
            (is (= circle-pos0  (-> @*circles :selection)))
            (is (= "red"
                   (.. (circle-0 comp)
                       -attributes
                       -fill
                       -value)))
            (is (= "white"
                   (.. (circle-2 comp)
                       -attributes
                       -fill
                       -value))))


          (testing "Opening Radius-Box"
            (u/click-context-menu! svg-comp)
            (is (and (-> @*circles :slider-opened? true?)
                     (slider-label comp))))

          (testing "Changing radius from circle0"
            (let [r-test 100]
              (u/change-element! (radius-slider comp) {:target {:value r-test}})
              (is (= r-test (-> @*circles :circles last :r)))))

          (testing "Undo button"
            (u/click-element! (undo-btn comp))
            (u/click-element! (undo-btn comp))
            (is (= [(assoc circle-pos0 :r 100)
                    circle-pos2]
                   (-> @*circles :history)))
            (is (= [circle-pos0 circle-pos1]
                   (-> @*circles :circles))))

          (testing "Redo button"
            (u/click-element! (redo-btn comp))
            (is (= [(assoc circle-pos0 :r 100)]
                   (-> @*circles :history)))
            (is (= [circle-pos0
                    circle-pos1
                    circle-pos2] (-> @*circles :circles)))))))))
