(ns thinkstats-clj.chapter01
  (:require [thinkstats-clj.data.tablesaw :as ts]
            [thinkstats-clj.data.stata :as stata]
            [thinkstats-clj.nsfg :as nsfg]
            [fastmath.stats :as stats]))

;; import dataset
(def preg (nsfg/fem-preg))

;; shape of dataset
(ts/shape preg)
;; => {:columns 244, :rows 13593}

;; select first 5 rows from 10 columns
(println (.first (.select preg (into-array String (take 10 (.columnNames preg)))) 5))
;; #object[tech.tablesaw.api.Table 0x2b6d4db7                                                            2002FemPreg                                                           
;;         caseid  |  pregordr  |  howpreg_n  |  howpreg_p  |  moscurrp  |  nowprgdk  |  pregend1  |  pregend2  |  nbrnaliv  |  multbrth  |
;;         ---------------------------------------------------------------------------------------------------------------------------------
;;         1  |         1  |             |             |            |            |         6  |            |         1  |            |
;;         1  |         2  |             |             |            |            |         6  |            |         1  |            |
;;         2  |         1  |             |             |            |            |         5  |            |         3  |         5  |
;;         2  |         2  |             |             |            |            |         6  |            |         1  |            |
;;         2  |         3  |             |             |            |            |         6  |            |         1  |            |]

;; list column names
(.columnNames preg)
;; => ["caseid" "pregordr" "howpreg_n" "howpreg_p" "moscurrp" "nowprgdk" "pregend1" "pregend2" "nbrnaliv" "multbrth" "cmotpreg" "prgoutcome" "cmprgend" "flgdkmo1" "cmprgbeg" "ageatend" "hpageend" "gestasun_m" "gestasun_w" "wksgest" "mosgest" "dk1gest" "dk2gest" "dk3gest" ... "finalwgt" "secu_p" "sest" "cmintvw" "totalwgt_lb"]

;; select column under id=1
(ts/column preg 1)
;; => #object[tech.tablesaw.api.ShortColumn 0x4e92a539 "Short column: pregordr"]

;; select column by name
(def pregordr (ts/column preg "pregordr"))
(type pregordr)
;; => tech.tablesaw.api.ShortColumn

;; first 10 values
(take 10 pregordr)
;; => (1 2 1 2 3 1 2 3 1 2)

;; number of values
(.size pregordr)
;; => 13593

;; select first element by id
;; tablesaw columns are collections but not sequences (they are Seqable)
(nth (seq pregordr) 0)
;; => 1

;; or
(.get pregordr 0)
;; => 1

;; select 3 elements from id=2
(take 3 (drop 2 pregordr))
;; => (1 2 3)

;; count frequencies from table `outcome`
(into (sorted-map) (ts/value-counts (ts/column preg "outcome")))
;; => {1 9148, 2 1862, 3 120, 4 1921, 5 190, 6 352}

;; count frequencies from table `brithwgt_lb`
(into (sorted-map) (ts/value-counts (.removeMissing (ts/column preg "birthwgt_lb"))))
;; => {0 8,
;;     1 40,
;;     2 53,
;;     3 98,
;;     4 229,
;;     5 697,
;;     6 2223,
;;     7 3049,
;;     8 1889,
;;     9 623,
;;     10 132,
;;     11 26,
;;     12 10,
;;     13 3,
;;     14 3,
;;     15 1}

;; create list of case indices
(def preg-map (nsfg/preg-map preg))

