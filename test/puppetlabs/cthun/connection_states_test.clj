(ns puppetlabs.cthun.connection-states-test
  (require [clojure.test :refer :all]
           [puppetlabs.cthun.connection-states :refer :all]
           [puppetlabs.kitchensink.core :as ks]))


; private symbols
(def get-endpoint-string #'puppetlabs.cthun.connection-states/get-endpoint-string)
(def new-socket #'puppetlabs.cthun.connection-states/new-socket)
(def process-login-message #'puppetlabs.cthun.connection-states/process-login-message)
(def process-server-message  #'puppetlabs.cthun.connection-states/process-server-message)
(def logged-in?  #'puppetlabs.cthun.connection-states/logged-in?)
(def login-message?  #'puppetlabs.cthun.connection-states/login-message?)

(deftest get-endpoint-string-test
  (testing "It creates a correct endpoint string"
    (is (re-matches #"cth://localhost/controller/.*" (get-endpoint-string "localhost" "controller")))))

(deftest new-socket-test
  (testing "It returns a map that matches represents a new socket"
    (let [socket (new-socket)]
      (is (= (:socket-type socket) "undefined"))
      (is (= (:status socket) "connected"))
      (is (= (:user socket) "undefined"))
      (is (= (:endpoint socket) "undefined"))
      (is (not= nil (ks/datetime? (:created-at socket)))))))

(deftest process-login-message-test
  (with-redefs [puppetlabs.cthun.validation/validate-login-data (fn [data] true)]
    (testing "It should perform a login"
      (add-connection "ws" "localhost")
      (process-login-message "localhost" 
                             "ws"
                               {:data {
                                        :type "controller"
                                        :user "testing"
                                        }})
      (let [connection (get (get @connection-map "localhost") "ws")]
        (is (= (:socket-type connection) "controller"))
        (is (= (:status connection) "ready"))
        (is (= (:user connection) "testing"))
        (is (re-matches #"cth://localhost/controller/.*" (:endpoint connection))))))


  (testing "It does not allow a login to happen twice on the same socket"
    (with-redefs [puppetlabs.cthun.validation/validate-login-data (fn [data] true)]
      (add-connection "localhost" "ws")
      (process-login-message "localhost" 
                             "ws"
                             {:data {
                                     :type "controller"
                                     :user "testing"
                                     }}))
      (is (thrown? Exception  (process-login-message "localhost" 
                                                     "ws"
                                                     {:data {
                                                             :type "controller"
                                                             :user "testing"
                                                             }})))))

(deftest process-server-message-test
  (with-redefs [puppetlabs.cthun.connection-states/process-login-message (fn [host ws message-body] true)]
    (testing "It should identify a login message from the data schema"
      (is (= (process-server-message "localhost" "w" {:data_schema "http://puppetlabs.com/loginschema"}) true)))
    (testing "It should not process an unkown type of server message"
      (is (= (process-server-message "localhost" "w" {:data_schema "http://puppetlabs.com"}) nil)))))

(deftest logged-in?-test
    (testing "It returns true if the websocket is logged in"
      (swap! connection-map assoc-in ["localhost" "ws"] {:status "ready"})
      (is (= (logged-in? "localhost" "ws") true)))
    (testing "It returns false if the websocket is not logged in"
      (swap! connection-map assoc-in ["localhost" "ws"] {:status "connected"})))

(deftest login-message?-test
  (testing "It returns true when passed a login type messge"
    (is (= (login-message? {:endpoints ["cth://server"] :data_schema "http://puppetlabs.com/loginschema"}))))
  (testing "It returns false when passed a message of an unknown type"
    (is (= (login-message? {:endpoints ["cth://otherserver"] :data_schema "http://puppetlabs.com/loginschema"})))))

(deftest add-connection-test
  (testing "It should add a connection to the connection map"
    (add-connection "localhost" "ws")
    (is (= (:status (get  (get @connection-map "localhost") "ws")) "connected"))))

(deftest remove-connection-test
  (testing "It should remove a connection from the connection map"
    (add-connection "localhost" "ws")
    (remove-connection "localhost" "ws")
    (is (= (get @connection-map "localhost") nil))))

(deftest process-message-test
  (testing "It will ignore messages until the the client is logged in"
    (is (= (process-message "localhost" "ws" {}) nil)))
  (testing "It will process a login message if the client is not logged in"
    (with-redefs [puppetlabs.cthun.connection-states/process-server-message (fn [host ws message-body] "login")
                  puppetlabs.cthun.connection-states/login-message? (fn [message-body] true)]
      (is (= (process-message "localhost" "ws" {}) "login"))))
  (testing "It will process a client message"
    (with-redefs [puppetlabs.cthun.connection-states/logged-in? (fn [host ws] true)
                  puppetlabs.cthun.connection-states/process-client-message (fn [host ws message-body] "client")]
      (is (= (process-message "localhost" "ws" {:endpoints ["cth://client1.com"]}) "client"))))
  (testing "It will process a server message"
    (with-redefs [puppetlabs.cthun.connection-states/logged-in? (fn [host ws] true)
                  puppetlabs.cthun.connection-states/process-server-message (fn [host ws message-body] "server")]
      (is (= (process-message "localhost" "ws" {:endpoints ["cth://server"]}) "server")))))
        
