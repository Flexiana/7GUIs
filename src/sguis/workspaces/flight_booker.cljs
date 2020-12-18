(ns sguis.workspaces.flight-booker
  (:require [reagent.core :as r]
            [cljs.pprint :as pp]))

(def booker-state
  (r/atom {:book-flight :one-way-flight}))

(defn booker-ui [booker-state]
  [:div {:style {:padding "1em"}}
   [:div "Book a flight ✈️"]
   [:label {:for "book-flight"}
    [:select {:on-change #(swap! booker-state assoc :book-flight (keyword (.. % -target -value)))
              :id        "book"}
     [:option {:value "one-way-flight"} "one-way flight"]
     [:option {:value "return-flight"} "return flight"]]]
   [:pre (with-out-str (pp/pprint @booker-state))]])
