(ns terrain.routes.schemas.collaborator
  (:use [common-swagger-api.schema :only [describe]]
        [schema.core :only [defschema optional-key]])
  (:require [common-swagger-api.schema.groups :as group-schema]
            [common-swagger-api.schema.subjects :as subject-schema]))

(def GroupMember
  (assoc subject-schema/Subject
    :display_name (describe String "The displayable group member name.")))

(defn group-members [group-descriptor]
  {:members (describe [GroupMember] (str "The list of " group-descriptor " members."))})

(def CollaboratorListNamePathParam (describe String "The name of the collaborator list"))

(defschema CollaboratorListSearchParams
  {(optional-key :search)
   (describe String "The collaborator list name substring to search for")})

(defschema CollaboratorListRetainPermissionsParams
  {(optional-key :retain-permissions)
   (describe Boolean "Select `true` if removed users should retain permission to shared resources")})

(defschema GetCollaboratorListsResponse (group-schema/group-list "collaborator list" "collaborator lists"))
(defschema AddCollaboratorListRequest (group-schema/base-group "collaborator list"))
(defschema CollaboratorList (group-schema/group "collaborator list"))
(defschema CollaboratorListUpdate
  (select-keys (group-schema/group-update "collaborator list") (map optional-key [:name :description])))
(defschema CollaboratorListStub (group-schema/group-stub "collaborator list"))
(defschema CollaboratorListMembers (group-members "collaborator list"))

(def TeamNamePathParam
  (describe String "The name of the team, including the username prefix (e.g. `username:team-name`)"))

(defschema TeamListingParams
  {(optional-key :search)
   (describe String "The team name substring to search for")

   (optional-key :creator)
   (describe String "Only teams created by the user with this username will be listed if specified")

   (optional-key :member)
   (describe String "Only teams to which the user with this username belongs will be listed if specified")})

(defschema TeamListing (group-schema/group-list "team" "teams"))
(defschema AddTeamRequest
  (assoc (select-keys (group-schema/base-group "team") [:name (optional-key :description)])
    (optional-key :public_privileges)
    (describe [group-schema/ValidGroupPrivileges] "Team privileges granted to all DE users")))
(defschema Team (group-schema/group "team"))
(defschema UpdateTeamRequest (select-keys (group-schema/group-update "team") (map optional-key [:name :description])))
(defschema TeamStub (group-schema/group-stub "team"))
(defschema TeamMembers (group-members "team"))
