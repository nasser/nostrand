(ns nostrand.repl
  (:require [clojure.string :as string])
  (:import 
    [System.IO StringWriter EndOfStreamException]
    [System.Collections Queue]
    [System.Text Encoding StringBuilder]
    [System.Threading Thread ThreadStart]
    [System.Net IPEndPoint IPAddress]
    [System.Net.Sockets UdpClient SocketException]
    [Nostrand Nostrand Terminal] 
    [Mono.Terminal LineEditor]
    ))

(defn- prompt []
  (str *ns* "> "))

(defn- continue-prompt []
  (let [l (dec (count (prompt)))]
    (str (apply str (repeat l ".")) " ")))

(def socket-repl-running (atom true))

(defn balanced? [s]
  (try 
    (read-string {:read-cond :allow} s)
    true
    (catch EndOfStreamException e
      false)))

(defn cli [{:keys [history
                   line-editor
                   env]
            :or {history 500
                 line-editor (LineEditor. "nostrand" 500)}
            :as args}]
  (binding [*ns* (find-ns 'user)
            *warn-on-reflection* *warn-on-reflection*
            *unchecked-math* *warn-on-reflection*]
    (loop [s (.Edit line-editor (prompt) "")]
      (when s
        (if-not (balanced? s)
          (recur (str s "\n" (.Edit line-editor (continue-prompt) "")))
          (do
            (try 
              (-> (read-string {:read-cond :allow} s)
                  eval
                  pr-str
                  (Terminal/Message ConsoleColor/Gray))
              (catch Exception e
                (Terminal/Message "Exception" (str e) ConsoleColor/Yellow)))
            (recur (.Edit line-editor (prompt) "")))))))
  (reset! socket-repl-running false))

(defonce cli-output-queue (Queue/Synchronized (Queue.)))

(defn- random-port []
  (int (+ 1024 (* (rand) (- UInt16/MaxValue 1024)))))

(defn- available-socket [port]
  (try
    (UdpClient. (IPEndPoint. IPAddress/Any port))
    (catch SocketException e
      (available-socket (random-port)))))

(defn socket [{:keys [line-editor
                      env
                      history
                      port
                      send-buffer-size
                      receive-buffer-size
                      receive-timeout]
               :or {history 500
                    port 11217
                    send-buffer-size (* 1024 5000)
                    receive-buffer-size (* 1024 5000)
                    receive-timeout 100}
               :as args}]
  (let [^UdpClient socket (available-socket port)
        sb (StringBuilder.)]
    (set! (.. socket Client SendBufferSize) (int send-buffer-size))
    (set! (.. socket Client ReceiveBufferSize) (int receive-buffer-size))
    (set! (.. socket Client ReceiveTimeout) (int receive-timeout))
    (Terminal/Message "REPL" (.. socket Client LocalEndPoint) ConsoleColor/Blue)
    (.Start (Thread. (gen-delegate ThreadStart [] (cli args))))
    (binding [*ns* (find-ns 'user)
              *out* (StringWriter. sb)
              *warn-on-reflection* *warn-on-reflection*
              *unchecked-math* *warn-on-reflection*]
      (loop [running @socket-repl-running]
        (when running
          (try
            (let [^IPEndPoint sender (IPEndPoint. IPAddress/Any 0)
                  in-bytes (.Receive socket (by-ref sender))]
              (if (> (.Length in-bytes) 0)
                (let [in-code (.GetString Encoding/UTF8 in-bytes)]
                  (try 
                    (let [result (-> (read-string {:read-cond :allow} in-code)
                                     eval
                                     pr-str)
                          out-str (str sb)
                          out-bytes (.GetBytes Encoding/UTF8 out-str)
                          response-bytes (.GetBytes Encoding/UTF8 (str in-code out-str result "\n" (prompt)))
                          ]
                      (.Send socket response-bytes (.Length response-bytes) sender)
                      ;; cli print
                      (Console/WriteLine)
                      (Console/Write (prompt))
                      (Console/Write in-code)
                      (when-not (empty? out-str)
                        (Terminal/Message (str sb) ConsoleColor/Gray))
                      (Terminal/Message result ConsoleColor/Gray)
                      (.SnapHomeRowToCursor line-editor)
                      (.StopEdit line-editor)
                      (.Clear sb))
                    (catch Exception e
                      (let [ex-bytes (.GetBytes Encoding/UTF8 (str in-code e "\n" (prompt)))]
                        (.Send socket ex-bytes (.Length ex-bytes) sender)
                        ;; cli print
                        (Console/WriteLine)
                        (Console/Write (prompt))
                        (Terminal/Message "Exception" (str e) ConsoleColor/Yellow)
                        (.SnapHomeRowToCursor line-editor)
                        (.StopEdit line-editor)
                        (.Clear sb)))))))
            (catch SocketException e))
          (recur @socket-repl-running))))))

(defn repl [port]
  (let [repl-args {:port port :line-editor (LineEditor. "nostrand" 500)}]
    (.Start (Thread. (gen-delegate ThreadStart [] (socket repl-args))))))