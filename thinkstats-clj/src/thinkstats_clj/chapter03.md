
```clojure
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
```




```clojure
(def preg (nsfg/fem-preg))
(def live-ids (set (.isEqualTo (ts/column preg "outcome") 1.0)))
```



as counts
```clojure
(show (plot/histogram-discrete (ts/select-values preg "birthwgt_lb" live-ids)
                               {:xlabel "Birth weight (pounds)" :ylabel "Count"}))
```



as pmf
```clojure
(show (plot/histogram-discrete (ts/select-values preg "birthwgt_lb" live-ids)
                               {:xlabel "Birth weight (pounds)" :ylabel "Count" :percents? true}))
```



pmf


```clojure
(def pmf (rnd/distribution :enumerated-int {:data [1 2 2 3 5]}))
(rnd/pdf pmf 2)
```

Result:
```clojure
=> 0.4
```




```clojure
(def pregnancy-length (map #(int (m/floor %)) (ts/select-values preg "prglngth" live-ids)))
```




```clojure
(show (plot/histogram-discrete pregnancy-length {:xlabel "Pregnancy length (weeks)" :ylabel "Pmf" :percents? true}))
(show (plot/pmf-int pregnancy-length {:xlabel "Pregnancy length (weeks)" :ylabel "Pmf" :samples 500 :stroke {:size 3}}))
```




```clojure
(def firsts (set/intersection live-ids (set (.isEqualTo (ts/column preg "birthord") 1.0))))
(def others (set/intersection live-ids (set (.isNotEqualTo (ts/column preg "birthord") 1.0))))
```




```clojure
(def pregnancy-length-first (ts/select-values preg "prglngth" firsts))
(def pregnancy-length-other (ts/select-values preg "prglngth" others))
```




```clojure
(def first-pmf (rnd/distribution :enumerated-int {:data pregnancy-length-first}))
(def other-pmf (rnd/distribution :enumerated-int {:data pregnancy-length-other}))
```


```clojure
(show (plot/histogram [pregnancy-length-first pregnancy-length-other]
                      {:xlabel "Pregnancy length(weeks)" :ylabel "Count" :bins 50 :percents? true :x {:domain [27 46]}}))
```


```clojure
(show (plot/pmf-int [pregnancy-length-first pregnancy-length-other]
                    {:xlabel "Pregnancy length(weeks)" :stroke {:size 3} :samples 500 :ylabel "Count" :domain [27 46]}))
```


```clojure
(def preg-diffs (map #(let [p1 (rnd/pdf first-pmf %)
                            p2 (rnd/pdf other-pmf %)]
                        [% (* 100.0 (- p1 p2))]) (range 35 46)))
```


```clojure
(show (plot/vbar preg-diffs {:xlabel "Pregnancy length(weeks)" :ylabel "Difference (percentage points)"}))
```



class sizes
```clojure
(def d {7 8, 12 8, 17 14, 22 4, 27 6, 32 12, 37 8, 42 3, 47 2})
```



we need to gather raw data from frequencies
```clojure
(def d-pmf (rnd/distribution :enumerated-int {:data (mapcat (fn [[k v]] (repeat v k)) d)}))
```


```clojure
(def biased-pmf
  (let [biased-probs (map #(* % (rnd/pdf d-pmf %)) (keys d))]
    (rnd/distribution :enumerated-int {:data (keys d) :probabilities biased-probs})))
```


```clojure
(rnd/mean d-pmf)
```

Result:
```clojure
=> 23.6923076923077
```


```clojure
(rnd/mean biased-pmf)
```

Result:
```clojure
=> 29.123376623376625
```


```clojure
(def unbiased-pmf
  (let [biased-probs (map #(/ (rnd/pdf biased-pmf %) %) (keys d))]
    (rnd/distribution :enumerated-int {:data (keys d) :probabilities biased-probs})))
```


```clojure
(rnd/mean unbiased-pmf)
```

Result:
```clojure
=> 23.69230769230769
```


```clojure
(show (plot/functions {:regular (partial rnd/pdf d-pmf)
                       :biased (partial rnd/pdf biased-pmf)}
                      {:xlabel "class size" :stroke {:size 3} :samples 500 :ylabel "PMF" :domain [6 48]}))
```


```clojure
(defn build-pmf-points
  ([pmf ks] (build-pmf-points pmf ks nil))
  ([pmf ks extra] (map #(vector % (rnd/pdf pmf %)) (sort (concat extra ks)))))
```


```clojure
(show (plot/lines {:regular (build-pmf-points d-pmf (keys d) [6 48])
                   :biased (build-pmf-points biased-pmf (keys d) [6 48])}
                  {:xlabel "class size" :ylabel "PMF" :stroke {:size 3} :area? true :interpolation in/step-after}))
```


