(ns thinkstats-clj.data.tablesaw
  (:require [fastmath.stats :as stats]
            [fastmath.core :as m]
            [clojure.pprint :as pp]
            [clojure.string :as str])
  (:import [tech.tablesaw.io.csv CsvReadOptions CsvReader]
           [tech.tablesaw.columns Column]
           [tech.tablesaw.api Table Row ShortColumn StringColumn IntColumn LongColumn DoubleColumn FloatColumn NumericColumn CategoricalColumn ColumnType]
           [tech.tablesaw.columns.numbers NumberMapFunctions]
           [java.util Iterator]
           [clojure.lang Seqable ISeq]))

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

(defn value-counts
  ([^Column col]
   (into (sorted-map) (frequencies (seq col))))
  ([^Table t ^String colname]
   (value-counts (column t colname))))

(defn shape
  [^Table data]
  {:columns (.columnCount data)
   :rows (.rowCount data)})

(defn rows
  [^Table data rs]
  (.rows data (int-array rs)))

(defn size
  [^Table data]
  (.rowCount data))

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
  ^Column [column operation & pars]
  (.setName ^Column (case operation
                      :log1p (.log1p ^NumberMapFunctions column)
                      :divide (.divide ^NumberMapFunctions column (first pars))
                      column) (.name ^Column column)))

(defn update-column!
  ^Table [^Table data col operation & pars]
  (let [^Column c (column data col)] 
    (.replaceColumn data col (apply update-column c operation pars))))

(defn replace-columns!
  ^Table [^Table data & cols]
  (doseq [^Column col cols]
    (.replaceColumn data (.name col) col)))

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

;;

(defn select-values
  "Select values using Selector"
  ([table colname selector] (select-values table colname selector false))
  ([^Table table colname selector missing?]
   (let [col (column table colname)
         data (map #(.get col ^int %) selector)]
     (if missing?
       data
       (remove #(.isMissingValue col %) data)))))


;;

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

;;;;

(defprotocol NanProto
  (nan-value [col]))

(extend-protocol NanProto
  StringColumn (nan-value [_] "")
  ShortColumn (nan-value [_] (tech.tablesaw.columns.numbers.ShortColumnType/missingValueIndicator))
  IntColumn (nan-value [_] (tech.tablesaw.columns.numbers.IntColumnType/missingValueIndicator))
  LongColumn (nan-value [_] (tech.tablesaw.columns.numbers.LongColumnType/missingValueIndicator))
  DoubleColumn (nan-value [_] (tech.tablesaw.columns.numbers.DoubleColumnType/missingValueIndicator))
  FloatColumn (nan-value [_] (tech.tablesaw.columns.numbers.FloatColumnType/missingValueIndicator)))

(defprotocol EchoMDProto
  (echo-md [data] [data config] "Generate markdown table"))

(defn lr-cols [^Table table l r]
  (let [f (partial column table)
        cc (.columnCount table)]
    (map seq [(map f (range l))
              (map f (range (- cc r) cc))])))

(defn join-lr
  ([f l r sep] (join-lr (map f l) (map f r) sep))
  ([l r sep]
   (str 
    (str/join "|" l)
    (when (seq r) (str "|" sep "|" (str/join "|" r)))
    "\n")))

(defn tb-rows [^Table table l r t b]
  (let [f (fn [lst] (map (fn [id] (join-lr #(let [v (.get % id)]
                                            (if (= v (nan-value %)) "NaN" v)) l r "...")) lst))
        tt (f t)
        bb (if b (concat (join-lr (constantly "---") l r "---")  (f b)) nil)]
    (concat tt bb)))


(extend-protocol EchoMDProto
  Table (echo-md
          ([^Table table {:keys [cols rows] :or {cols 5 rows 3}}]
           (let [cc (.columnCount table)
                 [l r] (if (> cc (* cols 2))
                         (lr-cols table cols cols)
                         (lr-cols table (min cc (* cols 2)) 0))
                 rc (.rowCount table)
                 [t b] (if (> rc (* rows 2))
                         [(range rows) (range (- rc rows) rc)]
                         [(range (min rc (* 2 rows))) nil])]
             (apply str (join-lr #(.name %) l r "...")
                    (join-lr (constantly "---") l r "---")
                    (tb-rows table l r t b))))
          ([table] (echo-md table nil))))

(defn fmap
  ([^Table table colname f]
   (fmap (column table colname) f))
  ([^Column col f]
   (let [value-fn (condp = (.type col)
                    ColumnType/SHORT short
                    ColumnType/INTEGER int
                    ColumnType/LONG long
                    ColumnType/DOUBLE double
                    ColumnType/FLOAT float
                    ColumnType/STRING str
                    identity)]
     (.map col (reify java.util.function.Function
                 (apply [t v] (value-fn (f t v))))))))

