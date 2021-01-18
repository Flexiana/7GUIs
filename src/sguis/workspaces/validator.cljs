(ns validator)

(defn number? [x]
  (and (number? x) (not (js/Number.isNaN x))))

#_(false? (numeric? (js/parseFloat "a")))
#_(true? (numeric? (js/parseFloat "1")))
