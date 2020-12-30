(ns sguis.workspaces.circle-drawer
  (:require [reagent.core :as r]
            [reagent.dom :as dom]))

(def *circles
  (r/atom {:modal-opened? nil
           :drawing       nil
           :current-id    0
           :circles       []}))

(defn insert-circle! [*state circle-pos]
  (swap! *state update
         :circles conj circle-pos)
  (swap! *state update
         :current-id inc))

(defn ui-draw-circles-on-canvas [*state mouse-event]
  (let [{:keys [canvas
                drawing
                current-id]} @*state
        rect                 (.getBoundingClientRect canvas)
        xrel                 (- (.-clientX mouse-event) (.-left rect))
        yrel                 (- (.-clientY mouse-event) (.-top rect))
        ctx                  (.getContext canvas "2d")]
    (if-not drawing
      (do (insert-circle! *state {:id current-id
                                  :x  xrel
                                  :y  yrel
                                  :r  50})
          (doto ctx
            (.beginPath)
            (.arc xrel yrel 50 0 (* 2 Math/PI))
            ;;    x  y  r  startangle endangle
            (.stroke))
          :draw)
      (do (swap! *state dissoc :drawing)
          (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))))))

(defn div-with-canvas [*state]
  (let [{:keys [window-widht
                canvas
                drawing]} @*state]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (swap! circles-state assoc :canvas (-> this
                                               dom/dom-node
                                               .-firstChild)))
      :reagent-render
      (fn [*state]
        [:div.with-canvas
         [:canvas {:style           {:border   "1px solid #000000"
                                     :position "relative"}
                   :on-click        (fn [mouse-event]
                                      (swap! circles-state
                                             assoc
                                             :drawing
                                             (ui-draw-circles-on-canvas circles-state mouse-event)))
                   :on-context-menu (fn [event]
                                      (swap! circles-state assoc :modal-opened? true)
                                      (when event
                                        (.preventDefault event)))}
          (when canvas
            {:width  (.-clientWidth canvas)
             :height (.-clientHeight canvas)})]])})))

(defn input-circles-ui [*state]
  (let [{:keys [modal-opened]} @*state]
    (if modal-opened
      [:div modal-opened]
      [:div])))

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
              [:td (get line attr)])
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

(defn radius-slider [*state])

(defn circles-ui [*circles]
  [:div {:padding "1em"}
   [:div "HI!"]
   [:div
    [:button "Undo"]
    [:button "Redo"]]
   [:div
    [div-with-canvas *circles]
    [circles-table *circles]]])
