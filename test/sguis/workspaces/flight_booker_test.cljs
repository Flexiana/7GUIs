(ns sguis.workspaces.flight-booker-test
  (:require [sguis.workspaces.flight-booker :refer [booker-ui
                                                    booker-start
                                                    parse-date
                                                    can-book?
                                                    parse-date-format]]
            ["date-fns" :refer [addDays
                                subDays
                                format]]
            [cljs.test :as t
             :include-macros true
             :refer [is testing]]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [sguis.workspaces.test-utils :as u]))

(def testing-dates
  (let [today (js/Date.)]
    {:today     today
     :yesterday (subDays today 1)
     :tomorrow  (addDays today 1)
     :future    (addDays today 40)}))

(defn unparse-date [date]
  (format date parse-date-format))

(ws/deftest parse-date-specific-format-test
  (let [{:keys [today]} testing-dates]
    (is (=  today (parse-date today (unparse-date today))))))

(ws/deftest can-book-one-way?-test
  (let [{:keys [today yesterday tomorrow]} testing-dates]
    (letfn [(today-can-book? [booker]
              (can-book? booker today))]
      (is (false? (today-can-book? {:book-flight :one-way-flight
                                    :go-flight   (unparse-date yesterday)})))
      (is (true? (today-can-book? {:book-flight :one-way-flight
                                   :go-flight   (unparse-date today)})))
      (is (true? (today-can-book? {:book-flight :one-way-flight
                                   :go-flight   (unparse-date tomorrow)}))))))

(ws/deftest can-book-return?-test
  (let [{:keys [today yesterday tomorrow future]} testing-dates]
    (letfn [(today-can-book? [booker]
              (can-book? booker today))]
      (is (false? (today-can-book? {:book-flight   :return-flight
                                    :go-flight     (unparse-date yesterday)
                                    :return-flight (unparse-date tomorrow)})))
      (is (true? (today-can-book? {:book-flight   :return-flight
                                   :go-flight     (unparse-date today)
                                   :return-flight (unparse-date tomorrow)})))
      (is (false? (today-can-book? {:book-flight   :return-flight
                                    :go-flight     (unparse-date tomorrow)
                                    :return-flight (unparse-date tomorrow)})))
      (is (true? (today-can-book? {:book-flight   :return-flight
                                   :go-flight     (unparse-date tomorrow)
                                   :return-flight (unparse-date future)}))))))

(ws/deftest one-way-flight-ui
  (let [{:keys [today yesterday tomorrow]} testing-dates
        flight-selector                    #(.getByTestId % "flight-selector")
        go-flight-input                    #(.getByTestId % "go-flight")
        book-btn                           #(.getByText % "Book!")
        *booker                            (r/atom booker-start)]
    (u/with-mounted-component [booker-ui *booker]
      (fn [comp]
        (u/select-element! (flight-selector comp)  {:value {:option "one-way-flight"}})
        (u/input-element! (go-flight-input comp) {:target {:value (unparse-date today)}})
        (js/console.log (.getByText comp "Book!"))))))
