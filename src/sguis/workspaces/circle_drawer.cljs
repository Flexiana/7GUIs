(ns sguis.workspaces.circle-drawer
  "7GUIs circle drawer"
  (:require
    [reagent.core :as r]))

(def circles-start
  "init state"
  {:slider-opened? nil
   :current-id     0
   :circles        []
   :selection      {}
   :history        []})

(def svg-style
  {:margin           "auto"
   :width            "100%"
   :height           "600px"
   :padding          "1em"
   :text-align       "center"
   :display          "flex"
   :border           "1px solid black"
   :stroke           "#646464"
   :stroke-width     "1px"
   :stroke-dasharray 2.2
   :stroke-linejoin  "round"
   :background-color "#eee"})

(defn undo-on-click!
  "Undo action"
  [*state circles _]
  (when-not (empty? circles)
    (swap! *state #(-> %
                       (assoc :slider-opened? false)
                       (update :circles pop)
                       (update :history conj (last circles))))))

(defn undo-button
  "Undo button"
  [{:keys [circles]} undo-on-click!]
  [:div.panel-block.is-block
   [:button.button.is-primary {:on-click (partial undo-on-click! circles)} "Undo"]])

(defn redo-on-click!
  "Redo action"
  [*state history _]
  (when-not (empty? history)
    (swap! *state #(-> %
                       (assoc :slider-opened? false)
                       (update :circles conj (last history))
                       (update :history pop)))))

(defn redo-button
  "Redo button"
  [{:keys [history]} redo-on-click!]
  [:div.panel-block.is-block
   [:button.button.is-primary {:on-click (partial redo-on-click! history)} "Redo"]])

(def radius-box-style
  {:position "sticky"
   :width "80%"
   :height "10%"
   :top "50%"
   :left "50%"
   :transform "translate(-10%,-500%)"
   :padding "1em"
   :text-align "center"
   :overflow "hidden"
   :background-color "rgba(255,255,255,0.5)"})

(defn update-radius!
  "Update action"
  [*state selection event]
  (when selection
    (swap! *state
      update :circles conj
      (assoc selection :r  (.. event
                               -target
                               -valueAsNumber)))))

(defn radius-slider
  "Update slider"
  [{:keys [x y] :as selected?} update-radius!]
  [:label (str "Changing circle at "
               "(" x ", " y ")")
   [:input.slider.is-primary.is-circle
    {:data-testid "radius-slider"
     :type        "range"
     :min         0
     :max         100
     :disabled    (not selected?)
     :on-change   (partial update-radius! selected?)}]])

(defn last-circles-by-id
  "Get recently added circles"
  [circles]
  (->> circles
       (map (juxt :id identity))
       (into {})
       vals))

(defn select-fill
  "Coloring by selection"
  [{selected-x :x
    selected-y :y}
   {:keys [x y]}]
  (if (and (= x selected-x)
           (= y selected-y))
    "red"
    "white"))

(defn circle-draw!
  "Visual representation of a circle"
  [*state selected? {:keys [id x y r] :as selection}]
  ^{:key id}
  [:circle {:data-testid  (str "circle-" id)
            :cx           x
            :cy           y
            :r            r
            :stroke       "black"
            :stroke-width "1"
            :fill         (select-fill selected? selection)
            :on-click     (fn [event]
                            (.stopPropagation event)
                            (swap! *state assoc :selection selection))}])

(defn open-slider!
  "Show or hide change diameter dialog"
  [*state selected? slider-opened? event]
  (if-not (and slider-opened? (not-empty selected?))
    (swap! *state assoc :slider-opened? true)
    (swap! *state assoc :slider-opened? false))
  (when event
    (.preventDefault event)))

(defn get-circle-dim
  "Compute activated circle dimensions"
  [event]
  (let [dim   (-> ^js event
                  .-target
                  .getBoundingClientRect)
        x-rel (.-clientX event)
        y-rel (.-clientY event)]
    {:x (- x-rel (.-left dim))
     :y (- y-rel (.-top dim))}))

(defn insert-circle!
  "Create a new circle"
  [*state current-id event]
  (let [circle-pos (-> event
                       get-circle-dim
                       (assoc :id current-id
                         :r 50))]
    (swap! *state update :circles conj circle-pos)
    (swap! *state assoc :selection circle-pos))
  (swap! *state assoc :slider-opened? false)
  (swap! *state update :current-id inc))

#_:clj-kondo/ignore

(defn svg-draw
  "Draw circles onto canvas"
  [{:keys [circles selection current-id slider-opened?]}
   open-slider!
   insert-circle!
   circle-draw!
   update-radius!]
  [:<>
   [:svg {:data-testid     "svg-drawer"
          :style           svg-style
          :on-context-menu (partial open-slider! selection slider-opened?)
          :on-click        (partial insert-circle! current-id)}
    (for [circle (last-circles-by-id circles)]
      (circle-draw! selection circle))]
   (when (and slider-opened? (not-empty circles))
     [:div {:style radius-box-style}
      [radius-slider selection update-radius!]])])

(defn circles-ui
  "Main UI of circles"
  ([]
   (r/with-let [*circles (r/atom circles-start)]
     [circles-ui *circles]))
  ([*circles]
   [:div
    {:style {:min-width "24em"}}
    [:div.panel-heading {:style {:background-color "#00d1b2"}} "🔴 Drawer"]
    [:div.panel-block.is-justify-content-space-evenly
     [undo-button @*circles (partial undo-on-click! *circles)]
     [redo-button @*circles (partial redo-on-click! *circles)]]
    [svg-draw @*circles
     (partial open-slider! *circles)
     (partial insert-circle! *circles)
     (partial circle-draw! *circles)
     (partial update-radius! *circles)]]))
