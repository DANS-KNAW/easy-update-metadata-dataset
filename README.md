easy-update-metadata-dataset
===========
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-update-metadata-dataset.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-update-metadata-dataset)


SYNOPSIS
--------

    easy-update-metadata-dataset --stream-id [EMD|DC|...] --tag [accessRights|rights|...] <datasets.csv>


DESCRIPTION
-----------

Batch-updates metadata streams of datasets in a Fedora Commons repository


ARGUMENTS
---------

          --doUpdate                 Without this argument no changes are made to the repository, the default is a
                                     test mode that logs the intended changes
          --fedora-password  <arg>   Password for fedora repository, if omitted provide it on stdin
      -f, --fedora-url  <arg>        Base url for the fedora repository (default = http://localhost:8080/fedora)
          --fedora-username  <arg>   Username for fedora repository, if omitted provide it on stdin
      -s, --stream-id  <arg>         id of fedoara stream to update
      -t, --tag  <arg>               xml tag to change
          --help                     Show help message
          --version                  Show version of this program
    
     trailing arguments:
      input-file (required)   The CSV file with required changes. Columns: fedoraID, new value, optional old value



INSTALLATION AND CONFIGURATION
------------------------------


1. Unzip the tarball to a directory of your choice, e.g. /opt/
2. A new directory called easy-update-metadata-dataset-<version> will be created
3. Add the command script to your `PATH` environment variable by creating a symbolic link to it from a directory that is
   on the path, e.g. 
   
        ln -s /opt/easy-update-metadata-dataset-<version>/bin/easy-update-metadata-dataset /usr/bin



General configuration settings can be set in `src/main/assembly/dist/cfg/appliation.properties` and logging can be configured
in `src/main/assembly/dist/cfg/logback.xml`. The available settings are explained in comments in aforementioned files.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher

Steps:

        git clone https://github.com/DANS-KNAW/easy-update-metadata-dataset.git
        cd easy-update-metadata-dataset
        mvn install
