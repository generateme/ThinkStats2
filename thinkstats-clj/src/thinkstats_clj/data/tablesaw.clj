(ns thinkstats-clj.data.tablesaw
  (:require [fastmath.stats :as stats]
            [fastmath.core :as m])
  (:import [tech.tablesaw.io.csv CsvReadOptions CsvReader]
           [tech.tablesaw.columns Column]
           [tech.tablesaw.api Table Row ShortColumn StringColumn IntColumn LongColumn DoubleColumn FloatColumn NumericColumn CategoricalColumn ColumnType]
           [tech.tablesaw.columns.numbers NumberMapFunctions]
           [java.util Iterator]
           [clojure.lang Seqable ISeq])
  (:require [fastmath.stats :as stats]))

;;

(defonce ^CsvReader csv-reader (CsvReader.))

(defn load-csv-data
  ([file] (load-csv-data file nil))
  ([^String file {:keys [separator line-ending header?]
                  :or {separator \, line-ending "\n" header? true}}]
   (let [builder (doto (CsvReadOptions/builder file)
                   (.separator separator)
                   (.lineEnding line-ending)
                   (.header header?))]
     (->> builder
          (.build)
          (.read csv-reader)))))

(defn row-iterator
  [^Table table row-selector]
  (loop [coll []
         ^Iterator iter (.next (.iterator table))]
    (let [ncoll (conj coll (row-selector iter))]
      (if (.hasNext iter) (recur ncoll (.next iter)) ncoll))))

(defn column
  ^Column [^Table data id]
  (let [id (if (string? id) (.columnIndex data id) id)]
    (.column data id)))

(defn columns
  [^Table data]
  (.columns data))

(defn drop-columns!
  ^Table [^Table data & ids]
  (let [columns (if (string? (first ids))
                  (into-array String ids)
                  (into-array Integer ids))]
    (.removeColumns data columns)))

(defn drop-column!
  ^Table [data id]
  (drop-columns! data id))

(defn numerical-columns
  ^Table [^Table data]
  (Table/create (str (.name data) " (numerical)") (into-array Column (.numericColumns data))))

(defn string-columns
  ^Table [^Table data]
  (Table/create (str (.name data) " (string)") (into-array Column (.stringColumns data))))

(defn- structure-selector
  [^Row row]
  [(.getInt row 0) (.getString row 1) (keyword (.getString row 2))])

(defn structure
  (^Table [data] (structure data true))
  (^Table [^Table data raw?]
   (let [s (.structure data)]
     (if raw? s 
         (row-iterator s structure-selector)))))

(defn- count-by-category-selector
  [^Row row]
  [(.getString row 0) (.getInt row 1)])

(defn count-by-category
  [^CategoricalColumn column]
  (row-iterator (.countByCategory column) count-by-category-selector))

(defn shape
  [^Table data]
  {:columns (.columnCount data)
   :rows (.rowCount data)})

(defmulti summary (fn [d & _] (type d)))

(defmethod summary tech.tablesaw.api.Table
  ([^Table data & columns]
   (let [check-columns (if (seq columns) (set columns) (constantly true))]
     (reduce (fn [m ^Column c]
               (if (and (instance? NumericColumn c)
                        (check-columns (.name c)))
                 (assoc m (.name c) (dissoc (stats/stats-map c) :Outliers))
                 m)) {} (.columns data)))))

(defmethod summary tech.tablesaw.columns.Column
  [^Column col]
  (when (instance? NumericColumn col)
    (dissoc (stats/stats-map col) :Outliers)))

(defn update-column
  ^Column [column operation pars]
  (.setName ^Column (case operation
                      :log1p (.log1p ^NumberMapFunctions column)
                      :divide (.divide ^NumberMapFunctions column (first pars))
                      column) (.name ^Column column)))

(defn update-column!
  ^Table [^Table data col operation & pars]
  (let [^Column c (column data col)] 
    (.replaceColumn data col (update-column c operation pars))))

;;

(defn merge-tables
  ^Table [^Table a ^Table b]
  (-> (.emptyCopy a)
      (.append a)
      (.append b)))

;; one hot encoding

(defn encode-column
  [^String nm value column]
  (ShortColumn/create nm (short-array (mapv #(if (= value %) 1 0) column))))

(defn encode-columns
  #^"[Ltech.tablesaw.columns.Column;" [^Column column labels]
  (println "Processing column: " (.name column))
  (into-array Column (mapv #(encode-column (str (.name column) "-" %) % column) labels)))

(defn one-hot-encoding
  ^Table [^Table data labels] 
  (let [ndata (Table/create (str (.name data) " - one-hot encoded"))] 
    (reduce (fn [^Table table ^Column c]
              (if (and (#{ColumnType/STRING ColumnType/BOOLEAN} (.type c))
                       (contains? labels (.name c)))
                (.addColumns table (encode-columns c (labels (.name c))))
                (.addColumns table #^"[Ltech.tablesaw.columns.Column;" (into-array Column [c])))) ndata (.columns data))))

(defn row->doubles
  ^doubles [cols ^long cnt ^long row-id]
  (let [arr (double-array cnt)]
    (dotimes [i cnt]
      (aset ^doubles arr i (.getDouble ^NumericColumn (cols i) row-id)))
    arr))

(defn table->double-arrays
  [^Table table]
  (let [cols (vec (.columns table))
        cnt (count cols)]
    (into-array (mapv (partial row->doubles cols cnt) (range (.rowCount table))))))

;;

(defn missing-values
  [^Table table]
  (let [^Table mt (.missingValueCounts table)]
    (reduce (fn [m ^NumericColumn c]
              (let [v (.getDouble c 0)]
                (if (pos? v)
                  (assoc m (second (re-find #"\[(.*)\]" (.name c))) v)
                  m))) {} (.columns mt))))

(defn string-column
  [name data]
  (StringColumn/create (str name) data))

(defn short-column
  [name data]
  (ShortColumn/create (str name) (short-array data)))

(defn int-column
  [name data]
  (IntColumn/create (str name) (int-array data)))

(defn long-column
  [name data]
  (LongColumn/create (str name) (long-array data)))

(defn double-column
  [name data]
  (DoubleColumn/create (str name) (double-array data)))

(defn float-column
  [name data]
  (FloatColumn/create (str name) (float-array data)))

(defn columns->table
  [name columns]
  (Table/create name #^"[Ltech.tablesaw.columns.Column;" (into-array Column columns)))

(defn add-columns!
  [^Table table & columns]
  (.addColumns table #^"[Ltech.tablesaw.columns.Column;" (into-array Column columns)))
