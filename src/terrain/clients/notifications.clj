(ns terrain.clients.notifications
  (:use [clojure-commons.client :only [build-url-with-query]]
        [terrain.util.config :only [notificationagent-base]]
        [terrain.util.transformers :only [add-current-user-to-map]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [terrain.clients.notifications.raw :as raw]))

(defn notificationagent-url
  "Builds a URL that can be used to connect to the notification agent."
  ([relative-url]
     (notificationagent-url relative-url {}))
  ([relative-url query]
     (build-url-with-query (notificationagent-base)
                           (add-current-user-to-map query) relative-url)))

(defn send-notification
  "Sends a notification to a user."
  [m]
  (let [res (client/post (notificationagent-url "notification")
                         {:content-type :json
                          :body (cheshire/encode m)})]
    res))

(defn send-tool-notification
  "Sends notification of tool deployment to a user if notification information
   is included in the import JSON."
  [m]
  (let [{:keys [user email]} m]
    (when (every? (comp not nil?) [user email])
      (try
        (send-notification {:type "tool"
                            :user user
                            :subject (str (:name m) " has been deployed")
                            :email true
                            :email_template "tool_deployment"
                            :payload {:email_address email
                                      :toolname (:name m)
                                      :tooldirectory (:location m)
                                      :tooldescription (:description m)
                                      :toolattribution (:attribution m)
                                      :toolversion (:version m)}})
        (catch Exception e
          (log/warn e "unable to send tool deployment notification for" m))))))

(defn send-team-join-notification
  "Sends a notification indicating that a user has requested to join a team."
  [user-name user-email team-name admin message]
  (send-notification {:type "team"
                      :user (:id admin)
                      :subject (str user-name " has requested to join team \"" team-name "\"")
                      :email true
                      :email_template "team_join_request"
                      :payload {:email_address (:email admin)
                                :requester_name user-name
                                :requester_email user-email
                                :requester_message message
                                :team_name team-name}}))

(defn send-team-join-denial
  "Sends a notification indicating that a user's request to join a team has been denied."
  [user-id user-email team-name message]
  (send-notification {:type "team"
                      :user user-id
                      :subject "Team join request denied"
                      :email true
                      :email_template "team_join_denial"
                      :payload {:email_address user-email
                                :team_name team-name
                                :admin_message message}}))

(defn send-team-add-notification
  "Sends a notification indicating that the userhas been added to a team."
  [user team-name]
  (send-notification {:type "team"
                      :user (:id user)
                      :subject (str "Added to team")
                      :email true
                      :email_template "added_to_team"
                      :payload {:email_address (:email user)
                                :team_name team-name}}))

(defn mark-all-notifications-seen
  []
  (raw/mark-all-notifications-seen (cheshire/encode (add-current-user-to-map {}))))
