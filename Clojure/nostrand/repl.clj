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
    [Mono.Terminal LineEditor]))

(defn- prompt []
  (str *ns* "> "))

(def socket-repl-running (atom true))

(defn cli [{:keys [repl/history
                   repl/line-editor
                   repl/env]
            :or {repl/history 500}
            :as args}]
  (binding [*ns* (find-ns 'user)
            *warn-on-reflection* *warn-on-reflection*
            *unchecked-math* *warn-on-reflection*]
    (loop [s (.Edit line-editor (prompt) "")]
      (when s
        (try (-> s
                 read-string
                 eval
                 pr-str
                 (Terminal/Message ConsoleColor/Gray))
          (catch EndOfStreamException e)
          (catch Exception e
            (Terminal/Message "Exception" (str e) ConsoleColor/Yellow)))
        (recur (.Edit line-editor (prompt) "")))))
  (reset! socket-repl-running false))

(defonce cli-output-queue (Queue/Synchronized (Queue.)))

(defn socket [{:keys [repl/line-editor
                      repl/env
                      repl/history
                      repl/port
                      repl/send-buffer-size
                      repl/receive-buffer-size
                      repl/receive-timeout]
               :or {repl/history 500
                    repl/port 11217
                    repl/send-buffer-size (* 1024 5000)
                    repl/receive-buffer-size (* 1024 5000)
                    repl/receive-timeout 100}
               :as args}]
  (let [socket (UdpClient. (IPEndPoint. IPAddress/Any port))
        sb (StringBuilder.)]
    (set! (.. socket Client SendBufferSize) send-buffer-size)
    (set! (.. socket Client ReceiveBufferSize) receive-buffer-size)
    (set! (.. socket Client ReceiveTimeout) receive-timeout)
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
                    (let [result (-> in-code
                                     read-string
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

(defn repl [args]
  (let [repl-args (merge args {:repl/line-editor (LineEditor. "nostrand" 500)})]
    (.Start (Thread. (gen-delegate ThreadStart [] (socket repl-args))))))