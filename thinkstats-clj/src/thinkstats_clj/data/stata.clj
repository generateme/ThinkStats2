;; https://github.com/ray1729/thinkstats/blob/master/src/thinkstats/dct_parser.clj

(ns thinkstats-clj.data.stata
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [thinkstats-clj.data.tablesaw :as ts])
  (:import [java.util.zip GZIPInputStream ZipInputStream]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(def ^:const dict-line-rx #"^\s+_column\((\d+)\)\s+(byte|int|long|float|double|str)[0-9]*\s+(\S+)\s+%([0-9.]+).\s+\"([^\"]+)\"")

(defn type-parser
  "Parse given type. Empty numbers are treated as ##NaN"
  [type]
  (case type
    "byte" #(if-not (empty? %) (Short/parseShort %) Short/MIN_VALUE)
    "int" #(if-not (empty? %) (Integer/parseInt %) Integer/MIN_VALUE)
    "long" #(if-not (empty? %) (Long/parseLong %) Long/MIN_VALUE)
    "float" #(if-not (empty? %) (Float/parseFloat %) ##NaN)
    "double" #(if-not (empty? %) (Double/parseDouble %) ##NaN)
    identity))

(defn parse-dict-line
  "Parse dictionary line, return map"
  [line]
  (let [[_ col type name len descr] (re-find dict-line-rx line)
        column (dec (Integer/parseInt col))]
    {:column column
     :parser (type-parser type)
     :name name
     :type type
     :end (+ column (Integer/parseInt len))
     :description descr}))

(defn read-dict
  "Read dictionary"
  [path]
  (with-open [r (io/reader path)]
    (mapv parse-dict-line (butlast (rest (line-seq r))))))

(defn reader
  "Open path with io/reader; coerce to a GZIPInputStream if suffix is .gz"
  [path]
  (condp #(.endsWith ^String %2 ^String %1) path
    ".gz" (io/reader (GZIPInputStream. (io/input-stream path)))
    ".zip" (io/reader (ZipInputStream. (io/input-stream path)))
    (io/reader path)))

(defn read-field
  "Read filed according to dictionary"
  [{:keys [column end parser]} line]
  (parser (s/trim (subs line column end))))

(defn data-line-parser
  "Read line from data and parse"
  [dct-path]
  (fn [line]
    (mapv #(read-field % line) dct-path)))

(def type->col-fn {"byte" ts/short-column
                   "int" ts/int-column
                   "long" ts/long-column
                   "float" ts/float-column
                   "double" ts/double-column
                   "str" ts/string-column})

(defn- dict-line->column
  "Create tablesaw column according to dictionary, add data"
  [dict data]
  (let [f (type->col-fn (:type dict))]
    (f (:name dict) data)))

(defn- filename
  [path]
  (last (re-find #"\/([a-zA-Z0-9]*?)\..*$" path)))

(defn read-data->dataset
  "Read data from Stata formats and store them in dataset"
  ([dct-path data-path] (read-data->dataset (filename dct-path) dct-path data-path))
  ([table-name dct-path data-path]
   (let [dict (read-dict dct-path)
         parser (data-line-parser dict)
         data (with-open [^java.io.Reader r (reader data-path)]
                (mapv parser (line-seq r)))]
     (ts/columns->table table-name
                        (doall (map-indexed (fn [id d]
                                              (dict-line->column d (mapv #(nth % id) data))) dict))))))

