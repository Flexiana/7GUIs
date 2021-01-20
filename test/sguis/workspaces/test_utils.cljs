(ns sguis.workspaces.test-utils
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            ["@testing-library/react" :as rtl]))

(defn with-mounted-component [comp f]
  (let [mounted-component (rtl/render (r/as-element comp))]
    (try
      (f mounted-component)
      (finally
        (.-unmount comp)
        (r/flush)))
    (rtl/cleanup)))

(defn component-select-id [id comp]
  (-> comp
      .-container
      (.querySelector id)))

(defn click-element! [el]
  (.click rtl/fireEvent el)
  (r/flush))

(defn input-element! [el action-map]
  (.input rtl/fireEvent el (clj->js action-map))
  (r/flush))

(defn change-element! [el action-map]
  (.change rtl/fireEvent el (clj->js action-map))
  (r/flush))

(defn select-element! [el action-map]
  (.select rtl/fireEvent el (clj->js action-map))
  (r/flush))
