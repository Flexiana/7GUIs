(ns sguis.workspaces.flight-booker
  (:require
    ["date-fns" :as dfns]
    [reagent.core :as r]
    [sguis.workspaces.utils :as u]))

(def date-format "yyyy.MM.dd")

(defn format-date
  [d]
  (dfns/format d date-format))

(defn valid-date?
  [s]
  (dfns/isMatch s date-format))

(defn parse-date
  [s]
  (when (valid-date? s)
    (dfns/parse s date-format (js/Date.))))

(defn booker-start
  [booking-available-from]
  {:flight-type   :one-way-flight
   :go-flight     (format-date booking-available-from)
   :return-flight (format-date booking-available-from)
   :booker-msg    nil})

(def go-flight (comp parse-date :go-flight))
(def return-flight (comp parse-date :return-flight))

(defmulti can-book? (fn [booking _] (:flight-type booking)))

(defmulti format-msg :flight-type)

(defmethod can-book? :one-way-flight [b booking-available-from]
  (>= (go-flight b) booking-available-from))

(defmethod can-book? :return-flight [b booking-available-from]
  (>= (return-flight b) (go-flight b) booking-available-from))

(defmethod format-msg :one-way-flight [{:keys [go-flight]}]
  (str "✅ You have booked a one-way flight for " go-flight))

(defmethod format-msg :return-flight [{:keys [go-flight return-flight]}]
  (str "✅ You have booked a return flight from " go-flight
       " to " return-flight))

(defn select-booking!
  [*booker e]
  (swap! *booker assoc :flight-type (keyword (.. e -target -value))))

(defn flight-selector
  [select-booking!]
  [:div.field
   [:div.control.is-expanded
    [:div.select.is-fullwidth
     [:select {:data-testid "flight-selector"
               :on-change   select-booking!}
      [:option {:value "one-way-flight"} "one-way flight"]
      [:option {:value "return-flight"} "return flight"]]]]])

(defn go-flight-change!
  [*booker e]
  (swap! *booker assoc :go-flight (.. e -target -value)))

(defn return-flight-change!
  [*booker e]
  (swap! *booker assoc :return-flight  (.. e -target -value)))

(defn flight-input
  [{:keys [testid value disabled flight-change!]}]
  [:div.field
   [:input.input {:data-testid testid
                  :type        :text
                  :class       (u/classes
                                 (when-not (valid-date? value) :is-danger))
                  :disabled    disabled
                  :value       value
                  :on-change   flight-change!}]])

(defn booking-message!
  [*booker msg _]
  (swap! *booker assoc :booker-msg msg))

(defn book-button
  [booker booking-available-from booking-message!]
  [:button.button.is-primary
   {:data-testid "book-button"
    :disabled    (not (can-book? booker booking-available-from))
    :on-click    #(booking-message! (format-msg booker))}
   "Book!"])

(defn reset-booker-msg!
  [*booker _]
  (swap! *booker assoc :booker-msg nil))

(defn book-message
  [{:keys [booker-msg]} reset-booker-msg!]
  (when booker-msg
    [:div.notification.is-success.is-light.mt-4.is-flex-grow-0
     [:button.delete {:data-testid "reset-button"
                      :on-click    reset-booker-msg!}]
     [:span
      {:data-testid "book-msg"}
      booker-msg]]))

(defn booker-ui
  ([]
   (r/with-let [today (dfns/startOfToday)
                *booker (r/atom (booker-start today))]
     [booker-ui *booker today]))
  ([*booker booking-available-from]
   (let [{:keys [flight-type go-flight return-flight]} @*booker]
     [:div.panel.is-primary
      {:style {:min-width "24em"}}
      [:div.panel-heading "Book a flight ✈️"]
      [:div.panel-block.is-block
       [flight-selector (partial select-booking! *booker)]
       [flight-input {:testid         "go-flight"
                      :value          go-flight
                      :flight-change! (partial go-flight-change! *booker)}]
       [flight-input {:testid         "return-flight"
                      :value          return-flight
                      :disabled       (not= :return-flight flight-type)
                      :flight-change! (partial return-flight-change! *booker)}]
       [book-button @*booker booking-available-from (partial booking-message! *booker)]
       [book-message @*booker (partial reset-booker-msg! *booker)]]])))
