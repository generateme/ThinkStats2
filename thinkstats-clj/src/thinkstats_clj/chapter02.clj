(ns thinkstats-clj.chapter02
  (:require [thinkstats-clj.data.tablesaw :as ts]
            [thinkstats-clj.data.stata :as stata]
            [thinkstats-clj.nsfg :as nsfg]
            [thinkstats-clj.plot :refer :all]
            [cljplot.core :refer :all]
            [fastmath.stats :as stats]
            [fastmath.random :as rnd]
            [fastmath.core :as m]
            [clojure.set :as set]))


(def t [1 2 2 3 5])

;; histograms in fastmath work on continuous range, but you can specify how many bins you need
(stats/histogram t 5)
;; => {:size 5, :step 0.8, :samples 5, :min 1.0, :max 5.0, :bins ([1.0 1 0.2] [1.8 2 0.4] [2.6 1 0.2] [3.4000000000000004 0 0.0] [4.2 1 0.2])}

;; for counting values use frequencies
(def hist (frequencies t))

hist
;; => {1 1, 2 2, 3 1, 5 1}

(hist 2)
;; => 2

(hist 4)
;; => nil
(get hist 4 0)
;; => 0

(keys hist)
;; => (1 2 3 5)

(show (histogram t {:bins 5 :xlabel "value" :ylabel "frequency" :percents? false}))
(show (histogram-discrete t {:xlabel "value" :ylabel "frequency"}))

;;

(def preg (nsfg/fem-preg))
(def live-ids (set (.isEqualTo (ts/column preg "outcome") 1.0)))

(show (histogram-discrete (ts/select-values preg "birthwgt_lb" live-ids)
                          {:xlabel "Birth weight (pounds)" :ylabel "Count"}))

(show (histogram-discrete (ts/select-values preg "birthwgt_oz" live-ids)
                          {:xlabel "Birth weight (ounces)" :ylabel "Count"}))


(def ages (ts/select-values preg "agepreg" live-ids))
(show (histogram ages {:percents? false :bins 20 :xlabel "years" :ylabel "Count" :y {:fmt int} :x {:fmt int}}))
(def ages-int (map #(int (m/floor %)) ages))
(show (histogram-discrete ages-int {:xlabel "years" :ylabel "Count"}))

(def pregnancy-length (map #(int (m/floor %)) (ts/select-values preg "prglngth" live-ids)))
(show (histogram-discrete pregnancy-length {:y {:fmt int} :xlabel "years" :ylabel "Count"}))

(take 10 (sort-by first (frequencies ages-int)))
;; => ([10 2]
;;     [11 1]
;;     [12 1]
;;     [13 14]
;;     [14 43]
;;     [15 128]
;;     [16 242]
;;     [17 398]
;;     [18 546]
;;     [19 559])

;; longest pregnancy lengths
(take-last 10 (sort-by first (frequencies pregnancy-length)))
;; => ([40 1116]
;;     [41 587]
;;     [42 328]
;;     [43 148]
;;     [44 46]
;;     [45 10]
;;     [46 1]
;;     [47 1]
;;     [48 7]
;;     [50 2])

;;

(def firsts (set/intersection live-ids (set (.isEqualTo (ts/column preg "birthord") 1.0))))
(def others (set/intersection live-ids (set (.isNotEqualTo (ts/column preg "birthord") 1.0))))

(def pregnancy-length-first (ts/select-values preg "prglngth" firsts))
(def pregnancy-length-other (ts/select-values preg "prglngth" others))

(show (histogram [pregnancy-length-first pregnancy-length-other]
                 {:xlabel "weeks" :ylabel "Count" :bins 50 :percents? false :y {:fmt int} :x {:domain [27 46]}}))

;; mean of pregnancy length
(stats/mean pregnancy-length)
;; => 38.56055968517709

(stats/stddev pregnancy-length)
;; => 2.7023438100705963

(stats/variance pregnancy-length)
;; => 7.302662067826868

[(stats/mean pregnancy-length-first) (stats/mean pregnancy-length-other)]
;; => [38.60095173351461 38.52291446673706]

(- (stats/mean pregnancy-length-first) (stats/mean pregnancy-length-other))
;; => 0.07803726677754952

(defn cohen-effect-size
  "Computes Cohen's effect size for two groups"
  [group1 group2]
  (let [group1 (m/seq->double-array group1)
        group2 (m/seq->double-array group2)
        diff (- (stats/mean group1) (stats/mean group2))
        var1 (stats/variance group1)
        var2 (stats/variance group2)
        n1 (alength group1)
        n2 (alength group2)
        pooled-var (/ (+ (* n1 var1) (* n2 var2)) (+ n1 n2))]
    (/ diff (m/sqrt pooled-var))))

(cohen-effect-size pregnancy-length-first pregnancy-length-other)
;; => 0.028879044654449862

;;;;;;
;; EXCERCISES

(def weight-first (ts/select-values preg "totalwgt_lb" firsts))
(def weight-other (ts/select-values preg "totalwgt_lb" others))

(show (histogram [weight-first weight-other]
                 {:xlabel "weight (lbs)" :ylabel "Count" :bins 30 :percents? false :y {:fmt int}}))

(- (stats/mean weight-first) (stats/mean weight-other))
;; => -0.12476118453548768

(cohen-effect-size weight-first weight-other)
;; => -0.0886723633320275

(def resp (nsfg/fem-resp))

(show (histogram-discrete (ts/column resp "totincr") {:xlabel "total income" :ylabel "Count"}))
(show (histogram-discrete (ts/column resp "age_r") {:xlabel "respondent age" :ylabel "Count"}))
(show (histogram-discrete (ts/column resp "numfmhh") {:xlabel "number of people in household" :ylabel "Count"}))
(show (histogram-discrete (ts/column resp "parity") {:xlabel "number of childern born" :ylabel "Count"}))

(take-last 4 (sort-by first (frequencies (ts/column resp "parity"))))
;; => ([9 2] [10 3] [16 1] [22 1])

(def parities-max-income (ts/select-values resp "parity" (.isEqualTo (ts/column resp "totincr") 14.0)))
(def parities-other-income (ts/select-values resp "parity" (.isNotEqualTo (ts/column resp "totincr") 14.0)))

(show (histogram-discrete parities-max-income {:xlabel "number of childern born (income >= $75k)" :ylabel "Count"}))
(show (histogram-discrete parities-other-income {:xlabel "number of childern born (income <= $75k)" :ylabel "Count"}))

(take-last 4 (sort-by first (frequencies parities-max-income)))
;; => ([4 19] [5 5] [7 1] [8 1])

(- (stats/mean parities-max-income) (stats/mean parities-other-income))
;; => -0.17371374470099554

(cohen-effect-size parities-max-income parities-other-income)
;; => -0.12511855314660628

