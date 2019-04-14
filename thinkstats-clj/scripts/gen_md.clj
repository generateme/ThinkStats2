(ns gen-md
  (:require [marginalia.parser :as p]
            [clojure.string :as str]))

(defn- code-block
  "Create code block from given string `s`"
  [s]
  (str "\n```clojure\n" s "\n```\n"))

(defn save-md
  "Save markdown built from clojure source"
  [filename]
  (let [target (str (second (re-find #"(.*)\.(\w+)$" filename)) ".md")]
    (println "saving " target)
    (spit target "")
    (doseq [{:keys [docstring raw form type] :as all} (p/parse-file filename)]
      (spit target
            (condp = type
              :code (str docstring (code-block raw))
              :comment (if (str/starts-with? raw "=>")
                         (str (code-block raw))
                         (str raw "\n\n")))
            :append true))))

(run! save-md (map #(str "src/thinkstats_clj/chapter" (format "%02d" (inc %)) ".clj") (range 4)))
