(ns terrain.services.fileio.actions
  (:use [clj-jargon.init :only [with-jargon]]
        [clojure-commons.error-codes]
        [terrain.util.service :only [success-response]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cemerick.url :as url]
            [clojure-commons.file-utils :as ft]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [ring.util.response :as rsp-utils]
            [clj-jargon.item-info :as info]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.permissions :as perm]
            [terrain.services.filesystem.icat :as icat]
            [terrain.services.filesystem.validators :as validators]
            [terrain.services.filesystem.updown :as updown]
            [terrain.services.metadata.internal-jobs :as internal-jobs])
  (:import [java.io InputStream]
           [clojure.lang IPersistentMap]))


(defn save
  [cm istream user dest-path]
  (log/info "In save function for " user dest-path)
  (let [ddir (ft/dirname dest-path)]
    (when-not (info/exists? cm ddir)
      (ops/mkdirs cm ddir))
    (ops/copy-stream cm istream user dest-path)
    (log/info "save function after copy.")
    dest-path))

(defn store
  [cm istream user dest-path]
  (log/info "In store function for " user dest-path)
  (let [ddir (ft/dirname dest-path)]
    (when-not (perm/is-writeable? cm user ddir)
      (log/error (str "Directory " ddir " is not writeable by " user))
      (throw+ {:error_code ERR_NOT_WRITEABLE
               :path ddir} )))

  (save cm istream user dest-path)
  dest-path)


(defn upload
  "This function uploads the contents of a stream to a data object in iRODS. If the data object
   exists, it will first be moved to the trash. The parent collection must exist and must be
   writeable by the user.

   Params:
     irods-cfg - the irods configuration parameter map
     user      - the who will own the data object being uploaded
     dest-path - the absolute path to the data object after it has been uploaded
     istream   - an input stream containing the contents of the data object."
  [^IPersistentMap irods-cfg ^String user ^String dest-path ^InputStream istream]
  (with-jargon irods-cfg :client-user user [cm]
    (let [dest-dir (ft/dirname dest-path)]
      (when-not (info/exists? cm dest-dir)
        (throw+ {:error_code ERR_DOES_NOT_EXIST :path dest-dir}))
      (if (info/exists? cm dest-path)
        (ops/delete cm dest-path))
      (store cm istream user dest-path)
      nil)))


(defn- url-encoded?
  [string-to-check]
  (re-seq #"\%[A-Fa-f0-9]{2}" string-to-check))

(defn urlimport
  "Submits a URL import job for execution.

   Parameters:
     user - string containing the username of the user that requested the import.
     address - string containing the URL of the file to be imported.
     filename - the filename of the file being imported.
     dest-path - irods path indicating the directory the file should go in."
  [user address filename dest-path]
  (let [filename  (if (url-encoded? filename) (url/url-decode filename) filename)
        dest-path (ft/rm-last-slash dest-path)]
    (with-jargon (icat/jargon-cfg) [cm]
      (validators/user-exists cm user)
      (validators/path-writeable cm user dest-path)
      (validators/path-not-exists cm (ft/path-join dest-path filename)))
    (internal-jobs/submit :url-import [address filename dest-path])
    (success-response
     {:msg   "Upload scheduled."
      :url   address
      :label filename
      :dest  dest-path})))

(defn download
  "Returns a response map filled out with info that lets the client download
   a file.

   Forcibly set Content-Type to application/octet-stream to ensure the file
   is downloaded rather than displayed."
  [user file-path]
  (log/debug "In download.")
  (let [resp (updown/download-file-as-stream user file-path true)]
    (assoc-in resp [:headers "Content-Type"] "application/octet-stream")))
