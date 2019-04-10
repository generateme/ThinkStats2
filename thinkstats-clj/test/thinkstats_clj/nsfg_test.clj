(ns thinkstats-clj.nsfg-test
  (:require [thinkstats-clj.nsfg :refer :all]
            [clojure.test :refer :all]
            [thinkstats-clj.data.tablesaw :as ts]))

(def resp-data (fem-resp))
(def preg-data (fem-preg))

(deftest fem-resp-tests
  (is (= 7643 (ts/size resp-data)))
  (is (= 1267 (get (ts/value-counts resp-data "pregnum") 1))))

(deftest fem-preg-tests
  (let [test-cols ["pregordr" "nbrnaliv" "babysex" "birthwgt_lb" "birthwgt_oz"
                   "prglngth" "outcome" "birthord" "agepreg" "totalwgt_lb"]
        test-keys [1 1 1 7 0 39 1 1 22.75 7.5]]
    (is (== 13593 (ts/size preg-data)))
    (is (= "12571" (.get (ts/column preg-data "caseid") 13592)))
    (is (= [5033 8981 4641 3049 1037 4744 9148 4413 100 302] (map #(get (ts/value-counts preg-data %1) %2) test-cols test-keys)))

    (let [m (ts/value-counts preg-data "finalwgt")
          mkey (reduce max (keys m))]
      (is (== 6 (m mkey))))))

(defn- validate-pregnum
  [resp preg]
  (let [pregmap (preg-map preg)]
    (every? (fn [[id pregnum]]
              (= (count (pregmap id)) pregnum)) (map vector (ts/column resp "caseid") (ts/column resp "pregnum")))))

(deftest validate-pregnum-test
  (is (validate-pregnum resp-data preg-data)))
