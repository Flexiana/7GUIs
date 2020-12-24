(ns sguis.workspaces.circle-drawer
  (:require [reagent.core :as r]
            [reagent.dom :as dom]))

(def *circles
  (r/atom {}))

(def window-width (r/atom nil))

(defn draw-canvas-contents [ canvas ]
  (let [ctx (.getContext canvas "2d")
        w   (.-clientWidth canvas)
        h   (.-clientHeight canvas)]
    (.beginPath ctx)
    (.arc ctx 75 75 50 0 (* 2 js/Math.PI) true)
    (.stroke ctx)))

(defn div-with-canvas [ ]
  (let [dom-node (r/atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (reset! dom-node (dom/dom-node this)))

      :reagent-render
      (fn [ ]
        @window-width
        [:div.with-canvas
         [:canvas {:on-click #(draw-canvas-contents (.-firstChild @dom-node))}
          (if-let [node @dom-node]
            {:width  (.-clientWidth node)
             :height (.-clientHeight node)})]])})))


(defn circles-ui [circles-state]
  [:<> [:div "HI!"]
   [div-with-canvas]])
