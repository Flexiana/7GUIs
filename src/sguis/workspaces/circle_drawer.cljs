(ns sguis.workspaces.circle-drawer
  (:require [reagent.core :as r]
            [reagent.dom :as dom]))

(def *circles
  (r/atom {:modal-opened? nil
           :current-id    0
           :circles       []
           :selected?     {}
           :history       []}))

(defn increment-id! [*state]
  (swap! *state update
         :current-id inc))

(defn insert-circle! [*state {:keys [current-id]} click]
  (let [circle-pos (assoc click
                          :id current-id
                          :r 50)]
    (swap! *state update
           :circles conj circle-pos)
    (swap! *state assoc
           :selected? circle-pos))
  (increment-id! *state))

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
              [:td {:on-click #(swap! *state assoc :selected? line)}
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

(defn radius-slider [*state {:keys [selected?]} update-radius!]
  [:label "slider"
   [:input {:type "range"
            :min  0
            :max  100
            :disabled (not selected?)
            :on-change #(update-radius! *state
                                        selected?
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


(defn insert-input [*state {:keys [mouse-x
                                   mouse-y]}]
  [:<>
   [:label [:input {:type "number"
                    :on-change #(swap! *state assoc :mouse-x
                                       (.. % -target -valueAsNumber))}] "x"]
   [:label [:input {:type "number"
                    :on-change #(swap! *state assoc :mouse-y
                                       (.. % -target -valueAsNumber))}] "y"]
   [:button {:on-click #(when (and mouse-y mouse-x)
                          (insert-circle! *state @*state {:x mouse-x
                                                          :y mouse-y}))
             :on-context-menu (fn [event]
                                (swap! *state update :modal-opened? not)
                                (when event
                                  (.preventDefault event)))}
    "Insert"]])

(defn svg-draw [*state {:keys [circles
                               selected?]}]
  [:svg {:width "100%"
         :height "100%"
         :background-color "#eee"
         :on-click (fn [event]
                     (let [dim (-> ^js event
                                   .-target
                                   .getBoundingClientRect)
                           x-rel (.-clientX event)
                           y-rel (.-clientY event)]
                       (insert-circle! *state @*state {:x (- x-rel (.-left dim))
                                                       :y (- y-rel (.-top dim))})))}
   (let [circles-to-plot (->> circles
                              (map (juxt :id identity))
                              (into {})
                              vals)]
     (map (fn [{:keys [x y r] :as select}]
            [:circle {:cx x
                      :cy y
                      :r r
                      :stroke "black"
                      :stroke-width "1"
                      :fill (let [{selected-x :x
                                   selected-y :y} selected?]
                              (if (and (= x selected-x)
                                       (= y selected-y))
                                "red"
                                "white"))
                      :on-click (fn [event]
                                  (.stopPropagation event)
                                  (swap! *state assoc :selected? select))
                      :on-context-menu (fn [event]
                                         (swap! *state update :modal-opened? not)
                                         (when event
                                           (.preventDefault event)))}]) circles-to-plot))])

(defn circles-ui [*circles]
  [:div {:padding "1em"}
   [:div "HI!"]
   [:div
    [svg-draw *circles @*circles]
    [insert-input *circles @*circles]
    [undo-button *circles @*circles]
    [redo-button *circles @*circles]]
   [:div
    [circles-table *circles]
    [radius-modal *circles]]])
