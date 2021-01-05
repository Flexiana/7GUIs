(ns sguis.workspaces.cells
  (:require [reagent.core :as r]))

(def *cells
  (r/atom {:cells [{:id 0 :x 1 :y 2}]}))

(def a->z
  (map (comp (fn [c] [:td c]) char) (range 65 91)))

(defn cells-ui [*cells]
  (let [{:keys [cells]} @*cells
        columns         [{:attr  :x
                          :label "x"}
                         {:attr  :y
                          :label "y"}]]
    (letfn [(row-fn [line {:keys [attr]}]
              ^{:key attr}
              [:td {:on-click #(swap! *cells assoc :selected? line)}
               (get line attr)])
            (column-fn [columns {:keys [id] :as line}]
              [:tr
               [:td]
               [:td id]
               (map (partial row-fn line) columns)])]
      [:table
       (map (fn [[line coll]]
              [:<>
               [:thead (concat [[:th]] a->z)]
               [:tbody (concat [[:th]]
                               (map (fn [c]
                                      [:tr [:td c]
                                       [:td line]])
                                    ;; lines
                                    (range 0 10)))]]) {0 {:A 1 :B 2}})]

      #_[:table
         [:thead
          (concat
           [[:th] [:td "id"] [:td "a"] [:td "b"]])]
         [:tbody
          (map (partial column-fn columns) cells)]])))
