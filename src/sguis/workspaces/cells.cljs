(ns sguis.workspaces.cells
  (:require [reagent.core :as r]))

(def *cells
  (r/atom {:cells [{:id 0 :x 1 :y 2}]}))

(def a->z
  (map char (range 65 91)))

(def table-lines
  (range 0 10))

(defn cells-ui [*cells]
  (letfn [(header-fn [c]
            ^{:key c}
            [:td {:style {:border "1px solid black"}} c])
          (coll-fn [l c]
            ^{:key (str l c)}
            [:td {:style    {:border "1px solid black"}
                  :on-click #(swap! *cells assoc :focused-cell  (keyword (str l c)))}])
          (row-fn [l]
            ^{:key l}
            [:tr
             ;; coll entries per line
             (concat [^{:key l}
                      [:td {:style {:border "1px solid black"}} l]]
                     (map (partial coll-fn l) a->z))])]
    [:table {:style {:border "1px solid black"}}
     [:thead (concat [^{:key :n} [:th]]
                     (map header-fn  a->z))]
     [:tbody (concat [^{:key :n} [:th]]
                     (map row-fn table-lines))]]))
