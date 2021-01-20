(ns sguis.workspaces.test-utils
  (:require [reagent.core :as r]
            [goog.dom :as gdom]
            ["@testing-library/react" :as rtl]))

(def test-container-id "tests-container")

;; TODO try doing this setup/cleanup with fixtures instead
;; use-fixtures doesn't seem to be working - investigate.
(defn create-tests-container! []
  (let [container (gdom/createDom "div" #js {:id test-container-id})]
    (gdom/appendChild (-> js/document .-body) container)
    container))

(defn with-mounted-component [comp f]
  (let [container         (create-tests-container!)
        mounted-component (rtl/render (r/as-element comp)
                                      #js {"container" container})]
    (try
      (f mounted-component)
      (finally
        (rtl/cleanup)))))

(defn click-element! [el]
  (.click rtl/fireEvent el)
  (r/flush))

(defn input-element! [el action-map]
  (.input rtl/fireEvent el (clj->js action-map))
  (r/flush))

(defn change-element! [el action-map]
  (.change rtl/fireEvent el (clj->js action-map))
  (r/flush))
