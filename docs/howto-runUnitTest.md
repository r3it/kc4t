# How to run Unit test

## setting of kintoneCredential.groovy

You have to create the kintoneCredential.groovy file to src/test/resources/ folder.

***This file has been added to .gitignore.***

kintoneCredential.groovy format is here.

```
account1 {
    subDomain = 'yourSubDomain1'
    apiToken = 'yourApiToken1'
}

account2 {
    subDomain = 'yourSubDomain2'
    apiToken = 'yourApiToken2'
}

```

## KintoneConnectorForTalendSpec.groovy

"account1" is normal application. "account2" is an application that has many sub-tables.

The type of test, you can switch between the subDomain and apiToken.

"config.appId" in the UnitTest, please specify the applicationId of your application.
