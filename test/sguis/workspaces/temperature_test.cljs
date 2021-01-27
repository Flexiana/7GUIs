(ns sguis.workspaces.temperature-test
  (:require
    [cljs.test :as t
     :include-macros true
     :refer [is testing]]
    [nubank.workspaces.core :as ws]
    [sguis.workspaces.temperature :refer [temperature-ui
                                          convert]]
    [sguis.workspaces.test-utils :as u]))

(ws/deftest test-convert
  (testing "fahrenheit <-> celsius"
    (is (== 0 (convert :fahrenheit :celsius (convert :celsius :fahrenheit 0))))
    (is (== 5 (convert :fahrenheit :celsius (convert :celsius :fahrenheit 5)))))
  (testing "converted values are rounded"
    (is (== 90 (convert :celsius :fahrenheit 32)))
    (is (== 91 (convert :celsius :fahrenheit 32.9)))))

(ws/deftest temperature-ui-tests
  (let [celsius-input    #(.getByLabelText % #"(?i)celsius")
        fahrenheit-input #(.getByLabelText % #"(?i)fahrenheit")]
    (u/with-mounted-component [temperature-ui]
                              (fn [comp]
                                (let [_celsius->fahrenheit!     (u/change-element! (celsius-input comp) "5")
                                      celsius->fahrenheit-value (.-value (fahrenheit-input comp))
                                      _fahrenheit->celsius!     (u/change-element! (fahrenheit-input comp) celsius->fahrenheit-value)
                                      fahrenheit->celsius-value (.-value (celsius-input comp))]
                                  (is (= "41" celsius->fahrenheit-value))
                                  (is (= "5" fahrenheit->celsius-value)))))))
