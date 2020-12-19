(ns sguis.workspaces.flight-booker
  (:require [reagent.core :as r]
            [cljs.pprint :as pp]))

(def booker-state
  (r/atom {:book-flight :one-way-flight}))

(defn forms-valid? [booker-state]
  false)

(defn flight-selector [booker-state]
  [:label {:id "book-flight"}
   [:select {:id        "book-selector"
             :on-change #(swap! booker-state assoc :book-flight (keyword (.. % -target -value)))}
    [:option {:value "one-way-flight"} "one-way flight"]
    [:option {:value "return-flight"} "return flight"]]])

(defn go-flight-input [booker-state]
  [:div
   [:label {:id "go-flight"}
    [:input {:type      "text"
             :on-change #(swap! booker-state assoc :go-flight  (.. % -target -value))}]]])

(defn return-flight-input [booker-state]
  (let [{:keys [book-flight]} @booker-state]
    [:div
     [:label {:id "return-flight"}
      [:input {:type      "text"
               :on-change #(swap! booker-state assoc :return-flight  (.. % -target -value))
               :disabled  (false? (= :one-way-flight book-flight))}]]]))

(defn book-button [booker-state]
  [:button {:disabled (not (forms-valid? booker-state))}
   "Book!"])

(defn booker-ui [booker-state]
  [:div {:style {:padding "1em"}}
   [:div "Book a flight ✈️"]
   [flight-selector booker-state]
   [go-flight-input booker-state]
   [return-flight-input booker-state]
   [book-button booker-state]
   [:pre (with-out-str (pp/pprint @booker-state))]])
