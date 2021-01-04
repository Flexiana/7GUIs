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

(defn undo-button [*state {:keys [circles]}]
  [:button {:on-click #(when-not (empty? circles)
                         (swap! *state update :circles pop)
                         (swap! *state update :history conj (last circles)))} "Undo"])

(defn redo-button [*state {:keys [history]}]
  [:button {:on-click #(when-not (empty? history)
                         (swap! *state update :circles conj (last history))
                         (swap! *state update :history pop))} "Redo"])

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

(def radius-modal-style
  {:position "absolute"
   :width "80%"
   :top "50%"
   :left "50%"
   :transform "translate(-50%,-50%)"
   :padding "1em"
   :text-align "center"
   :background-color "rgba(255,255,255,0.5)"})

(defn radius-modal [*state]
  (let [{:keys [modal-opened?]} @*state]
    (when modal-opened?
      [:div.slider {:style radius-modal-style}
       [radius-slider *circles @*circles update-radius!]])))

(defn svg-draw [*state {:keys [circles
                               selected?]}]
  [:svg {:width "100%"
         :height "100%"
         :background-color "#eee"
         :on-context-menu (fn [event]
                            (swap! *state update :modal-opened? not)
                            (when event
                              (.preventDefault event)))
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
                                  (swap! *state assoc :selected? select))}]) circles-to-plot))])



(defn circles-ui [*circles]
  [:div {:padding "1em"
         :position "absolute"
         :width "100%"
         :text-align "center"}
   [undo-button *circles @*circles]
   [redo-button *circles @*circles]
   [svg-draw *circles @*circles]
   [radius-modal *circles]])
