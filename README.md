
[![Build Status](https://github.com/igniterealtime/openfire-pushnotification-plugin/workflows/Java%20CI/badge.svg)](https://github.com/igniterealtime/openfire-pushnotification-plugin/actions)

The Push Notification Plugin is a plugin for the [Openfire XMPP server](https://www.igniterealtime.org/openfire), which adds support sending push notifications to client software, as described in [XEP-0357: "Push Notifications"](https://xmpp.org/extensions/xep-0357.html).

Building
--------

This project is using the Maven-based Openfire build process, as introduced in Openfire 4.2.0. To build this plugin locally, ensure that the following are available on your local host:

* A Java Development Kit, version 7 or (preferably) 8
* Apache Maven 3

To build this project, invoke on a command shell:

    $ mvn clean package

Upon completion, the openfire plugin will be available in `target/pushnotification-openfire-plugin-assembly.jar`. This file should be renamed to `pushnotification.jar`

Installation
------------
Copy `pushnotification.jar` into the plugins directory of your Openfire server, or use the Openfire Admin Console to upload the plugin. The plugin will then be automatically deployed.

To upgrade to a new version, copy the new `pushnotification.jar` file over the existing file.

Releasing (smartcraft-shape fork)
---------------------------------
To publish a release (e.g. 1.1.2):

1. Set `pom.xml` version to the release version (e.g. `1.1.2`, no `-SNAPSHOT`).
2. Commit, push, then create and push a tag:  
   `git tag patched_1.1.2 && git push origin patched_1.1.2`
3. The GitHub Action **Release** will build and create the release with `pushnotification.jar`.
4. Bump `pom.xml` to the next dev version (e.g. `1.1.3-SNAPSHOT`) and commit.

