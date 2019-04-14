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
    (spit target "")
    (doseq [{:keys [docstring raw form type] :as all} (p/parse-file filename)]
      (spit target
            (condp = type
              :code (str docstring (code-block raw))
              :comment (if (str/starts-with? raw "=>")
                         (str (code-block raw))
                         (str raw "\n\n")))
            :append true))))

(save-md "src/thinkstats_clj/chapter01.clj")
(save-md "src/thinkstats_clj/chapter02.clj")
(save-md "src/thinkstats_clj/chapter03.clj")
(save-md "src/thinkstats_clj/chapter04.clj")
