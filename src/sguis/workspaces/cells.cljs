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

(defn header-fn [chars]
  ^{:key chars}
  [:td {:style {:border "1px solid black"}} chars])

(defn focus-cell! [*state cell-id _]
  (swap! *state assoc :focused-cell cell-id))

(defn submit-cell! [*state cell-id edition event]
  (.preventDefault event)
  (swap! *state assoc-in [:cells cell-id] edition)
  (swap! *state dissoc :focused-cell))

(defn change-cell! [*state event]
  (swap! *state assoc :edition (.. event -target -value)))

(defn coll-fn [{:keys [focused-cell cells edition]}
               {:keys [focus-cell! submit-cell! change-cell!]} l c]
  (let [cell-id (keyword (str l c))]
    ^{:key cell-id}
    [:td {:style           {:border "1px solid black"}
          :on-double-click (partial focus-cell! cell-id)}
     [:div (if (= cell-id focused-cell)
             [:form {:id        cell-id
                     :on-submit (partial submit-cell! cell-id edition)}
              [:input {:type      "text"
                       :on-change change-cell!}]]
             (get cells cell-id))]]))

(defn row-fn [cells actions-map l]
  ^{:key l}
  [:tr
   (concat [^{:key l}
            [:td {:style {:border "1px solid black"}} l]]
           (map (partial coll-fn cells actions-map l) a->z))])

(defn cells-ui [*cells]
  [:table {:style {:border "1px solid black"}}
   [:thead [:tr (concat [^{:key :n} [:th]]
                        (map header-fn a->z))]]
   [:tbody  (concat [^{:key :n} [:tr]]
                    (map (partial row-fn @*cells
                                  {:focus-cell!  (partial focus-cell! *cells)
                                   :submit-cell! (partial submit-cell! *cells)
                                   :change-cell! (partial change-cell! *cells)}) table-lines))]])
