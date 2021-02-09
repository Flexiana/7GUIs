(ns sguis.workspaces.flight-booker
  "7GUIs flight-booker"
  (:require
    ["date-fns" :as dfns]
    [reagent.core :as r]
    [sguis.workspaces.utils :as u]))

(def date-format "yyyy.MM.dd")

(defn format-date
  "Date formatter"
  [d]
  (dfns/format d date-format))

(defn valid-date?
  "Date validator"
  [s]
  (dfns/isMatch s date-format))

(defn parse-date
  "Date parser"
  [s]
  (when (valid-date? s)
    (dfns/parse s date-format (js/Date.))))

(defn booker-start
  "Initial state"
  [booking-available-from]
  {:flight-type   :one-way-flight
   :go-flight     (format-date booking-available-from)
   :return-flight (format-date booking-available-from)
   :booker-msg    nil})

(def go-flight
  "Extract date of depart"
  (comp parse-date :go-flight))

(def return-flight
  "Extract date of return"
  (comp parse-date :return-flight))

(defmulti can-book?
  "Booking validation"
  (fn [booking _] (:flight-type booking)))

(defmulti format-msg
  "Message formatter"
  :flight-type)

(defmethod can-book? :one-way-flight
  ;;Validates if can book a one way flight
  [b booking-available-from]
  (>= (go-flight b) booking-available-from))

(defmethod can-book? :return-flight
  ;;Validates if can book a returning flight
  [b booking-available-from]
  (>= (return-flight b) (go-flight b) booking-available-from))

(defmethod format-msg :one-way-flight
  ;;One way flight message
  [{:keys [go-flight]}]
  (str "✅ You have booked a one-way flight for " go-flight))

(defmethod format-msg :return-flight
  ;;Two way flight message
  [{:keys [go-flight return-flight]}]
  (str "✅ You have booked a return flight from " go-flight
       " to " return-flight))

(defn select-booking!
  "Stores flight type"
  [*booker e]
  (swap! *booker assoc :flight-type (keyword (.. e -target -value))))

(defn flight-selector
  "Visual representation of flight type selection"
  [select-booking!]
  [:div.field
   [:div.control.is-expanded
    [:div.select.is-fullwidth
     [:select {:data-testid "flight-selector"
               :on-change   select-booking!}
      [:option {:value "one-way-flight"} "one-way flight"]
      [:option {:value "return-flight"} "return flight"]]]]])

(defn go-flight-change!
  "Change flight "
  [*booker e]
  (swap! *booker assoc :go-flight (.. e -target -value)))

(defn return-flight-change!
  [*booker e]
  (swap! *booker assoc :return-flight (.. e -target -value)))

(defn flight-input
  "Visual representation of flight selector"
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
  "Stores booker's message"
  [*booker msg _]
  (swap! *booker assoc :booker-msg msg))

(defn book-button
  "Make a book"
  [booker booking-available-from booking-message!]
  [:button.button.is-primary
   {:data-testid "book-button"
    :disabled    (not (can-book? booker booking-available-from))
    :on-click    #(booking-message! (format-msg booker))}
   "Book"])

(defn reset-booker-msg!
  "Delete booker's message"
  [*booker _]
  (swap! *booker assoc :booker-msg nil))

(defn book-message
  "Visual representation of message from booker"
  [{:keys [booker-msg]} reset-booker-msg!]
  (when booker-msg
    [:div.notification.is-success.is-light.mt-4.is-flex-grow-0
     [:button.delete {:data-testid "reset-button"
                      :on-click    reset-booker-msg!}]
     [:span
      {:data-testid "book-msg"}
      booker-msg]]))

(defn booker-ui
  "Main flight booker UI"
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
