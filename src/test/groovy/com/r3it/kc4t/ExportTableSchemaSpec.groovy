package com.r3it.kc4t

import spock.lang.Specification

import com.cybozu.kintone.database.FieldType

class ExportTableSchemaSpec extends Specification {
    def "getSchema"() {
        setup:
        def jobId = "20150430123456"
        def config = new KintoneConnectorConfig()
        config.tablePrefix = "tmpTable_"
        def schema = new ExportTableSchema()

        when:
        schema.addField(FieldType.SINGLE_LINE_TEXT, 'userName')
        schema.addField(FieldType.NUMBER, 'fooRate')
        schema.addField(FieldType.DATETIME, 'createDate')
        schema.addField(FieldType.NUMBER, 'fooRate') // 同じカラムは追加されない
        schema.addField(FieldType.NUMBER, 'totalCost') // 型は同じでも名前が違えば追加

        then:
        schema.hasSubtables() == false
        schema.getSchema(config, jobId) == """|CREATE TABLE IF NOT EXISTS `tmpTable_20150430123456` (
            |`userName` text,
            |`fooRate` double DEFAULT NULL,
            |`createDate` datetime DEFAULT NULL,
            |`totalCost` double DEFAULT NULL
            |)""".stripMargin()
    }

    def "getFieldTypes"() {
        setup:
        def schema = new ExportTableSchema()

        when:
        schema.addField(FieldType.CALC, 'total')
        schema.addField(FieldType.RICH_TEXT, 'msgHtml')
        schema.addField(FieldType.MULTI_LINE_TEXT, 'msg')

        then:
        schema.getFieldTypes() == """["CALC","RICH_TEXT","MULTI_LINE_TEXT"]"""
    }

    def "getSubTable"() {
        setup:
        def schema = new ExportTableSchema()

        when:
        schema.addField(FieldType.SUBTABLE, 'users')
        schema.addField(FieldType.SUBTABLE, 'details')
        schema.getSubTable("details").addField(FieldType.CHECK_BOX, "likes")
        schema.getSubTable("details").addField(FieldType.RADIO_BUTTON, "likeType")
        schema.getSubTable("users").addField(FieldType.DROP_DOWN, "pref")

        then:
        schema.hasSubtables() == true
        schema.getSubTable("users").getFieldTypes() == """["DROP_DOWN"]"""
        schema.getSubTable("details").getFieldTypes() == """["CHECK_BOX","RADIO_BUTTON"]"""
        schema.getSubTable("unknown").getFieldTypes() == """[]""" // 存在しないテーブルを取得しても空を返す
    }

    def "getSubtableSchema"() {
        setup:
        def jobId = "20150430123456"
        def config = new KintoneConnectorConfig()
        config.tablePrefix = "tmpTable_"
        def schema = new ExportTableSchema()

        when:
        schema.addField(FieldType.SUBTABLE, 'users')
        schema.addField(FieldType.SUBTABLE, 'details')
        schema.getSubTable("details").addField(FieldType.CHECK_BOX, "likes")
        schema.getSubTable("details").addField(FieldType.RADIO_BUTTON, "likeType")
        schema.getSubTable("users").addField(FieldType.DROP_DOWN, "pref")

        then:
        schema.hasSubtables() == true
        schema.getSubTable("users").getSubtableSchema(config, jobId, 'users') ==
                """|CREATE TABLE IF NOT EXISTS `tmpTable_20150430123456_users` (
            |`tmpTable_20150430123456_users_fk` bigint(20) NOT NULL,
            |`pref` text
            |)""".stripMargin()
        schema.getSubTable("details").getSubtableSchema(config, jobId, 'details') ==
                """|CREATE TABLE IF NOT EXISTS `tmpTable_20150430123456_details` (
            |`tmpTable_20150430123456_details_fk` bigint(20) NOT NULL,
            |`likes` text,
            |`likeType` text
            |)""".stripMargin()
    }
}
