(ns sguis.workspaces.cells
  (:require [reagent.core :as r]))

(def *cells
  (r/atom {:focused-cell nil
           :edition      ""
           :cells        {}}))

(def a->z
  (map char (range 65 91)))

(def table-lines
  (range 0 10))

(defn header-fn [c]
  ^{:key c}
  [:td {:style {:border "1px solid black"}} c])

(defn coll-fn [focused-cell cells edition l c]
  (let [cell-id (keyword (str l c))]
    ^{:key cell-id}
    [:td {:style           {:border "1px solid black"}
          :on-double-click #(swap! *cells assoc :focused-cell cell-id)}
     [:div (when (= cell-id focused-cell)
             [:form {:id        cell-id
                     :on-submit #(do (.preventDefault %)
                                     (swap! *cells assoc-in [:cells cell-id] edition)
                                     (swap! *cells dissoc :focused-cell))}
              [:input {:type      "text"
                       :on-change #(swap! *cells assoc :edition (.. % -target -value))}]])
      (get cells cell-id)]]))

(defn row-fn [focused-cell cells edition l]
  ^{:key l}
  [:tr
   (concat [^{:key l}
            [:td {:style {:border "1px solid black"}} l]]
           (map (partial coll-fn focused-cell cells edition l) a->z))])

(defn cells-ui [*cells]
  (let [{:keys [focused-cell
                cells
                edition]} @*cells]
    [:table {:style {:border "1px solid black"}}
     [:thead (concat [^{:key :n} [:td]]
                     (map header-fn  a->z))]
     [:tbody (concat [^{:key :n} [:th]]
                     (map (partial row-fn focused-cell cells edition) table-lines))]]))
