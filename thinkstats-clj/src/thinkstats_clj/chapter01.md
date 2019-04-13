

# Think Stats 2e

Clojure version of examples and excercises from [notebook](https://github.com/AllenDowney/ThinkStats2/blob/master/code/chap01ex.ipynb) and [book chapter](http://greenteapress.com/thinkstats2/html/thinkstats2002.html).
Please read the book/notebook before. 

Read [approach.md]() first for all information about assumptions and libraries used.

## Chapter 1

Namespaces used:

* `data.tablesaw` - simple wrapper for Tablesaw, java dataframe library
* `data.stata` - stata files interpreter and loader
* `nsfg` - loads and cleans NSFG datasets used here
* `fastmath.stats` - statistical functions
```clojure
(ns thinkstats-clj.chapter01
  (:require [thinkstats-clj.data.tablesaw :as ts]
            [thinkstats-clj.data.stata :as stata]
            [thinkstats-clj.nsfg :as nsfg]
            [fastmath.stats :as stats]))
```


Let's read `2002FemPreg.dat` from Stata file to Tablesaw table.
```clojure
(def preg (nsfg/fem-preg))
```


Check type of `preg`
```clojure
(type preg)
```

```clojure
=> tech.tablesaw.api.Table
```


Print shape of the dataset
```clojure
(ts/shape preg)
```

```clojure
=> {:columns 244, :rows 13593}
```


Print some rows and columns from dataset
```clojure
(println (ts/echo-md preg))
```
caseid|pregordr|howpreg_n|howpreg_p|moscurrp|...|finalwgt|secu_p|sest|cmintvw|totalwgt_lb
---|---|---|---|---|---|---|---|---|---|---
1|1|NaN|NaN|NaN|...|6448.271111704751|2|9|1231|8.8125
1|2|NaN|NaN|NaN|...|6448.271111704751|2|9|1231|7.875
2|1|NaN|NaN|NaN|...|12999.542264385902|2|12|1231|9.125
---|---|---|---|---|---|---|---|---|---|---
12571|3|NaN|NaN|NaN|...|6269.200988679606|1|78|1227|NaN
12571|4|NaN|NaN|NaN|...|6269.200988679606|1|78|1227|7.5
12571|5|NaN|NaN|NaN|...|6269.200988679606|1|78|1227|7.5



Print first 10 column names
```clojure
(take 10 (.columnNames preg))
```

```clojure
=> ("caseid" "pregordr" "howpreg_n" "howpreg_p" "moscurrp" "nowprgdk" "pregend1" "pregend2" "nbrnaliv" "multbrth")
```


Check type of second column
```clojure
(ts/column preg 1)
```

```clojure
=> #object[tech.tablesaw.api.ShortColumn 0x4e92a539 "Short column: pregordr"]
```


Let's bind `pregordr` column to a var


```clojure
(def pregordr (ts/column preg "pregordr"))
(type pregordr)
```

```clojure
=> tech.tablesaw.api.ShortColumn
```


What are first 20 values? Fortunately tablesaw columns are seqable.
```clojure
(take 20 pregordr)
```

```clojure
=> (1 2 1 2 3 1 2 3 1 2 1 1 2 3 1 2 3 1 2 1)
```


What is the length of `pregordr` column?
```clojure
(.size pregordr)
```

```clojure
=> 13593
```


Select first element by id. We have to explicitly call seq in this case.
```clojure
(nth (seq pregordr) 0)
```

```clojure
=> 1
```


Or let's call Java method
```clojure
(.get pregordr 0)
```

```clojure
=> 1
```


Select 3 elements from id=2
```clojure
(take 3 (drop 2 pregordr))
```

```clojure
=> (1 2 3)
```


`ts/value-counts` is the same as `Pandas` dataframe method. Returns frequencies of values. We need to sort them.
```clojure
(ts/value-counts (ts/column preg "outcome"))
```

```clojure
=> {1 9148, 2 1862, 3 120, 4 1921, 5 190, 6 352}
```


Count frequencies from table `brithwgt_lb` without missing values.
```clojure
(ts/value-counts (.removeMissing (ts/column preg "birthwgt_lb")))
```

```clojure
=> {0 8,
    1 40,
    2 53,
    3 98,
    4 229,
    5 697,
    6 2223,
    7 3049,
    8 1889,
    9 623,
    10 132,
    11 26,
    12 10,
    13 3,
    14 3,
    15 1}
```


`nsfg/preg-map` returns a dictionary of row ids for every pregnancy of respondent (identified by `caseid`). 
```clojure
(def preg-map (nsfg/preg-map preg))
```

```clojure
(take 10 preg-map)
```

```clojure
=> (["2828" [3180 3181]]
    ["281" [274 275 276]]
    ["8962" [9716]]
    ["3036" [3407 3408 3409]]
    ["4079" [4573]]
    ["11865" [12851 12852 12853 12854 12855]]
    ["1803" [2065 2066]]
    ["6607" [7296 7297]]
    ["7096" [7748]]
    ["12065" [13070]])
```
Select values from given column for `caseid`
```clojure
(defn select-for-case
  [colname caseid]
  (let [c (ts/column preg colname)]
    (map #(.get c %) (preg-map (str caseid)))))
```


select values from `outcome` for caseid `10229`
```clojure
(select-for-case "outcome" 10229)
```

```clojure
=> (4 4 4 4 4 4 1)
```
### Excercises



Order of births frequencies. Reference: [codebook](https://www.icpsr.umich.edu/nsfg6/Controller?displayPage=labelDetails&fileCode=PREG&section=A&subSec=8016&srtLabel=611933)
```clojure
(ts/value-counts (.removeMissing (ts/column preg "birthord")))
```

```clojure
=> {1 4413, 2 2874, 3 1234, 4 421, 5 126, 6 50, 7 20, 8 7, 9 2, 10 1}
```


How many missing values are there?
```clojure
(.countMissing (ts/column preg "birthord"))
```

```clojure
=> 4445
```


Check counts for given pregnancy length ranges. Reference: [codebook](https://www.icpsr.umich.edu/nsfg6/Controller?displayPage=labelDetails&fileCode=PREG&section=A&subSec=8016&srtLabel=611931)
```clojure
(let [cnts (->> (ts/column preg "prglngth")
                (group-by (fn [v]
                            (cond
                              (<= 0 v 13) "0-13"
                              (<= 14 v 26) "14-26"
                              :else "27-50")))
                (map (fn [[k v]] [k (count v)]))
                (into (sorted-map)))]
  (assoc cnts "Total" (reduce + (vals cnts))))
```

```clojure
=> {"0-13" 3522, "14-26" 793, "27-50" 9278, "Total" 13593}
```


Mean of total weight in lbs
```clojure
(stats/mean (.removeMissing (ts/column preg "totalwgt_lb")))
```

```clojure
=> 7.26562845762337
```


Create column with weight in kg. `fmap` returns tablesaw column of the same type as original one by calling `(.map ...)` under the hood.
```clojure
(def totalwgt-kg (ts/fmap (.removeMissing (ts/column preg "totalwgt_lb")) #(* 0.45359237 %2)))
```


Mean of total weight in kgs
```clojure
(stats/mean totalwgt-kg)
```

```clojure
=> 3.2956336316328283
```


Let's read response data now
```clojure
(def resp (nsfg/fem-resp))
```


Number of rows and columns
```clojure
(ts/shape resp)
```

```clojure
=> {:columns 3087, :rows 7643}
```

```clojure
(println (ts/echo-md resp))
```
caseid|rscrinf|rdormres|rostscrn|rscreenhisp|...|sest|cmintvw|cmlstyr|screentime|intvlngth
---|---|---|---|---|---|---|---|---|---|---
2298|1|5|5|1|...|18|1234|1222|18:26:36|110.49267
5012|1|5|1|5|...|18|1233|1221|16:30:59|64.294
11586|1|5|1|5|...|18|1234|1222|18:19:09|75.14917
---|---|---|---|---|---|---|---|---|---|---
5649|1|5|2|5|...|76|1228|1216|18:42:41|68.168
501|5|5|3|5|...|76|1228|1216|16:02:45|32.717335
10252|1|5|2|5|...|76|1230|1218|12:45:19|74.0615



Respondents age
```clojure
(ts/value-counts resp "age_r")
```

```clojure
=> {15 217,
    16 223,
    17 234,
    18 235,
    19 241,
    20 258,
    21 267,
    22 287,
    23 282,
    24 269,
    25 267,
    26 260,
    27 255,
    28 252,
    29 262,
    30 292,
    31 278,
    32 273,
    33 257,
    34 255,
    35 262,
    36 266,
    37 271,
    38 256,
    39 215,
    40 256,
    41 250,
    42 215,
    43 253,
    44 235}
```


Minimum and maximum age
```clojure
((juxt first last) (sort (keys (ts/value-counts resp "age_r"))))
```

```clojure
=> [15 44]
```


Select row id for given case-id from `resp` data.
```clojure
(def resp-case2298 (.rows resp (int-array (.isEqualTo (ts/column resp "caseid") "2298"))))
```

```clojure
(ts/shape resp-case2298)
```

```clojure
=> {:columns 3087, :rows 1}
```

```clojure
(println (ts/echo-md resp-case2298))
```
caseid|rscrinf|rdormres|rostscrn|rscreenhisp|...|sest|cmintvw|cmlstyr|screentime|intvlngth
---|---|---|---|---|---|---|---|---|---|---
2298|1|5|5|1|...|18|1234|1222|18:26:36|110.49267



select row id for given case-id from `preg` data
```clojure
(def preg-case2298 (.rows preg (int-array (.isEqualTo (ts/column preg "caseid") "2298"))))
```

```clojure
(ts/shape preg-case2298)
```

```clojure
=> {:columns 244, :rows 4}
```

```clojure
(println (ts/echo-md preg-case2298))
```
caseid|pregordr|howpreg_n|howpreg_p|moscurrp|...|finalwgt|secu_p|sest|cmintvw|totalwgt_lb
---|---|---|---|---|---|---|---|---|---|---
2298|1|NaN|NaN|NaN|...|5556.717241429314|2|18|1234|6.875
2298|2|NaN|NaN|NaN|...|5556.717241429314|2|18|1234|5.5
2298|3|NaN|NaN|NaN|...|5556.717241429314|2|18|1234|4.1875
2298|4|NaN|NaN|NaN|...|5556.717241429314|2|18|1234|6.875



How old is the respondent with caseid 1?
```clojure
(let [resp-ids (.isEqualTo (ts/column resp "caseid") "1")]
  (first (ts/select-values resp "age_r" resp-ids)))
```

```clojure
=> 44
```


What are the pregnancy lengths for the respondent with caseid 2298?
```clojure
(let [resp-ids (.isEqualTo (ts/column preg "caseid") "2298")]
  (ts/select-values preg "prglngth" resp-ids))
```

```clojure
=> (40 36 30 40)
```


What was the birthweight of the first baby born to the respondent with caseid 5012?
```clojure
(let [resp-ids (.isEqualTo (ts/column preg "caseid") "5012")]
  (first (ts/select-values preg "totalwgt_lb" resp-ids)))
```

```clojure
=> 6.0
```
