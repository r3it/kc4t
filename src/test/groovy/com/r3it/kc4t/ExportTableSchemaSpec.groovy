package com.r3it.kc4t

import spock.lang.Specification

import com.cybozu.kintone.database.FieldType

class ExportTableSchemaSpec extends Specification {

    def "getSchema"() {
        setup:
        def schema = new ExportTableSchema()

        when:
        schema.addField(FieldType.SINGLE_LINE_TEXT, 'userName')
        schema.addField(FieldType.NUMBER, 'userNumber')
        schema.addField(FieldType.DATETIME, 'createDate')
        schema.addField(FieldType.NUMBER, 'userNumber') // 同じカラムは追加されない
        schema.addField(FieldType.NUMBER, 'totalCost') // 型は同じでも名前が違えば追加

        then:
        schema.getSchema() == """|`userName` text,
            |`userNumber` bigint(20) DEFAULT NULL,
            |`createDate` datetime DEFAULT NULL,
            |`totalCost` bigint(20) DEFAULT NULL""".stripMargin()
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
        schema.getSubTable("users").getFieldTypes() == """["DROP_DOWN"]"""
        schema.getSubTable("details").getFieldTypes() == """["CHECK_BOX","RADIO_BUTTON"]"""
        schema.getSubTable("unknown").getFieldTypes() == """[]""" // 存在しないテーブルを取得しても空を返す
    }
}
