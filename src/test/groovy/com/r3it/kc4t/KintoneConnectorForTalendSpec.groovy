package com.r3it.kc4t

import spock.lang.*

class KintoneConnectorForTalendSpec extends Specification {
    def config = new KintoneConnectorConfig()

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

        config.apiToken = '8LYW56ZWhc7gPneAmJTwUuCneyrTQOrGtxbD8N06'
        config.subDomain = 'ehr9p'
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

        config.apiToken = '8LYW56ZWhc7gPneAmJTwUuCneyrTQOrGtxbD8N06'
        config.subDomain = 'ehr9p'
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
        //        config.apiToken = '8LYW56ZWhc7gPneAmJTwUuCneyrTQOrGtxbD8N06'
        //        config.subDomain = 'ehr9p'
        //        config.appId = 15l

        // ゲストスペースの「案件」アプリ（3万件データが入ってるやつ）
        //        config.tablePrefix = 'anken_'
        //        config.jobStatusReportTableName = 'anken_export_jobreport'
        //        config.saveTmpTable = true
        //        config.useRevision = false
        //
        //        config.apiToken = 'duXfacz05iWc2sTnzghp1twszsTPML4Ek2kYS1aE'
        //        config.subDomain = 'r3it'
        //        config.appId = 139l
        //        config.guestSpaceId = 9l
        //        config.orderByField = "project_id"

        // サブテーブルが多くあるパターン
        config.tablePrefix = 'sbtbl_test_'
        config.jobStatusReportTableName = 'sbtbl_test_jobreport'
        config.saveTmpTable = true

        config.apiToken = 'jM3taudfyhnXsbU3deU2URV5uudPLSDo1OSRT6Ob'
        config.subDomain = 'r3it'
        config.appId = 176l


        expect:
        def result = con.exportAllFromKintone(config)
        if (result.exception != null) {
            result.exception.printStackTrace()
        }
        result.success == true
    }
}
