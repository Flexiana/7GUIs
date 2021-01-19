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

(defn click-element! [el]
  (.click rtl/fireEvent el)
  (r/flush))
