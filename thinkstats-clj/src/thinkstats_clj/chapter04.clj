(ns thinkstats-clj.chapter04
  (:require [thinkstats-clj.data.tablesaw :as ts]
            [thinkstats-clj.data.stata :as stata]
            [thinkstats-clj.nsfg :as nsfg]
            [thinkstats-clj.plot :as plot]
            [cljplot.core :refer :all]
            [fastmath.stats :as stats]
            [fastmath.random :as rnd]
            [fastmath.core :as m]
            [fastmath.interpolation :as in]
            [clojure.set :as set]
            [cljplot.render :as r]
            [clojure2d.color :as c]))


(def preg (nsfg/fem-preg))
(def live-ids (set (.isEqualTo (ts/column preg "outcome") 1.0)))
(def firsts (set/intersection live-ids (set (.isEqualTo (ts/column preg "birthord") 1.0))))
(def others (set/intersection live-ids (set (.isNotEqualTo (ts/column preg "birthord") 1.0))))

(def first-wgt (ts/select-values preg "totalwgt_lb" firsts true))
(def first-wgt-dropna (ts/select-values preg "totalwgt_lb" firsts))

(def other-wgt (ts/select-values preg "totalwgt_lb" others true))
(def other-wgt-dropna (ts/select-values preg "totalwgt_lb" others))

[(count first-wgt) (count first-wgt-dropna)]
;; => [4413 4363]
[(count other-wgt) (count other-wgt-dropna)]
;; => [4735 4675]

(show (plot/histogram [first-wgt-dropna other-wgt-dropna] {:xlabel "Weight (pounds)" :ylabel "PMF"}))

(def t [55 66 77 88 99])

(stats/percentile t 50)
;; => 77.0

(def pregnancy-length (ts/select-values preg "prglngth" live-ids))

(show (plot/cdf pregnancy-length {:stroke {:size 3} :samples 500 :xlabel "Pregnancy length (weeks)" :ylabel "CDF"}))

(def cdf (rnd/distribution :empirical {:data pregnancy-length}))

(rnd/cdf cdf 41)
;; => 0.9406427634455619

(rnd/icdf cdf 0.5)
;; => 39.0

(show (plot/cdfs {:first first-wgt-dropna
                  :other other-wgt-dropna} {:stroke {:size 3} :samples 500 :xlabel "weight (pounds)" :ylabel "CDFs"}))

(def weights (ts/select-values preg "totalwgt_lb" live-ids))

;;
(def live-cdf (rnd/distribution :enumerated-real {:data (sort weights)}))

(stats/percentile weights 50)
;; => 7.375
(rnd/icdf live-cdf 0.5)
;; => 7.375

;; iqf
[(stats/percentile weights 25) (stats/percentile weights 75)]
;; => [6.5 8.125]
[(rnd/icdf live-cdf 0.25) (rnd/icdf live-cdf 0.75)]
;; => [6.5 8.125]

(rnd/cdf live-cdf 10.2)
;; => 0.9882717415357386

(let [ranks (map #(* 100.0 (rnd/cdf live-cdf %)) (repeatedly 100 #(rand-nth weights)))]
  (show (plot/cdf ranks {:stroke {:size 3} :samples 500 :xlabel "Percentile rank" :ylabel "CDF"})))

(show (plot/cdfs {:live weights
                  :resample (take 1000 (rnd/->seq live-cdf))}
                 {:stroke {:size 3} :samples 500 :xlabel "Birth weight (pounds)" :ylabel "CDF"}))

;;; excercises

;; well... sometimes enumerated works better than empirical...
(let [first-wgt-distr (rnd/distribution :enumerated-real {:data (sort first-wgt-dropna)})]
  (rnd/cdf first-wgt-distr (* 2.20462262 3.2)))

;;

(def rnd (repeatedly 1000 rnd/drand))

(show (plot/histogram rnd {:bins 100 :ylabel "PMF" :xlabel "uniform random" :percents? true :type :lollipops}))

(show (plot/cdf rnd {:stroke {:size 3} :samples 500 :xlabel "uniform random" :ylabel "CDF"}))
