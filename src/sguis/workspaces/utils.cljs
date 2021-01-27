(ns sguis.workspaces.utils
  (:require
    [clojure.string :as string]))

(defn classes
  "Combines CSS classes that can be conditional (when condition class) and keywords."
  [& args]
  (->> args
       (filter identity)
       (map name)
       (string/join " ")))
