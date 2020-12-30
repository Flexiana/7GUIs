(ns sguis.workspaces.circle-drawer
  (:require [reagent.core :as r]
            [reagent.dom :as dom]))

(def *circles
  (r/atom {:modal-opened? nil
           :drawing?      nil
           :current-id    0
           :circles       []
           :history       []}))

(defn increment-id! [*state]
  (swap! *state update
         :current-id inc))


(defn circle-pos [canvas {:keys [mouse-x mouse-y]}]
  (let [rect (.getBoundingClientRect canvas)]
    {:x (- mouse-x (.-left rect))
     :y (- mouse-y (.-top rect))}))

(defn insert-circle! [*state {:keys [canvas current-id]} click-on-canvas]
  (let [circle-pos (merge {:id current-id :r  50}
                          (circle-pos canvas click-on-canvas))]
    (swap! *state update
           :circles conj circle-pos))
  (increment-id! *state))

(defn div-draw [*state]
  (let [{:keys [window-widht
                canvas
                drawing]} @*state]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (swap! *state assoc :canvas (-> this
                                        dom/dom-node
                                        .-firstChild)))
      :reagent-render
      (fn [*state]
        [:div.with-canvas
         [:canvas {:style           {:border   "1px solid #000000"
                                     :position "relative"}
                   :on-click #(insert-circle! *state @*state {:mouse-x (.-clientX %)
                                                              :mouse-y (.-clientY %)})
                   :on-context-menu (fn [event]
                                      (swap! *state update :modal-opened? not)
                                      (when event
                                        (.preventDefault event)))}
          (when canvas
            {:width  (.-clientWidth canvas)
             :height (.-clientHeight canvas)})]])})))

(defn circles-table [*state]
  (let [{:keys [circles]} @*state
        columns           [{:attr  :x
                            :label "x"}
                           {:attr  :y
                            :label "y"}
                           {:attr  :r
                            :label "r"}]]

    (letfn [(row-fn [line {:keys [attr]}]
              ^{:key attr}
              [:td {:on-click #(swap! *state assoc :selection line)}
               (get line attr)])
            (column-fn [columns {:keys [id] :as line}]
              [:tr
               [:td]
               [:td id]
               (map (partial row-fn line) columns)])]
      [:table
       [:thead
        (concat
         [[:th] [:td "id"] [:td "x"] [:td "y"] [:td "r"]])]
       [:tbody
        (map (partial column-fn columns) circles)]])))

(defn update-radius! [*state selection new-radius]
  (when selection
    (swap! *state
           update :circles conj
           (assoc selection :r new-radius))))

(defn radius-slider [*state {:keys [selection]} update-radius!]
  [:label "slider"
   [:input {:type "range"
            :min  0
            :max  100
            :disabled (not selection)
            :on-change #(update-radius! *state
                                        selection
                                        (.. %
                                            -target
                                            -valueAsNumber))}]])

(defn radius-modal [*state]
  (let [{:keys [modal-opened?]} @*state]
    (if modal-opened?
      [radius-slider *circles @*circles update-radius!]
      [:div])))

(defn undo-button [*state {:keys [circles]}]
  [:button {:on-click #(when-not (empty? circles)
                         (swap! *state update :circles pop)
                         (swap! *state update :history conj (last circles)))} "Undo"])

(defn redo-button [*state {:keys [history]}]
  [:button {:on-click #(when-not (empty? history)
                         (swap! *state update :circles conj (last history))
                         (swap! *state update :history pop))} "Redo"])

(defn circles-ui [*circles]
  [:div {:padding "1em"}
   [:div "HI!"]
   [:div
    [undo-button *circles @*circles]
    [redo-button *circles @*circles]]
   [:div
    [div-draw *circles]
    [circles-table *circles]
    [radius-modal *circles]]])
