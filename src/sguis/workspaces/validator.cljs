(ns sguis.workspaces.validator
  (:refer-clojure :exclude [float?]))

(defn numeric? [x]
  (and (number? x) (not (js/Number.isNaN x))))

(defn float? [s]
  (re-matches #"[+-]?(\d*\.)?\d+" s))

(comment
  (false? (numeric? (js/parseFloat "a")))
  (true? (numeric? (js/parseFloat "1")))
  )
