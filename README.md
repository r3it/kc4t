# kintone Connector for talend

## What's this?

This is kintone and RDBMS data export/import component for Talend.

日本語ドキュメントは弊社Webサイトをご確認ください。

[https://www.r3it.com/community/oss/kc4t/](https://www.r3it.com/community/oss/kc4t/)

## kc4t features

* tKintoneInput / Records export from kintone to RDB(JDBC)
* tKintoneOutput / Records export from RDB to kintone

## Requirement

* kintone SDK for Java version 0.6 or later https://github.com/kintone/java-sdk
* JDK 7 or later

## Feature details

### Records export from kintone to RDB

* automatic schema creation 
* export to temporary tabel(s)
* GET single record by recordID
* job status logging

### Configurations

* kintone API token / subdomain / app Id for target application
* JDBC connection URL
* temporary table prefix
* job status report table name

## Installation & Usage

see [How to use tKintoneInput component](docs/howto-tKintoneInput.md) and [tKintoneOutput component](docs/howto-tKintoneOutput.md).

kc4t is using [Spock Framework](http://docs.spockframework.org/). see [How to run Unit test](docs/howto-runUnitTest.md)

## Author

[Koichiro Nishijima](https://github.com/k-nishijima)

## Contact

[R3 institute](http://www.r3it.com/)

## License

[Apache v2 License](http://www.apache.org/licenses/LICENSE-2.0.html)
