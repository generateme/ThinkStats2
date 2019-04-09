(ns thinkstats-clj.chapter01
  (:require [thinkstats-clj.data.tablesaw :as ts]
            [thinkstats-clj.data.stata :as stata]))

;; import dataset from Stata

(def preg (stata/read-data->dataset "2002FemPreg" "../code/2002FemPreg.dct" "../code/2002FemPreg.dat.gz"))
(def resp (stata/read-data->dataset "2002FemResp" "../code/2002FemResp.dct" "../code/2002FemResp.dat.gz"))

(defn clean-preg
  [preg]
  (ts/update-column! preg "agepreg" :divide 100.0)
  )

(clean-preg preg)

;; nsfg.py - tests

;; how many rows
(.rowCount resp)
;; => 7643

;; how many unique values in column `pregnum` under key `1`
(get (frequencies (seq (ts/column resp "pregnum"))) 1)
;; => 1267

(ts/shape preg)
;; => {:columns 243, :rows 13593}

(.rowCount preg)
;; => 13593

(.get (ts/column preg "caseid") 13592)
;; => "12571"

(filter #(.startsWith %1 "") (.columnNames preg))

(let [cols ["pregordr" "nbrnaliv" "babysex" "birthwgt_lb" "birthwgt_oz" "prglngth" "outcome" "birthord" "birthord" "totalwgt_lb"]
      ks [1 1 1 7 0 39 1 1 22.75 7.5]]
  (map #(get (frequencies (seq (ts/column preg %1))) %2) cols ks))
