(ns sguis.workspaces.cells
  (:require [reagent.core :as r]))

(def *cells
  (r/atom {:cells [{:id 0 :x 1 :y 2}]}))

(def a->z
  (map char (range 65 91)))

(def table-lines
  (range 0 10))

(defn cells-ui [*cells]
  [:table {:style {:border "1px solid black"}}
   [:thead (concat [^{:key :n} [:th]]
                   (map (fn [c] ^{:key c} [:td c])  a->z))]
   [:tbody (concat [^{:key :n} [:th]]
                   (map (fn [l]
                          ^{:key l}
                          [:tr
                           ;; coll entries per line
                           (concat [^{:key l} [:td l]]
                                   (map (fn [c]
                                          ^{:key (str l c)}
                                          [:td {:on-click #(js/console.log (str l c))} c]) a->z))])
                        ;; lines
                        table-lines))]])