```clojure
(show (plot/lines {:regular (build-pmf-points d-pmf (keys d) [6 48])
                   :unbiased (build-pmf-points unbiased-pmf (keys d) [6 48])}
                  {:xlabel "class size" :ylabel "PMF" :stroke {:size 3} :interpolation in/step-after}))
```






```clojure
(def A (ts/double-column "A" (repeatedly 4 rand)))
(def B (ts/double-column "B" (repeatedly 4 rand)))
```


```clojure
(def df (ts/columns->table "DF" [A B]))
```


```clojure
(ts/shape df)
```

Result:
```clojure
=> {:columns 2, :rows 4}
```


```clojure
(println df)
```

        A           |           B           |
        -----------------------------------------------
        0.05361782177063856  |  0.03300586798260763  |
        0.588401022515685  |  0.06113687619219055  |
        0.33809777615269954  |   0.9233553560506165  |
        0.06817524190143731  |  0.18856099600248988  |


```clojure
(.columnNames df)
```

Result:
```clojure
=> ["A" "B"]
```


```clojure
(println (ts/rows df [0 1]))
```

#object[tech.tablesaw.api.Table 0x408281ee                       DF                       
        A           |           B           |
        -----------------------------------------------
        0.05361782177063856  |  0.03300586798260763  |
        0.588401022515685  |  0.06113687619219055  |]


```clojure
(seq (ts/column df "A"))
```

Result:
```clojure
=> (0.05361782177063856 0.588401022515685 0.33809777615269954 0.06817524190143731)
```


```clojure
(seq (ts/column df "B"))
```

Result:
```clojure
=> (0.03300586798260763 0.06113687619219055 0.9233553560506165 0.18856099600248988)
```


```clojure
(type (ts/column df "A"))
```

Result:
```clojure
=> tech.tablesaw.api.DoubleColumn
```



get from [0,0]
```clojure
(.get (ts/column df 0) 0)
```

Result:
```clojure
=> 0.05361782177063856
```

Excercises


```clojure
(def resp (nsfg/fem-resp))
```




```clojure
(def numkdhh (ts/column resp "numkdhh"))
(def numkdhh-vals (distinct (seq numkdhh)))
```


```clojure
(show (plot/histogram-discrete numkdhh {:xlabel "number of childern" :ylabel "pmf" :percents? true}))
```


```clojure
(def numkdhh-pmf (rnd/distribution :enumerated-int {:data numkdhh}))
```


```clojure
(def biased-numkdhh-pmf
  (let [biased-probs (map #(* % (rnd/pdf numkdhh-pmf %)) numkdhh-vals)]
    (rnd/distribution :enumerated-int {:data numkdhh-vals :probabilities biased-probs})))
```


```clojure
(stats/mean numkdhh)
```



=> 1.0242051550438314
```clojure
(rnd/mean numkdhh-pmf)
```



=> 1.024205155043831
```clojure
(stats/variance numkdhh)
```



=> 1.4130492078405363
```clojure
(stats/population-variance numkdhh)
```



=> 1.4128643263531833
```clojure
(rnd/variance numkdhh-pmf)
```

Result:
```clojure
=> 1.4128643263531195
```


```clojure
(rnd/mean biased-numkdhh-pmf)
```

Result:
```clojure
=> 2.403679100664282
```


```clojure
(show (plot/lines {:regular (build-pmf-points numkdhh-pmf numkdhh-vals)
                   :biased (build-pmf-points biased-numkdhh-pmf numkdhh-vals)}
                  {:xlabel "number-of-childern" :ylabel "PMF" :stroke {:size 3} :area? true :interpolation in/step-after}))
```




```clojure
(def pregnancy-length>37 (set (.isGreaterThanOrEqualTo (ts/column preg "prglngth") 37.0)))
```


```clojure
(def more-kids-ids (->> (nsfg/preg-map preg (set/intersection pregnancy-length>37 live-ids)) 
                        (filter (comp #(> % 1) count second))
                        (map second)))
```


```clojure
(def prglngth (ts/column preg "prglngth"))
```


```clojure
(defn differences
  [ks]
  (let [f (.get prglngth (first ks))]
    (map #(- f (.get prglngth %)) (rest ks))))
```


```clojure
(def diffs (mapcat differences more-kids-ids))
```


```clojure
(stats/mean diffs)
```

Result:
```clojure
=> 0.1885057471264353
```


```clojure
(show (plot/histogram-discrete diffs {:xlabel "difference in weeks" :ylabel "PMF" :bins 30 :percents? true}))
```



TODO last excercise

