(ns thinkstats-clj.nsfg
  (:require [thinkstats-clj.data.tablesaw :as ts]
            [thinkstats-clj.data.stata :as stata]))

;; Create dataset about pregnancy and clean data

(defn- fem-resp- "Read 2002FemResp dataset." []
  (stata/read-data->dataset "2002FemResp" "../code/2002FemResp.dct" "../code/2002FemResp.dat.gz")) ;; read from Stata file

(def fem-resp (memoize fem-resp-))

(defn- to-nan-fn
  "Return NaN if predicate is true"
  [pred]
  (fn [col v]
    (if (pred v) (ts/nan-value col) v)))

(defn- fem-preg- "Read and clean 2002FemPreg dataset." []
  (let [preg (stata/read-data->dataset "2002FemPreg" "../code/2002FemPreg.dct" "../code/2002FemPreg.dat.gz") ;; read from Stata
        agepreg (ts/update-column (ts/column preg "agepreg") :divide 100.0) ;; divide pregnancy age by 100.0
        birthwgt-lb (ts/fmap preg "birthwgt_lb"  (to-nan-fn #(> % 20))) ;; remove weight greater than 20lb
        birthwgt-oz (ts/fmap preg "birthwgt_oz" (to-nan-fn #(<= 97 % 99))) ;; remove special cases
        hpagelb (ts/fmap preg "hpagelb" (to-nan-fn #(<= 97 % 99))) ;; same here
        babysex (ts/fmap preg "babysex" (to-nan-fn #(or (== % 7) (== % 9)))) ;; same here
        nbrnaliv (ts/fmap preg "nbrnaliv" (to-nan-fn #(== % 9))) ;; same here
        totalwgt-lb (ts/double-column "totalwgt_lb" (map #(if (or (neg? %1) (neg? %2))
                                                            ##NaN
                                                            (+ %1 (/ %2 16.0))) birthwgt-lb birthwgt-oz))] ;; combine weight into one value

    (ts/replace-columns! preg agepreg birthwgt-lb birthwgt-oz hpagelb babysex nbrnaliv) ;; replace old columns
    (ts/add-columns! preg totalwgt-lb) ;; and new column
    
    preg))

(def fem-preg (memoize fem-preg-))

(defn preg-map
  "Calculate pregancy indices for each case"
  ([preg] (preg-map preg nil))
  ([preg limit]
   (reduce (fn [m [id v]]
             (if (and limit (not (limit id)))
               m
               (if (contains? m v)
                 (update m v conj id)
                 (assoc m v [id])))) {} (map-indexed vector (ts/column preg "caseid")))))
