(ns sguis.workspaces.circle-drawer
  (:require [reagent.core :as r]
            [reagent.dom :as dom]))

(def *circles
  (r/atom {:dom-node     nil
           :window-width nil}))

(defn draw-canvas-contents [circles-state]
  (let [{:keys [dom-node]} @circles-state
        canvas             (.-firstChild dom-node)
        ctx                (.getContext canvas "2d")
        w                  (.-clientWidth canvas)
        h                  (.-clientHeight canvas)]
    (.beginPath ctx)
    (.arc ctx 75 75 50 0 (* 2 js/Math.PI) true)
    (.stroke ctx)))

(defn div-with-canvas [circles-state]
  (let [{:keys [window-widht
                dom-node]} @circles-state]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (swap! circles-state assoc :dom-node (dom/dom-node this)))

      :reagent-render
      (fn [ ]
        window-widht
        [:div.with-canvas
         [:canvas {:on-click #(draw-canvas-contents circles-state)}
          (when dom-node
            {:width  (.-clientWidth dom-node)
             :height (.-clientHeight dom-node)})]])})))


(defn circles-ui [circles-state]
  [:<> [:div "HI!"]
   [div-with-canvas circles-state]])
