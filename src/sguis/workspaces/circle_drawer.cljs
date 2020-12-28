(ns sguis.workspaces.circle-drawer
  (:require [reagent.core :as r]
            [reagent.dom :as dom]))

(def *circles
  (r/atom {:window-width nil
           :drawing      nil
           :current-id   0
           :events       []}))

(defn insert-circle! [circles-state circle-pos]
  (swap! circles-state update
         :events conj circle-pos)
  (swap! circles-state update
         :current-id inc))

(defn ui-draw-circles-on-canvas [circles-state mouse-event]
  (let [{:keys [canvas
                drawing]} @circles-state
        rect              (.getBoundingClientRect canvas)
        xrel              (- (.-clientX mouse-event) (.-left rect))
        yrel              (- (.-clientY mouse-event) (.-top rect))
        ctx               (.getContext canvas "2d")]
    (if-not drawing
      (do (insert-circle! circles-state {:axis xrel
                                         :ayis yrel})
          (doto ctx
            (.beginPath)
            (.arc xrel yrel 50 0 (* 2 Math/PI))
            ;;    x  y  r  startangle endangle
            (.stroke))
          :draw)
      (do (swap! circles-state dissoc :drawing)
          (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))))))

(defn div-with-canvas [circles-state]
  (let [{:keys [window-widht
                canvas
                drawing]} @circles-state]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (swap! circles-state assoc :canvas (-> this
                                               dom/dom-node
                                               .-firstChild)))
      :reagent-render
      (fn []
        [:div.with-canvas
         [:canvas {:style           {:border   "1px solid #000000"
                                     :position "relative"}
                   :on-click        (fn [mouse-event]
                                      (swap! circles-state
                                             assoc
                                             :drawing
                                             (ui-draw-circles-on-canvas circles-state mouse-event)))
                   :on-context-menu (fn [event]
                                      (swap! circles-state assoc :modal-opened true)
                                      (when event
                                        (.preventDefault event)))}
          (when canvas
            {:width  (.-clientWidth canvas)
             :height (.-clientHeight canvas)})]])})))

(defn input-circles-ui [circles-state]
  (let [{:keys [modal-opened]} @circles-state]
    (if modal-opened
      [:div (with-out-str (cljs.pprint/pprint (str modal-opened)))]
      [:div])))

(defn circles-ui [circles-state]
  [:div {:padding "1em"}
   [:div "HI!"]
   [:div
    [:button "Undo"]
    [:button "Redo"]]
   [:div [div-with-canvas circles-state]]
   [:pre (with-out-str (cljs.pprint/pprint @*circles))]])
