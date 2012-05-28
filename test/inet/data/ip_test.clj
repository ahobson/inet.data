(ns inet.data.ip-test
  (:require [inet.data.ip :as ip])
  (:use [clojure.test])
  (:import [java.net InetAddress]))

(deftest test-address-validation
  (testing "Validation"
    (testing "of IPv4 addresses which are"
      (is (= true (ip/address? "192.168.1.1")) "valid")
      (is (= false (ip/address? "8.x.17.y")) "invalid, non-numeric")
      (is (= false (ip/address? "8.8.256.7")) "invalid, numeric"))
    (testing "of IPv6 addresses which are"
      (is (= true (ip/address? "17:fe77::1899:12")) "valid")
      (is (= false (ip/address? "17::qq")) "invalid, non-numeric")
      (is (= false (ip/address? "17::18::ae")) "invalid, numeric"))))

(deftest test-address-roundtrip
  (testing "Round-tripping"
    (let [addr "172.12.16.1"]
      (is (= addr (-> addr ip/address str)) "IPv4 address strings"))
    (let [addr "fe:1100::1"]
      (is (= addr (-> addr ip/address str)) "IPv6 address strings"))))

(deftest test-network
  (testing "Creating networks"
    (testing "from IPv4 addresses"
      (let [addr-str   "192.168.0.0"
            addr-obj   (InetAddress/getByName addr-str)
            addr-bytes (.getAddress addr-obj)
            test-data [[addr-str "string"]
                       [addr-obj "InetAddress"]
                       [addr-bytes "bytes"]]]
        (doseq [[addr src] test-data]
          (is (= "192.168.0.0/32" (-> addr ip/network str))
              (format "From %s with implied prefix-length." src))
          (is (= "192.168.0.0/16" (-> addr (ip/network 16) str))
              (format "From %s with explicit prefix-length." src)))))
    (testing "from IPv6 addresses"
      (let [addr-str   "fe:11::"
            addr-obj   (InetAddress/getByName addr-str)
            addr-bytes (.getAddress addr-obj)
            test-data  [[addr-str "string"]
                        [addr-obj "InetAddress"]
                        [addr-bytes "bytes"]]]
        (doseq [[addr src] test-data]
          (is (= "fe:11::/128" (-> addr ip/network str))
              (format "From %s with implied prefix-length." src))
          (is (= "fe:11::/32" (-> addr (ip/network 32) str))
              (format "From %s with explicit prefix-length." src)))))))

(deftest test-network-contains
  (testing "Network does contain address"
    (is (ip/network-contains? "192.168.0.0/16" "192.168.13.37"))
    (is (ip/network-contains? "192.168.0.0/17" "192.168.127.1")))
  (testing "Network doesn't contain address"
    (is (not (ip/network-contains? "192.168.0.0/16" "8.8.8.8")))
    (is (not (ip/network-contains? "192.168.0.0/17" "192.168.128.1")))))

(deftest test-network-set
  (testing "Sets of networks"
    (let [networks (->> (range 0 256) (map #(ip/network (str "10.0.0." %)))
                        (apply ip/network-set))]
      (is (contains? networks "10.0.0.1"))
      (is (not (contains? networks "10.0.1.1"))))))
