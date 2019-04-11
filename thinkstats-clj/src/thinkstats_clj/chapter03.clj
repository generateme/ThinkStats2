(ns thinkstats-clj.chapter03
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

;; as counts
(show (plot/histogram-discrete (ts/select-values preg "birthwgt_lb" live-ids)
                               {:xlabel "Birth weight (pounds)" :ylabel "Count"}))

;; as pmf
(show (plot/histogram-discrete (ts/select-values preg "birthwgt_lb" live-ids)
                               {:xlabel "Birth weight (pounds)" :ylabel "Count" :percents? true}))



;; pmf
(def pmf (rnd/distribution :enumerated-int {:data [1 2 2 3 5]}))
(rnd/pdf pmf 2)
;; => 0.4

;;

(def pregnancy-length (map #(int (m/floor %)) (ts/select-values preg "prglngth" live-ids)))

(show (plot/histogram-discrete pregnancy-length {:xlabel "Pregnancy length (weeks)" :ylabel "Pmf" :percents? true}))
(show (plot/pmf-int pregnancy-length {:xlabel "Pregnancy length (weeks)" :ylabel "Pmf" :samples 500 :stroke {:size 3}}))

(def firsts (set/intersection live-ids (set (.isEqualTo (ts/column preg "birthord") 1.0))))
(def others (set/intersection live-ids (set (.isNotEqualTo (ts/column preg "birthord") 1.0))))

(def pregnancy-length-first (ts/select-values preg "prglngth" firsts))
(def pregnancy-length-other (ts/select-values preg "prglngth" others))

(def first-pmf (rnd/distribution :enumerated-int {:data pregnancy-length-first}))
(def other-pmf (rnd/distribution :enumerated-int {:data pregnancy-length-other}))

(show (plot/histogram [pregnancy-length-first pregnancy-length-other]
                      {:xlabel "Pregnancy length(weeks)" :ylabel "Count" :bins 50 :percents? true :x {:domain [27 46]}}))

(show (plot/pmf-int [pregnancy-length-first pregnancy-length-other]
                    {:xlabel "Pregnancy length(weeks)" :stroke {:size 3} :samples 500 :ylabel "Count" :domain [27 46]}))


(def preg-diffs (map #(let [p1 (rnd/pdf first-pmf %)
                            p2 (rnd/pdf other-pmf %)]
                        [% (* 100.0 (- p1 p2))]) (range 35 46)))

(show (plot/vbar preg-diffs {:xlabel "Pregnancy length(weeks)" :ylabel "Difference (percentage points)"}))

;; class sizes
(def d {7 8, 12 8, 17 14, 22 4, 27 6, 32 12, 37 8, 42 3, 47 2})

(stats/mean d-raw)
;; => 23.692307692307693

;; we need to gather raw data from frequencies
(def d-pmf (rnd/distribution :enumerated-int {:data (mapcat (fn [[k v]] (repeat v k)) d)}))

(def biased-pmf
  (let [biased-probs (map #(* % (rnd/pdf d-pmf %)) (keys d))]
    (rnd/distribution :enumerated-int {:data (keys d) :probabilities biased-probs})))

(rnd/mean d-pmf)
;; => 23.6923076923077

(rnd/mean biased-pmf)
;; => 29.123376623376625

(def unbiased-pmf
  (let [biased-probs (map #(/ (rnd/pdf biased-pmf %) %) (keys d))]
    (rnd/distribution :enumerated-int {:data (keys d) :probabilities biased-probs})))

(rnd/mean unbiased-pmf)
;; => 23.69230769230769

(show (plot/functions {:regular (partial rnd/pdf d-pmf)
                       :biased (partial rnd/pdf biased-pmf)}
                      {:xlabel "class size" :stroke {:size 3} :samples 500 :ylabel "PMF" :domain [6 48]}))

(defn build-pmf-points
  ([pmf ks] (build-pmf-points pmf ks nil))
  ([pmf ks extra] (map #(vector % (rnd/pdf pmf %)) (sort (concat extra ks)))))

(show (plot/lines {:regular (build-pmf-points d-pmf (keys d) [6 48])
                   :biased (build-pmf-points biased-pmf (keys d) [6 48])}
                  {:xlabel "class size" :ylabel "PMF" :stroke {:size 3} :area? true :interpolation in/step-after}))


(show (plot/lines {:regular (build-pmf-points d-pmf (keys d) [6 48])
                   :unbiased (build-pmf-points unbiased-pmf (keys d) [6 48])}
                  {:xlabel "class size" :ylabel "PMF" :stroke {:size 3} :interpolation in/step-after}))

;;

(def A (ts/double-column "A" (repeatedly 4 rand)))
(def B (ts/double-column "B" (repeatedly 4 rand)))

(def df (ts/columns->table "DF" [A B]))

(ts/shape df)
;; => {:columns 2, :rows 4}

(println df)
;; #object[tech.tablesaw.api.Table 0x583dfa0d                       DF                       
;;         A           |           B           |
;;         -----------------------------------------------
;;         0.05361782177063856  |  0.03300586798260763  |
;;         0.588401022515685  |  0.06113687619219055  |
;;         0.33809777615269954  |   0.9233553560506165  |
;;         0.06817524190143731  |  0.18856099600248988  |]


(.columnNames df)
;; => ["A" "B"]

(println (ts/rows df [0 1]))
;; #object[tech.tablesaw.api.Table 0x408281ee                       DF                       
;;         A           |           B           |
;;         -----------------------------------------------
;;         0.05361782177063856  |  0.03300586798260763  |
;;         0.588401022515685  |  0.06113687619219055  |]


(seq (ts/column df "A"))
;; => (0.05361782177063856 0.588401022515685 0.33809777615269954 0.06817524190143731)

(seq (ts/column df "B"))
;; => (0.03300586798260763 0.06113687619219055 0.9233553560506165 0.18856099600248988)

(type (ts/column df "A"))
;; => tech.tablesaw.api.DoubleColumn

;; get from [0,0]
(.get (ts/column df 0) 0)
;; => 0.05361782177063856

;;;;;;;;;;;; Excercises

(def resp (nsfg/fem-resp))

(def numkdhh (ts/column resp "numkdhh"))
(def numkdhh-vals (distinct (seq numkdhh)))

(show (plot/histogram-discrete numkdhh {:xlabel "number of childern" :ylabel "pmf" :percents? true}))

(def numkdhh-pmf (rnd/distribution :enumerated-int {:data numkdhh}))

(def biased-numkdhh-pmf
  (let [biased-probs (map #(* % (rnd/pdf numkdhh-pmf %)) numkdhh-vals)]
    (rnd/distribution :enumerated-int {:data numkdhh-vals :probabilities biased-probs})))

(stats/mean numkdhh)
;; => 1.0242051550438314
(rnd/mean numkdhh-pmf)
;; => 1.024205155043831
(stats/variance numkdhh)
;; => 1.4130492078405363
(stats/population-variance numkdhh)
;; => 1.4128643263531833
(rnd/variance numkdhh-pmf)
;; => 1.4128643263531195

(rnd/mean biased-numkdhh-pmf)
;; => 2.403679100664282

(show (plot/lines {:regular (build-pmf-points numkdhh-pmf numkdhh-vals)
                   :biased (build-pmf-points biased-numkdhh-pmf numkdhh-vals)}
                  {:xlabel "number-of-childern" :ylabel "PMF" :stroke {:size 3} :area? true :interpolation in/step-after}))

;;

(def pregnancy-length>37 (set (.isGreaterThanOrEqualTo (ts/column preg "prglngth") 37.0)))

(def more-kids-ids (->> (nsfg/preg-map preg (set/intersection pregnancy-length>37 live-ids)) 
                        (filter (comp #(> % 1) count second))
                        (map second)))

(def prglngth (ts/column preg "prglngth"))

(defn differences
  [ks]
  (let [f (.get prglngth (first ks))]
    (map #(- f (.get prglngth %)) (rest ks))))

(def diffs (mapcat differences more-kids-ids))

(stats/mean diffs)
;; => 0.1885057471264353

(show (plot/histogram-discrete diffs {:xlabel "difference in weeks" :ylabel "PMF" :bins 30 :percents? true}))


;;;;;;;;;;;;;;;;;;

;; TODO last excercise
