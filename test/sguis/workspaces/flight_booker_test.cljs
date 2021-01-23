(ns sguis.workspaces.flight-booker-test
  (:require [sguis.workspaces.flight-booker :refer [booker-ui
                                                    booker-start
                                                    parse-date
                                                    can-book?
                                                    format-msg
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
  (let [february-idx 1]
    (is (= (js/Date. 2020 february-idx 29) (parse-date "2020.02.29")))))

(ws/deftest can-book-one-way?-test
  (let [{:keys [today yesterday tomorrow]} testing-dates]
    (letfn [(today-can-book? [booker]
              (can-book? booker today))]
      (is (not (today-can-book? {:book-flight :one-way-flight
                                 :go-flight   (unparse-date yesterday)})))
      (is (today-can-book? {:book-flight :one-way-flight
                            :go-flight   (unparse-date today)}))
      (is (today-can-book? {:book-flight :one-way-flight
                            :go-flight   (unparse-date tomorrow)})))))

(ws/deftest one-way-flight-ui-test
  (let [{:keys [today yesterday tomorrow]} testing-dates
        flight-selector                    #(.getByTestId % "flight-selector")
        go-flight-input                    #(.getByTestId % "go-flight")
        book-btn                           #(.getByText % "Book!")
        reset-btn                          #(.getByText % "Reset!")
        *booker                            (r/atom booker-start)]
    (u/with-mounted-component [booker-ui *booker today]
      (fn [comp]
        (testing "Cannot book for yesterday"
          (u/input-element! (go-flight-input comp) (unparse-date yesterday))
          (is (.-disabled (book-btn comp))))

        (testing "Can book today"
          (u/input-element! (go-flight-input comp) (unparse-date today))
          (u/click-element! (book-btn comp))
          (is (= (format-msg @*booker) (.-textContent (.getByTestId comp "book-msg"))))
          (u/click-element! (reset-btn comp)))

        (testing "Can book tomorrow"
          (u/input-element! (go-flight-input comp) (unparse-date tomorrow))
          (u/click-element! (book-btn comp))
          (is (= (format-msg @*booker) (.-textContent (.getByTestId comp "book-msg")))))
        (u/click-element! (reset-btn comp))))))

(ws/deftest can-book-return?-test
  (let [{:keys [today yesterday tomorrow future]} testing-dates]
    (letfn [(today-can-book? [booker]
              (can-book? booker today))]
      (is (not (today-can-book? {:book-flight   :return-flight
                                 :go-flight     (unparse-date yesterday)
                                 :return-flight (unparse-date tomorrow)})))
      (is (today-can-book? {:book-flight   :return-flight
                            :go-flight     (unparse-date today)
                            :return-flight (unparse-date tomorrow)}))
      (is (not (today-can-book? {:book-flight   :return-flight
                                 :go-flight     (unparse-date tomorrow)
                                 :return-flight (unparse-date tomorrow)})))
      (is (today-can-book? {:book-flight   :return-flight
                            :go-flight     (unparse-date tomorrow)
                            :return-flight (unparse-date future)})))))

(ws/deftest return-flight-ui-test
  (let [{:keys [today yesterday tomorrow future]} testing-dates
        flight-selector                           #(.getByTestId % "flight-selector")
        go-flight-input                           #(.getByTestId % "go-flight")
        return-flight-input                       #(.getByTestId % "return-flight")
        book-btn                                  #(.getByText % "Book!")
        book-msg                                  #(.getByTestId % "book-msg")
        reset-btn                                 #(.getByText % "Reset!")
        *booker                                   (r/atom booker-start)]
    (u/with-mounted-component [booker-ui *booker today]
      (fn [comp]
        (testing "Cannot book yesterday"
          (u/change-element! (flight-selector comp) "return-flight")
          (u/input-element! (go-flight-input comp) (unparse-date yesterday))
          (u/input-element! (return-flight-input comp) (unparse-date tomorrow))
          (is (.-disabled (book-btn comp))))

        (testing "Can book today"
          (u/change-element! (flight-selector comp) "return-flight")
          (u/input-element! (go-flight-input comp) (unparse-date today))
          (u/input-element! (return-flight-input comp) (unparse-date tomorrow))
          (u/click-element! (book-btn comp))
          (is (= (format-msg @*booker) (.-textContent (book-msg comp)))))
        (u/click-element! (reset-btn comp))

        (testing "Cannot book sameday"
          (u/change-element! (flight-selector comp) "return-flight")
          (u/input-element! (go-flight-input comp) (unparse-date tomorrow))
          (u/input-element! (return-flight-input comp) (unparse-date tomorrow))
          (is (.-disabled (book-btn comp))))

        (testing "Can book in future."
          (u/change-element! (flight-selector comp) "return-flight")
          (u/input-element! (go-flight-input comp) (unparse-date tomorrow))
          (u/input-element! (return-flight-input comp) (unparse-date future))
          (u/click-element! (book-btn comp))
          (is (= (format-msg @*booker) (.-textContent (book-msg comp)))))
        (u/click-element! (reset-btn comp))))))