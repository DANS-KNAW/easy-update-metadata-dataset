easy-update-metadata-dataset
===========
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-update-metadata-dataset.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-update-metadata-dataset)


SYNOPSIS
--------

    easy-update-metadata-dataset --stream-id [EMD|DC|AMD|...] --tag [accessRights|datasetState|...] <datasets.csv>


DESCRIPTION
-----------

Batch-updates metadata streams of datasets in a Fedora Commons repository

Special cases like `--stream-id AMD --tag datasetState` (which requires a change history) are documented with [tests],
note some report a failure.

**WARNING**: It is the responsibility of the caller to

* Provide a _valid_ new value in the input file.
* Change `DC` and `EMD` alike as far as applicable.
* In case of `--tag 'accessRights'` (for both `EMD` and `AMD`) also
  * Update [file rights] along with dataset rights.
  * Call [easy-update-fs-rdb].
  * Call [easy-task-add-new-license], this link requires access to the legacy code base,
  * Reboot the web-ui to clear the [hibernate] cash.
* If applicable update relations such as hasDoi and isMemberOf.
* Call [easy-update-solr-index] if necessary.

[easy-update-fs-rdb]: https://github.com/DANS-KNAW/easy-update-fs-rdb
[file rights]: https://github.com/DANS-KNAW/easy-update-metadata-fileitem
[hibernate]: http://hibernate.org/
[easy-task-add-new-license]: https://github.com/DANS-KNAW/easy-app/blob/master/tool/task-add-new-license/README.md
[easy-update-solr-index]: https://github.com/DANS-KNAW/easy-update-solr-index
[tests]: nl.knaw.dans.easy.umd.TransformerSpec


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
     input-file (required)   The CSV file with required changes. Columns: fedoraID, newValue. First line is
                             assumed to be a header.


INSTALLATION AND CONFIGURATION
------------------------------

1. Unzip the tarball to a directory of your choice, e.g. /opt/
2. A new directory called easy-update-metadata-dataset-<version> will be created
3. Add the command script to your `PATH` environment variable by creating a symbolic link to it from a directory that is
   on the path, e.g. 
   
        ln -s /opt/easy-update-metadata-dataset-<version>/bin/easy-update-metadata-dataset /usr/bin

General configuration settings can be set in `cfg/application.properties` and logging can be configured
in `cfg/logback.xml`. The available settings are explained in comments in aforementioned files.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher

Steps:

        git clone https://github.com/DANS-KNAW/easy-update-metadata-dataset.git
        cd easy-update-metadata-dataset
        mvn install