(defn select-for-case
  "Select values from given column for case"
  [colname caseid]
  (let [c (ts/column preg colname)]
    (map #(.get c %) (preg-map (str caseid)))))

;; select values from `outcome` for caseid `10229`
(select-for-case "outcome" 10229)
;; => (4 4 4 4 4 4 1)

;;;;;;;;; EXCERCISES

;; birthord frequencies
(into (sorted-map) (ts/value-counts (.removeMissing (ts/column preg "birthord"))))
;; => {1 4413, 2 2874, 3 1234, 4 421, 5 126, 6 50, 7 20, 8 7, 9 2, 10 1}

;; how many missing values
(.countMissing (ts/column preg "birthord"))
;; => 4445

;; check counts for given ranges
(let [cnts (->> (ts/column preg "prglngth")
                (group-by (fn [v]
                            (cond
                              (<= 0 v 13) "0-13"
                              (<= 14 v 26) "14-26"
                              :else "27-50")))
                (map (fn [[k v]] [k (count v)]))
                (into (sorted-map)))]
  (assoc cnts "Total" (reduce + (vals cnts))))
;; => {"0-13" 3522, "14-26" 793, "27-50" 9278, "Total" 13593}

;; mean of total weight
(stats/mean (.removeMissing (ts/column preg "totalwgt_lb")))
;; => 7.26562845762337

;; create column with weight in kg
(def totalwgt-kg (ts/fmap (.removeMissing (ts/column preg "totalwgt_lb")) #(* 0.45359237 %2)))

;; mean of weight in kgs
(stats/mean totalwgt-kg)
;; => 3.2956336316328283

;; read response data
(def resp (nsfg/fem-resp))

(ts/shape resp)
;; => {:columns 3087, :rows 7643}

;; select first 5 rows from 10 columns
(println (.first (.select resp (into-array String (take 10 (.columnNames resp)))) 5))
;; #object[tech.tablesaw.api.Table 0x4a71a736                                                         2002FemResp                                                         
;;         caseid  |  rscrinf  |  rdormres  |  rostscrn  |  rscreenhisp  |  rscreenrace  |  age_a  |  age_r  |  cmbirth  |  agescrn  |
;;         ----------------------------------------------------------------------------------------------------------------------------
;;         2298  |        1  |         5  |         5  |            1  |            5  |     27  |     27  |      902  |       27  |
;;         5012  |        1  |         5  |         1  |            5  |            5  |     42  |     42  |      718  |       42  |
;;         11586  |        1  |         5  |         1  |            5  |            5  |     43  |     43  |      708  |       43  |
;;         6794  |        5  |         5  |         4  |            1  |            5  |     15  |     15  |     1042  |       15  |
;;         616  |        1  |         5  |         4  |            1  |            5  |     20  |     20  |      991  |       20  |]

(into (sorted-map) (ts/value-counts resp "age_r"))
;; => {15 217,
;;     16 223,
;;     17 234,
;;     18 235,
;;     19 241,
;;     20 258,
;;     21 267,
;;     22 287,
;;     23 282,
;;     24 269,
;;     25 267,
;;     26 260,
;;     27 255,
;;     28 252,
;;     29 262,
;;     30 292,
;;     31 278,
;;     32 273,
;;     33 257,
;;     34 255,
;;     35 262,
;;     36 266,
;;     37 271,
;;     38 256,
;;     39 215,
;;     40 256,
;;     41 250,
;;     42 215,
;;     43 253,
;;     44 235}

;; Minimum and maximum age
((juxt first last) (sort (keys (ts/value-counts resp "age_r"))))
;; => [15 44]


;; select row id for given case-id
(def resp-case2298 (.rows resp (int-array (.isEqualTo (ts/column resp "caseid") "2298"))))

(ts/shape resp-case2298)
;; => {:columns 3087, :rows 1}

;; select row id for given case-id, for preg
(def preg-case2298 (.rows preg (int-array (.isEqualTo (ts/column preg "caseid") "2298"))))

(ts/shape preg-case2298)
;; => {:columns 244, :rows 4}

;; How old is the respondent with caseid 1?
(let [resp-ids (.isEqualTo (ts/column resp "caseid") "1")]
  (first (ts/select-values resp "age_r" resp-ids)))
;; => 44

;; What are the pregnancy lengths for the respondent with caseid 2298?
(let [resp-ids (.isEqualTo (ts/column preg "caseid") "2298")]
  (ts/select-values preg "prglngth" resp-ids))
;; => (40 36 30 40)

;; What was the birthweight of the first baby born to the respondent with caseid 5012?
(let [resp-ids (.isEqualTo (ts/column preg "caseid") "5012")]
  (first (ts/select-values preg "totalwgt_lb" resp-ids)))
;; => 6.0
