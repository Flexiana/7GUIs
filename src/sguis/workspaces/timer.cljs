(ns sguis.workspaces.timer
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [cljs.pprint :as pp]))


(def *timer
  (r/atom {:timer-slider "50"}))

(def container-style
  {:position      "relative"
   :width         "100%"
   :height        "0.5em"
   :border-radius "50px"
   :padding       0
   :border        "1px solid #333"})

(def filler-style
  {:position "relative"
   :border-radius "50px"
   :box-shadow "0 0 10px 0"
   :height "0.5em"
   :opacity 1
   :color "rgb(178, 34, 34)"
   :background-color "rgb(178, 34, 34)"})

(defn progress-bar
  [{:keys [timer-slider]}]
  [:div {:style container-style}
   [:div {:style (merge filler-style {:width (-> timer-slider
                                                 (str "%"))})}]])

(defn timer-ui [timer-state]
  [:div {:style {:padding "1em"}}
   [:div {:style {:padding "0.5em"}}
    "Timer ⏲️"]
   [progress-bar @*timer]
   [:div {:class "timer-slider"}
    [:input {:type "range"
             :min "1"
             :max "100"
             :on-input #(swap! *timer assoc :timer-slider (.. % -target -value))}]]
   [:pre (with-out-str (pp/pprint @*timer))]])
