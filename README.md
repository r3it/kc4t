# kintone Connector for talend

## What's this?

This is kintone and RDBMS data export/import component for Talend.

## kc4t features

* Records export from kintone to RDB(JDBC)
* Records sync from RDB to kintone(coming soon)

## Requirement

* kintone SDK for Java version 0.6 or later https://github.com/kintone/java-sdk
* JDK 7 or later

## Feature details

### Records export from kintone to RDB

* automatic schema creation 
* export to temporary tabel(s)
* job status logging
* TODO GET single record by recordID

### Configurations

* kintone API token / subdomain / app Id for target application
* JDBC connection URL
* temporary table prefix
* job status report table name
* if you need save temporary tabels, you can configure it.

## Installation & Usage

TODO.

## Author

[Koichiro Nishijima](https://github.com/k-nishijima)

## License

[Apache v2 License](http://www.apache.org/licenses/LICENSE-2.0.html)
