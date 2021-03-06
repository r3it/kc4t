package com.r3it.kc4t

import spock.lang.*

class KintoneConnectorForTalendSpec extends Specification {
    def config = new KintoneConnectorConfig()
    def credential = new ConfigSlurper().parse(new File('src/test/resources/kintoneCredential.groovy').toURL())

    def "createJobJd"() {
        setup:
        def con = new KintoneConnectorForTalend()

        expect:
        con.createJobId(date) == result

        where:
        date | result
        new Date("2015/05/01 12:34:56") | "20150501123456"
    }

    def "select"() {
        setup:
        def con = new KintoneConnectorForTalend()
        config.jdbcUrl = "jdbc:h2:" + System.getProperty("user.home") + File.separator + 'kc4tdb'
        config.jdbcDriverClass = 'org.h2.Driver'
        config.jdbcUser = 'sa'
        config.jdbcPassword = ''

        config.tablePrefix = 'kc4t_'
        config.jobStatusReportTableName = 'report_'
        config.saveTmpTable = true

        config.apiToken = credential.account1.apiToken
        config.subDomain = credential.account1.subDomain
        config.appId = 15l
        config.useRevision = true

        expect:
        def result = con.select(jobId, config, query)
        if (result.exception) {
            result.exception.printStackTrace()
        }
        result.success == true

        println result.schema.getSchema(config, jobId)
        result.schema.getSchema(config, jobId) == """|CREATE TABLE IF NOT EXISTS `kc4t_20150507_123456` (
        |`\$revision` bigint(20) DEFAULT NULL,
        |`\$id` bigint(20) DEFAULT NULL,
        |`total` text,
        |`レコード番号` bigint(20) DEFAULT NULL,
        |`entryDate` date DEFAULT NULL,
        |`更新者` text,
        |`作成者` text,
        |`更新日時` datetime DEFAULT NULL,
        |`作成日時` datetime DEFAULT NULL
        |)""".stripMargin()

        println result.schema.hasSubtables()
        if (result.schema.hasSubtables()) {

            result.schema.getSubtableNames().each {
                println result.schema.getSubTable(it).getSubtableSchema(config, jobId, it)
                result.schema.getSubTable(it).getSubtableSchema(config, jobId, it) ==
                        """|CREATE TABLE IF NOT EXISTS `kc4t_20150507_123456_Table` (
                |`kc4t_20150507_123456_Table_fk` bigint(20) NOT NULL,
                |`備考` text,
                |`時刻` time DEFAULT NULL,
                |`price` bigint(20) DEFAULT NULL,
                |`食べたもの` text
                |)"""
            }
        }

        where:
        jobId | query
        '20150507_123456' | ''
    }

    def "execExportRestClient"() {
        setup:
        def con = new KintoneConnectorForTalend()
        config.jdbcUrl = "jdbc:h2:" + System.getProperty("user.home") + File.separator + 'kc4tdb'
        config.jdbcDriverClass = 'org.h2.Driver'
        config.jdbcUser = 'sa'
        config.jdbcPassword = ''

        config.tablePrefix = 'kc4t_'
        config.jobStatusReportTableName = 'report_'
        config.saveTmpTable = true

        config.apiToken = credential.account1.apiToken
        config.subDomain = credential.account1.subDomain
        config.appId = 15l

        expect:
        def result = con.execExportRestClient(config, query)
        if (result.exception) {
            result.exception.printStackTrace()
        }
        result.success == true

        Thread.sleep(1 * 1000)

        where:
        jobId | query
        '20150507_123456' | ''
    }

    def "exportAllFromKintone"() {
        setup:
        def con = new KintoneConnectorForTalend()
        config.jdbcUrl = "jdbc:h2:" + System.getProperty("user.home") + File.separator + 'kc4tdb'
        config.jdbcDriverClass = 'org.h2.Driver'
        config.jdbcUser = 'sa'
        config.jdbcPassword = ''

        // devアカウント
        //        config.tablePrefix = 'kc4t_'
        //        config.jobStatusReportTableName = 'kc4t_export_jobreport'
        //        config.saveTmpTable = true
        //
        //        config.apiToken = credential.account1.apiToken
        //        config.subDomain = credential.account1.subDomain
        //        config.appId = 15l

        // ゲストスペースの「案件」アプリ（3万件データが入ってるやつ）
        //        config.tablePrefix = 'anken_'
        //        config.jobStatusReportTableName = 'anken_export_jobreport'
        //        config.saveTmpTable = true
        //        config.useRevision = false
        //
        //        config.apiToken = credential.account1.apiToken
        //        config.subDomain = credential.account1.subDomain
        //        config.appId = 139l
        //        config.guestSpaceId = 9l
        //        config.orderByField = "project_id"

        // サブテーブルが多くあるパターン
        config.tablePrefix = 'sbtbl_test_'
        config.jobStatusReportTableName = 'sbtbl_test_jobreport'
        config.saveTmpTable = true

        config.apiToken = credential.account2.apiToken
        config.subDomain = credential.account2.subDomain
        config.appId = 176l


        expect:
        def result = con.exportAllFromKintone(config)
        if (result.exception != null) {
            result.exception.printStackTrace()
        }
        result.success == true
    }

