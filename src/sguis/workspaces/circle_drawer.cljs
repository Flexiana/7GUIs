(ns sguis.workspaces.circle-drawer
  (:require [reagent.core :as r]))


(def circles-start
  {:slider-opened? nil
   :current-id     0
   :circles        []
   :selection      {}
   :history        []})

(def *circles
  (r/atom circles-start))

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

(defn undo-on-click! [*state circles _]
  (when-not (empty? circles)
    (swap! *state assoc :slider-opened? false)
    (swap! *state update :circles pop)
    (swap! *state update :history conj (last circles))))

(defn undo-button [{:keys [circles]} undo-on-click!]
  [:button {:on-click (partial undo-on-click! circles)} "Undo"])

(defn redo-on-click! [*state history _]
  (when-not (empty? history)
    (swap! *state assoc :slider-opened? false)
    (swap! *state update :circles conj (last history))
    (swap! *state update :history pop)))

(defn redo-button [{:keys [history]} redo-on-click!]
  [:button {:on-click (partial redo-on-click! history)} "Redo"])

(def radius-box-style
  {:position "absolute"
   :width "80%"
   :top "50%"
   :left "50%"
   :transform "translate(-50%,-50%)"
   :padding "1em"
   :text-align "center"
   :background-color "rgba(255,255,255,0.5)"})


(defn update-radius! [*state selection event]
  (when selection
    (swap! *state
           update :circles conj
           (assoc selection :r  (.. event
                                    -target
                                    -valueAsNumber)))))

(defn radius-slider [{:keys [x y] :as selected?} update-radius!]
  [:label (str "Changing circle at "
               "(" x ", " y ")")
   [:input {:type "range"
            :min  0
            :max  100
            :disabled (not selected?)
            :on-change (partial update-radius! selected?)}]])

(defn radius-box [{:keys [slider-opened? selection]} update-radius!]
    (when slider-opened?
      [:div.slider {:style radius-box-style}
       [radius-slider selection update-radius!]]))

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
                        (swap! *state assoc :selection selection))}])

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
                       (assoc :id current-id
                              :r 50))]
    (swap! *state update :circles conj circle-pos)
    (swap! *state assoc :selection circle-pos))
  (swap! *state assoc :slider-opened? false)
  (swap! *state update :current-id inc))

(defn svg-draw [{:keys [circles selection slider-opened? current-id]}
                open-slider!
                insert-circle!
                circle-draw!]
  [:svg {:style svg-style
         :on-context-menu (partial open-slider! selection slider-opened?)
         :on-click (partial insert-circle! current-id)}
   (->> circles
        last-circles-by-id
        (map (partial circle-draw! selection)))])

(defn circles-ui [*circles]
  [:div {:style {:padding    "1em"
                 :text-align "center"}}
   [:div {:style {:display         "flex"
                  :justify-content "space-around"}
          :width "800"}
    [undo-button @*circles (partial undo-on-click! *circles)]
    [redo-button @*circles (partial redo-on-click! *circles)]]
   [svg-draw @*circles
    (partial open-slider! *circles)
    (partial insert-circle! *circles)
    (partial circle-draw! *circles)]
   [radius-box @*circles (partial update-radius! *circles)]])
