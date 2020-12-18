(ns sguis.workspaces.flight-booker
  (:require [reagent.core :as r]
            [cljs.pprint :as pp]))

(def booker-state
  (r/atom {:book-flight :one-way-flight}))

(defn booker-ui [booker-state]
  (let [{:keys [book-flight]} @booker-state]
    [:div {:style {:padding "1em"}}
     [:div "Book a flight ✈️"]
     [:label {:id "book-flight"}
      [:select {:id        "book-selector"
                :on-change #(swap! booker-state assoc :book-flight (keyword (.. % -target -value)))}
       [:option {:value "one-way-flight"} "one-way flight"]
       [:option {:value "return-flight"} "return flight"]]]
     [:div [:label {:id "go-flight"}
            [:input {}]]]
     [:div [:label {:id "return-flight"}
            [:input {:disabled (false? (= :one-way-flight book-flight))}]]]
     [:button "Book"]
     [:pre (with-out-str (pp/pprint @booker-state))]]))