    def "ToMySQLexportAllFromKintone"() {
        setup:
        def con = new KintoneConnectorForTalend()
        config.jdbcUrl = 'jdbc:mysql://localhost/kc4t'
        config.jdbcDriverClass = 'com.mysql.jdbc.Driver'
        config.jdbcUser = 'kc4t'
        config.jdbcPassword = 'kc4t'

        // サブテーブルが多くあるパターン
        config.tablePrefix = 'sbtbl_test_'
        config.jobStatusReportTableName = 'sbtbl_test_jobreport'
        config.saveTmpTable = true

        config.apiToken = credential.account2.apiToken
        config.subDomain = credential.account2.subDomain
        config.appId = 176l


        expect:
        def result = con.exportAllFromKintone(config)
        if (result.exception != null) {
            result.exception.printStackTrace()
        }
        result.success == true
        result.mainTableName == 'sbtbl_test_' + result.jobId
        result.subTableNames == result.mainTableName + '_item_table_deploy,'+
                result.mainTableName + '_item_table_develop,'+
                result.mainTableName + '_item_table_test,'+
                result.mainTableName + '_item_table_design'
    }

    def "exportFromKintone"() {
        setup:
        def con = new KintoneConnectorForTalend()
        config.jdbcUrl = "jdbc:h2:" + System.getProperty("user.home") + File.separator + 'kc4tdb'
        config.jdbcDriverClass = 'org.h2.Driver'
        config.jdbcUser = 'sa'
        config.jdbcPassword = ''

        config.tablePrefix = 'dkc4t_'
        config.jobStatusReportTableName = 'dreport_'
        config.saveTmpTable = true

        config.apiToken = credential.account1.apiToken
        config.subDomain = credential.account1.subDomain
        config.appId = 15l
        //        config.appId = 176l

        expect:
        def result = con.execExportRestClient(config, query)
        if (result.exception) {
            result.exception.printStackTrace()
        }
        result.success == true


        where:
        jobId | query
        '20150515_123456' | 'レコード番号 = 1'
    }


    def "insertAndUpdate"() {
        setup:
        def con = new KintoneConnectorForTalend()
        config.apiToken = credential.account3.apiToken
        config.subDomain = credential.account3.subDomain
        config.appId = "71"

        config.keyFieldCode = 'com_id'
        def columns = [
            noExistField: 'foobar',
            com_id: System.currentTimeMillis(),
            com_name: "株式会社123運輸" + new Date().toString()
        ]

        expect:
        // try insert
        def result = con.upsertKintone(config, columns)
        result.success == true
        result.insertedId > 0

        // try update
        def result2 = con.upsertKintone(config, [noExistField: 'foobar', com_id: columns.com_id, com_name: '会社名が変わりました'])
        result2.success == true
        result2.insertedId == 0
    }

    def "insertDateFormat"() {
        setup:
        def con = new KintoneConnectorForTalend()
        config.apiToken = credential.account3.apiToken
        config.subDomain = credential.account3.subDomain
        config.appId = "71"

        config.keyFieldCode = 'com_id'
        def columns = [
            com_id: System.currentTimeMillis(),
            com_name: "株式会社123運輸" + new Date().toString(),
            date: "2016-02-29",
            time: "14:55:11",
            datetime: "2016-03-01T12:13:14Z"
        ]

        expect:
        // try insert
        def result = con.upsertKintone(config, columns)
        result.success == true
        result.insertedId > 0

        // try update
        def result2 = con.upsertKintone(config, [
            com_id: columns.com_id, date: '2016-03-02', time: "12:12:12", datetime: '2016-03-02T12:12:12Z'])
        result2.success == true
        result2.insertedId == 0
    }

