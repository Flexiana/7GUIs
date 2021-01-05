(ns sguis.workspaces.circle-drawer
  (:require [reagent.core :as r]))

(def *circles
  (r/atom {:slider-opened? nil
           :current-id    0
           :circles       []
           :selected?     {}
           :history       []}))

(def svg-style
  {:width "800"
   :height "600"
   :display "flex"
   :border "1px solid black"
   :stroke "#646464"
   :stroke-width "1px"
   :stroke-dasharray 2.2
   :stroke-linejoin "round"
   :background-color "#eee"})

(defn undo-button [{:keys [circles]} *state]
  [:button {:on-click #(when-not (empty? circles)
                         (swap! *state assoc :slider-opened? false)
                         (swap! *state update :circles pop)
                         (swap! *state update :history conj (last circles)))} "Undo"])

(defn redo-button [{:keys [history]} *state]
  [:button {:on-click #(when-not (empty? history)
                         (swap! *state assoc :slider-opened? false)
                         (swap! *state update :circles conj (last history))
                         (swap! *state update :history pop))} "Redo"])

(defn update-radius! [*state selection new-radius]
  (when selection
    (swap! *state
           update :circles conj
           (assoc selection :r new-radius))))

(defn radius-slider [*state {:keys [selected?]} update-radius!]
  [:label (str "Changing circle at "
               "("
               (:x selected?) ", "
               (:y selected?)")")
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
  (let [{:keys [slider-opened?]} @*state]
    (when slider-opened?
      [:div.slider {:style radius-modal-style}
       [radius-slider *circles @*circles update-radius!]])))


(defn last-circles-by-id [circles]
  (->> circles
       (map (juxt :id identity))
       (into {})
       vals))

(defn select-fill [{selected-x :x
                    selected-y :y}
                   {:keys [x y]}]
  (if (and (= x selected-x)
           (= y selected-y))
    "red"
    "white"))

(defn circle-draw! [*state selected? {:keys [id x y r] :as selection}]
  ^{:key id}
  [:circle {:cx x
            :cy y
            :r r
            :stroke "black"
            :stroke-width "1"
            :fill (select-fill selected? selection)
            :on-click (fn [event]
                        (.stopPropagation event)
                        (swap! *state assoc :selected? selection))}])

(defn open-slider! [*state selected? slider-opened? event]
  (if-not (and slider-opened? (not-empty selected?))
    (swap! *state assoc :slider-opened? true)
    (swap! *state assoc :slider-opened? false))
  (when event
    (.preventDefault event)))

(defn get-circle-dim [event]
  (let [dim   (-> ^js event
                .-target
                .getBoundingClientRect)
        x-rel (.-clientX event)
        y-rel (.-clientY event)]
    {:x (- x-rel (.-left dim))
     :y (- y-rel (.-top dim))}))

(defn insert-circle! [*state current-id event]
  (let [circle-pos (-> event
                       get-circle-dim
                       (assoc
                        :id current-id
                        :r 50))]
    (swap! *state update
           :circles conj circle-pos)
    (swap! *state assoc
           :selected? circle-pos))
  (swap! *state assoc :slider-opened? false)
  (swap! *state update
         :current-id inc))

(defn svg-draw [{:keys [circles
                        selected?
                        slider-opened?
                        current-id]}
                open-slider!
                insert-circle!
                circle-draw!]
  [:svg {:style svg-style
         :on-context-menu (partial open-slider! selected? slider-opened?)
         :on-click (partial insert-circle! current-id)}
   (->> circles
        last-circles-by-id
        (map (partial circle-draw! selected?)))])

(defn circles-ui [*circles]
  [:div {:style {:padding "1em"
                 :text-align "center"}}
   [:div {:style {:display "flex"
                  :justify-content "space-around"}
          :width "800"}
    [undo-button @*circles *circles]
    [redo-button @*circles *circles]]
   [svg-draw @*circles
    (partial open-slider! *circles)
    (partial insert-circle! *circles)
    (partial circle-draw! *circles)]
   [radius-modal *circles]])
