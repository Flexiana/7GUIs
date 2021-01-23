(ns sguis.workspaces.test-utils
  (:require [reagent.core :as r]
            [goog.dom :as gdom]
            ["@testing-library/react" :as rtl]
            ["@testing-library/user-event" :as rtue]
            ["@sinonjs/fake-timers" :as timer]))

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

(defn click-element!
  ([el]
   (click-element! el #js {}))
  ([el action-map]
   (let [click-fn! (.. rtue -default -click)]
     (click-fn! el (clj->js action-map)))
   (r/flush)))

(defn click-context-menu! [el]
  (.contextMenu rtl/fireEvent el)
  (r/flush))

(defn action-map [value]
  (clj->js {:target {:value value}}))

(defn input-element! [el value]
  (.input rtl/fireEvent el (action-map value))
  (r/flush))

(defn change-element! [el value]
  (.change rtl/fireEvent el (action-map value))
  (r/flush))

(defn install-timer []
  (.install timer (.-getTime js/Date.)))

(defn tick [t x]
  (.tick t x))

(defn uninstall-timer [t]
  (.uninstall t))