    def "insertListValues"() {
        setup:
        def con = new KintoneConnectorForTalend()
        config.apiToken = credential.account4.apiToken
        config.subDomain = credential.account4.subDomain
        config.appId = "85"

        config.keyFieldCode = 'id'
        def columns = [
            id: System.currentTimeMillis(),
            creator: com.r3it.kc4t.KintoneUser.createUser("Administrator"),
            user: com.r3it.kc4t.KintoneUser.createUserList("Administrator"),
            list: java.util.Arrays.asList(["AA", "BB"] as String[])
        ]

        expect:
        // try insert
        def result = con.upsertKintone(config, columns)
        result.success == true
        result.insertedId > 0

        // try update
        def result2 = con.upsertKintone(config, [
            id: columns.id,
            user: com.r3it.kc4t.KintoneUser.createUser("taro"),
            list: java.util.Arrays.asList(["DD"] as String[])
        ])
        result2.success == true
        result2.insertedId == 0
    }

    def "insertCreator"() {
        setup:
        def con = new KintoneConnectorForTalend()
        config.apiToken = credential.account4.apiToken
        config.subDomain = credential.account4.subDomain
        config.appId = "85"

        config.keyFieldCode = 'id'
        def columns = [
            id: System.currentTimeMillis(),
            creator: com.r3it.kc4t.KintoneUser.createUser("taro"),
            list: java.util.Arrays.asList(["AA", "BB", "CC"] as String[])
        ]

        expect:
        // try insert
        def result = con.upsertKintone(config, columns)
        result.success == true
        result.insertedId > 0

        // try update
        def result2 = con.upsertKintone(config, [
            id: columns.id,
            user: com.r3it.kc4t.KintoneUser.createUser("Administrator"),
            list: java.util.Arrays.asList(["DD"] as String[])
        ])
        result2.success == true
        result2.insertedId == 0
    }

    def "insertEmptyUser"() {
        setup:
        def con = new KintoneConnectorForTalend()
        config.apiToken = credential.account4.apiToken
        config.subDomain = credential.account4.subDomain
        config.appId = "85"

        config.keyFieldCode = 'id'
        def columns = [
            id: System.currentTimeMillis(),
            user: com.r3it.kc4t.KintoneUser.createUserList("taro"),
            list: java.util.Arrays.asList(["AA", "BB"] as String[]),
            str: 'string!',
            num: '123'
        ]

        expect:
        // try insert
        def result = con.upsertKintone(config, columns)
        result.success == true
        result.insertedId > 0

        // try update
        def result2 = con.upsertKintone(config, [
            id: columns.id,
            user: null,
            list: null,
            str: null,
            num: null
        ])
        result2.success == true
        result2.insertedId == 0
    }

    def "updateEmptyDateFormat"() {
        setup:
        def con = new KintoneConnectorForTalend()
        config.apiToken = credential.account4.apiToken
        config.subDomain = credential.account4.subDomain
        config.appId = "85"

        config.keyFieldCode = 'id'
        def columns = [
            id: System.currentTimeMillis(),
            string: "株式会社123運輸" + new Date().toString(),
            date: "2016-02-29",
            time: "14:55:11",
            datetime: "2016-03-01T12:13:14Z"
        ]

        expect:
        // try insert
        def result = con.upsertKintone(config, columns)
        result.success == true
        result.insertedId > 0

        // try update
        def result2 = con.upsertKintone(config, [
            id: columns.id,
            date: null,
            time: null,
            datetime: null
        ])
        result2.success == true
        result2.insertedId == 0
    }

    def "insertAndUpdateWithPasswordAuth"() {
        setup:
        def con = new KintoneConnectorForTalend()
        config.subDomain = credential.account5.subDomain
        config.userName = credential.account5.userName
        config.password = credential.account5.password
        config.appId = "71"

        config.keyFieldCode = 'com_id'
        def columns = [
            noExistField: 'foobar',
            com_id: System.currentTimeMillis(),
            com_name: "株式会社123運輸" + new Date().toString()
        ]

        expect:
        // try insert
        def result = con.upsertKintone(config, columns)
        result.success == true
        result.insertedId > 0

        // try update
        def result2 = con.upsertKintone(config, [noExistField: 'foobar', com_id: columns.com_id, com_name: 'パスワード認証で書き換えました'])
        result2.success == true
        result2.insertedId == 0
    }

}
