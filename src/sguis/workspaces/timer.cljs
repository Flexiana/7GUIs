(ns sguis.workspaces.timer
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [cljs.pprint :as pp]))


(def *timer
  (r/atom {}))

(def progress-bar-style
  {:position      "relative"
   :height        "20px"
   :width         "350px"
   :border-radius "50px"
   :border        "1px solid #333"})

(def filler-style
  {:background    "#1DA598"
   :height        "100%"
   :border-radius "inherit"
   :transition    "width .2s ease-in"})

(defn timer-ui [timer-state]
  [:div {:style {:padding "1em"}}
   [:div "Timer ⏲️"]
   [:div {:className "progress-bar"
          :style     progress-bar-style}
    [:div {:className "filler"
           :style     filler-style}]]
   [:pre (with-out-str (pp/pprint @timer-state))]])
