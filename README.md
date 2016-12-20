easy-update-metadata-dataset
===========
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-update-metadata-dataset.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-update-metadata-dataset)


SYNOPSIS
--------

    easy-update-metadata-dataset <datasets.csv>


DESCRIPTION
-----------

Batch-updates XML streams of objects in a Fedora Commons repository.


ARGUMENTS
---------

          --doUpdate                 Without this argument no changes are made to the repository, the default is a
                                     test mode that logs the intended changes
          --fedora-password  <arg>   Password for fedora repository, if omitted provide it on stdin
      -f, --fedora-url  <arg>        Base url for the fedora repository (default = http://localhost:8080/fedora)
          --fedora-username  <arg>   Username for fedora repository, if omitted provide it on stdin
          --help                     Show help message
          --version                  Show version of this program
    
     trailing arguments:
      input-file (required)   The CSV file (RFC4180) with required changes. The first line must be
                              'FEDORA_ID,STREAM_ID,XML_TAG,OLD_VALUE,NEW_VALUE', in that order. Additional columns
                              and empty lines are ignored.

CONTEXT
-------

It is the responsibility of the caller to

* Provide _valid_ new values in the input file.
* Make sure the CSV file is properly stored as UTF8, please export a spreadsheet with open-office for a valid format.
* Specify all changes for one dataset in subsequent lines, thus at most one dataset will have inconsistent values
  in case of trouble such as the old value not found or a system failure.
* Verify the preconditions for stream `AMD` and XML tag `datasetState` which requires a change history.
  Details are documented with [tests], note that some legitimate preconditions are not implemented and cause a failure,
  not expected preconditions might pass without a warning.
* Execute with a representative sample in test mode (without `--doUpdate`) and review the logged changes.
* Change the streams `DC` and `EMD` alike as far as applicable.
  In case of `EMD,accessRights` / `DC,rights` also
  * Update [file rights].
  * Call [easy-update-fs-rdb].
  * Reboot the web-ui to clear the [hibernate] cash.
* Call [easy-task-add-new-license] if EMD and/or file rights were changed, the link requires access to the legacy code base.
* Update relations such as `hasDoi`, `isMemberOf`, collections ...
* Call [easy-update-solr-index] if necessary.

The legacy code base provides [CSV examples] for the `deasy` environment.

[easy-update-fs-rdb]: https://github.com/DANS-KNAW/easy-update-fs-rdb
[file rights]: https://github.com/DANS-KNAW/easy-update-metadata-fileitem
[hibernate]: http://hibernate.org/
[easy-task-add-new-license]: https://github.com/DANS-KNAW/easy-app/blob/master/tool/task-add-new-license/README.md
[easy-update-solr-index]: https://github.com/DANS-KNAW/easy-update-solr-index
[tests]: src/test/scala/nl/knaw/dans/easy/umd/TransformerSpec.scala
[CSV examples]: src/test/resources


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
