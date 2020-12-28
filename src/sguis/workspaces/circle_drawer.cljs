(ns sguis.workspaces.circle-drawer
  (:require [reagent.core :as r]
            [reagent.dom :as dom]))

(def *circles
  (r/atom {:dom-node     nil
           :window-width nil
           :drawing      nil}))



(defn ui-draw-circles-on-canvas [circles-state]
  (let [{:keys [dom-node
                drawing]} @circles-state
        canvas            (.-firstChild dom-node)
        ctx               (.getContext canvas "2d")
        w                 (.-clientWidth canvas)
        h                 (.-clientHeight canvas)]
    (if-not drawing
      (do (doto ctx
            (.beginPath)
            (.arc 75 75 50 0 (* 2 Math/PI)
                  )
            ;;    x  y  r  startangle endangle
            (.stroke))
          :draw)
      (do (swap! circles-state dissoc :drawing)
          (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))))))

;;(defn dialog [state id] (get-in state [:dialogs id]))


;; (let [{:keys [opened?] :as dialog-state} (dialog @state)] (when opened? [dialog dialog-state :id-1]))

#_{:dialogs
   {:id-1
    {:opened? false}}
   :id-2
   {:opened? true}}

(defn div-with-canvas [circles-state]
  (let [{:keys [window-widht
                dom-node
                drawing]} @circles-state]

    (r/create-class
     {:component-did-mount
      (fn [this]
        (swap! circles-state assoc :dom-node (dom/dom-node this)))

      :reagent-render
      (fn [ ]
        window-widht
        [:div.with-canvas
         [:canvas {:style           {:border "1px solid #000000"}
                   :on-click        #(swap! circles-state
                                            assoc
                                            :drawing
                                            (ui-draw-circles-on-canvas circles-state))
                   :on-context-menu (fn [event]
                                      (js/console.log event)
                                      (swap! circles-state assoc :modal-opened true)
                                      (when event
                                        (.preventDefault event)))}
          (when dom-node
            {:width  (.-clientWidth dom-node)
             :height (.-clientHeight dom-node)})]])})))

(defn input-circles-ui [circles-state]
  (let [{:keys [modal-opened]} @circles-state]
    (if modal-opened
      [:div (with-out-str (cljs.pprint/pprint (str modal-opened)))]
      [:div])))

(defn circles-ui [circles-state]
  [:<>
   [:div "HI!"]
   [:div
    [:button "Undo"]
    [:button "Redo"]]
   [:div
    [div-with-canvas circles-state]]
   [:pre (with-out-str (cljs.pprint/pprint @*circles))]])
