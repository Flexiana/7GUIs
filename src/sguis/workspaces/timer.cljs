(ns sguis.workspaces.timer
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [cljs.pprint :as pp]))


(def timer-state
  (r/atom {}))

(defn timer-ui [timer-state]
  [:div {:style {:padding "1em"}}
   [:div "Timer ⏲️"]
   [:pre (with-out-str (pp/pprint @timer-state))]])
