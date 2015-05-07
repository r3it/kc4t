package com.r3it.kc4t

import spock.lang.Specification

import com.cybozu.kintone.database.FieldType
import com.cybozu.kintone.database.Record
import com.cybozu.kintone.database.ResultSet

class ExportRecordSetSpec extends Specification {

    def "addResultSet-1record"() {
        setup:
        def jobId = "131313"
        def config = new KintoneConnectorConfig()
        def exportRecordSet = new ExportRecordSet(config, jobId)
        def rs = new ResultSet(null)
        def record = new Record()
        record.setString("userName", "test taro")
        record.setLong("userId", 123)
        rs.add(record)

        when:
        rs.next()
        exportRecordSet.next()
        exportRecordSet.addResultSet(rs, FieldType.SINGLE_LINE_TEXT, 'userName')
        exportRecordSet.addResultSet(rs, FieldType.NUMBER, 'userId')

        then:
        exportRecordSet.getRecords().size() == 1
        exportRecordSet.getRecords().get(0).colNames.size() == 2
        exportRecordSet.getRecords().get(0).colNames.get(0) == "userName"
        exportRecordSet.getRecords().get(0).colNames.get(1) == "userId"
        exportRecordSet.getRecords().get(0).colValues.get(0) == "test taro"
        exportRecordSet.getRecords().get(0).colValues.get(1) == "123"
    }

    def "addResultSet-2record"() {
        setup:
        def jobId = "131313"
        def config = new KintoneConnectorConfig()
        def expected = [
            "45,678",
            """今日は
いい天気でした""",
            """["選択肢1","選択肢2"]"""
        ]

        def exportRecordSet = new ExportRecordSet(config, jobId)
        def rs = new ResultSet(null)
        def record = new Record()
        record.setString("total", expected[0])
        record.setString("msg", expected[1])
        record.setString("likes", expected[2])
        rs.add(record)

        record = new Record()
        record.setString("total", expected[0] + "2")
        record.setString("msg", expected[1] + "2")
        record.setString("likes", expected[2] + "2")
        rs.add(record)

        when:
        rs.next()
        exportRecordSet.next()
        exportRecordSet.addResultSet(rs, FieldType.CALC, 'total')
        exportRecordSet.addResultSet(rs, FieldType.MULTI_LINE_TEXT, 'msg')
        exportRecordSet.addResultSet(rs, FieldType.CHECK_BOX, 'likes')
        rs.next()
        exportRecordSet.next()
        exportRecordSet.addResultSet(rs, FieldType.CALC, 'total')
        exportRecordSet.addResultSet(rs, FieldType.MULTI_LINE_TEXT, 'msg')
        exportRecordSet.addResultSet(rs, FieldType.CHECK_BOX, 'likes')

        then:
        exportRecordSet.getRecords().size() == 2
        exportRecordSet.getRecords().get(0).colNames.size() == 3
        exportRecordSet.getRecords().get(0).colNames.get(0) == "total"
        exportRecordSet.getRecords().get(0).colNames.get(1) == "msg"
        exportRecordSet.getRecords().get(0).colNames.get(2) == "likes"
        exportRecordSet.getRecords().get(0).colValues.get(0) == expected[0]
        exportRecordSet.getRecords().get(0).colValues.get(1) == expected[1]
        exportRecordSet.getRecords().get(0).colValues.get(2) == expected[2]

        exportRecordSet.getRecords().get(1).colNames.size() == 3
        exportRecordSet.getRecords().get(1).colNames.get(0) == "total"
        exportRecordSet.getRecords().get(1).colNames.get(1) == "msg"
        exportRecordSet.getRecords().get(1).colNames.get(2) == "likes"
        exportRecordSet.getRecords().get(1).colValues.get(0) == expected[0] + "2"
        exportRecordSet.getRecords().get(1).colValues.get(1) == expected[1] + "2"
        exportRecordSet.getRecords().get(1).colValues.get(2) == expected[2] + "2"
    }

    def "getInsertSQL-getInsertValues-2record"() {
        setup:
        def jobId = "131313"
        def config = new KintoneConnectorConfig()
        config.tablePrefix = "tmp_"

        def expected = [
            "45,678",
            """今日は
いい天気でした""",
            """["選択肢1","選択肢2"]"""
        ]

        def exportRecordSet = new ExportRecordSet(config, jobId)
        def rs = new ResultSet(null)
        def record = new Record()
        record.setString("total", expected[0])
        record.setString("msg", expected[1])
        record.setString("likes", expected[2])
        rs.add(record)

        record = new Record()
        record.setString("total", expected[0] + "2")
        record.setString("msg", expected[1] + "2")
        record.setString("likes", expected[2] + "2")
        rs.add(record)

        when:
        rs.next()
        exportRecordSet.next()
        exportRecordSet.addResultSet(rs, FieldType.CALC, 'total')
        exportRecordSet.addResultSet(rs, FieldType.MULTI_LINE_TEXT, 'msg')
        exportRecordSet.addResultSet(rs, FieldType.CHECK_BOX, 'likes')
        rs.next()
        exportRecordSet.next()
        exportRecordSet.addResultSet(rs, FieldType.CALC, 'total')
        exportRecordSet.addResultSet(rs, FieldType.MULTI_LINE_TEXT, 'msg')
        exportRecordSet.addResultSet(rs, FieldType.CHECK_BOX, 'likes')

        then:
        exportRecordSet.getInsertSQL(config, jobId) == [
            """|INSERT INTO `tmp_131313` (
            |`total`, `msg`, `likes` ) VALUES (
            |?, ?, ?)
            |""".stripMargin(),
            """|INSERT INTO `tmp_131313` (
            |`total`, `msg`, `likes` ) VALUES (
            |?, ?, ?)
            |""".stripMargin()
        ]
        exportRecordSet.getInsertValues() == [
            [
                expected[0],
                expected[1],
                expected[2]
            ],
            [
                expected[0]+"2",
                expected[1]+"2",
                expected[2]+"2"]
        ]
    }

    def "addResultSet-subtable"() {
        setup:
        def jobId = "131313"
        def config = new KintoneConnectorConfig()
        def fk = 12345l
        def exportRecordSet = new ExportRecordSet(config, jobId)
        def rs = new ResultSet(null)
        def record = new Record()
        record.setId(fk)
        record.setString("userName", "test taro")
        rs.add(record)

        def child1Rs = new ResultSet(null)
        def child1Record = new Record()
        child1Record.setId(fk)
        child1Record.setString("childName", "child taro")
        child1Rs.add(child1Record)
        def child2Rs = new ResultSet(null)
        def child2Record = new Record()
        child2Record.setId(fk)
        child2Record.setString("childName", "child jiro")
        child2Rs.add(child2Record)

        when:
        rs.next()
        exportRecordSet.next()
        exportRecordSet.addResultSet(rs, FieldType.__ID__, '$id')
        exportRecordSet.addResultSet(rs, FieldType.SINGLE_LINE_TEXT, 'userName')
        exportRecordSet.addResultSet(rs, FieldType.SUBTABLE, 'children')

        child1Rs.next()
        child2Rs.next()
        exportRecordSet.getSubTable(fk +'_children').nextSubTableRecord(fk, 'children')
        exportRecordSet.getSubTable(fk +'_children').addResultSet(child1Rs, FieldType.SINGLE_LINE_TEXT, 'childName')
        exportRecordSet.getSubTable(fk +'_children').nextSubTableRecord(fk, 'children')
        exportRecordSet.getSubTable(fk +'_children').addResultSet(child2Rs, FieldType.SINGLE_LINE_TEXT, 'childName')

        then:
        exportRecordSet.getRecords().size() == 1
        exportRecordSet.getRecords().get(0).colNames.size() == 2
        exportRecordSet.getRecords().get(0).colNames.get(0) == '$id'
        exportRecordSet.getRecords().get(0).colNames.get(1) == "userName" // subTableはカラムなし
        exportRecordSet.getRecords().get(0).colValues.size() == 2
        exportRecordSet.getRecords().get(0).colValues.get(0) == fk.toString()
        exportRecordSet.getRecords().get(0).colValues.get(1) == "test taro"

        def subtable1 = exportRecordSet.getRecords().get(0).subTables.get(fk +'_children')
        subtable1.records.size() == 2
        subtable1.records.get(0).foreignKey == fk
        subtable1.records.get(0).colNames.get(0) == "childName"
        subtable1.records.get(0).colValues.get(0) == "child taro"
        subtable1.records.get(1).foreignKey == fk
        subtable1.records.get(1).colNames.get(0) == "childName"
        subtable1.records.get(1).colValues.get(0) == "child jiro"
    }

    def "getInsertSQL-getInsertValues-subtable"() {
        setup:
        def jobId = "131313"
        def config = new KintoneConnectorConfig()
        config.tablePrefix = "tmp_"
        def fk = 12345l
        def exportRecordSet = new ExportRecordSet(config, jobId)
        def rs = new ResultSet(null)
        def record = new Record()
        record.setId(fk)
        record.setString("userName", "test taro")
        rs.add(record)

        def child1Rs = new ResultSet(null)
        def child1Record = new Record()
        child1Record.setId(fk)
        child1Record.setString("childName", "child taro")
        child1Rs.add(child1Record)
        def child2Rs = new ResultSet(null)
        def child2Record = new Record()
        child2Record.setId(fk)
        child2Record.setString("childName", "child jiro")
        child2Rs.add(child2Record)

        when:
        rs.next()
        exportRecordSet.next()
        exportRecordSet.addResultSet(rs, FieldType.__ID__, '$id')
        exportRecordSet.addResultSet(rs, FieldType.SINGLE_LINE_TEXT, 'userName')
        exportRecordSet.addResultSet(rs, FieldType.SUBTABLE, 'children')
        exportRecordSet.addResultSet(rs, FieldType.SUBTABLE, 'children2')

        child1Rs.next()
        child2Rs.next()
        exportRecordSet.getSubTable(fk +'_children').nextSubTableRecord(fk, 'children')
        exportRecordSet.getSubTable(fk +'_children').addResultSet(child1Rs, FieldType.SINGLE_LINE_TEXT, 'childName')
        exportRecordSet.getSubTable(fk +'_children').nextSubTableRecord(fk, 'children')
        exportRecordSet.getSubTable(fk +'_children').addResultSet(child2Rs, FieldType.SINGLE_LINE_TEXT, 'childName')
        exportRecordSet.getSubTable(fk +'_children2').nextSubTableRecord(fk, 'children2')
        exportRecordSet.getSubTable(fk +'_children2').addResultSet(child1Rs, FieldType.SINGLE_LINE_TEXT, 'childName')
        exportRecordSet.getSubTable(fk +'_children2').nextSubTableRecord(fk, 'children2')
        exportRecordSet.getSubTable(fk +'_children2').addResultSet(child2Rs, FieldType.SINGLE_LINE_TEXT, 'childName')

        then:
        exportRecordSet.getInsertSQL(config, jobId) == [
            """|INSERT INTO `tmp_131313` (
                |`\$id`, `userName` ) VALUES (
                |?, ?)
                |""".stripMargin()
        ]

        exportRecordSet.getSubTable(fk +'_children').getInsertSQL(config, jobId).size() == 2
        println exportRecordSet.getSubTable(fk +'_children').getInsertSQL(config, jobId)[0]
        println exportRecordSet.getSubTable(fk +'_children').getInsertSQL(config, jobId)[1]

        exportRecordSet.getSubTable(fk +'_children').getInsertSQL(config, jobId) == [
            """|INSERT INTO `tmp_131313_children` (
            |`tmp_131313_children_fk`, `childName` ) VALUES (
            |?, ?)
            |""".stripMargin(),
            """|INSERT INTO `tmp_131313_children` (
            |`tmp_131313_children_fk`, `childName` ) VALUES (
            |?, ?)
            |""".stripMargin()
        ]

        exportRecordSet.getSubTable(fk +'_children2').getInsertSQL(config, jobId).size() == 2
        exportRecordSet.getSubTable(fk +'_children2').getInsertSQL(config, jobId) == [
            """|INSERT INTO `tmp_131313_children2` (
            |`tmp_131313_children2_fk`, `childName` ) VALUES (
            |?, ?)
            |""".stripMargin(),
            """|INSERT INTO `tmp_131313_children2` (
            |`tmp_131313_children2_fk`, `childName` ) VALUES (
            |?, ?)
            |""".stripMargin()
        ]

        exportRecordSet.getInsertValues() == [
            [
                "12345",
                "test taro"]
        ]

        def subtable1 = exportRecordSet.getRecords().get(0).subTables.get(fk +'_children')
        subtable1.getInsertValues() == [
            ["12345", "child taro"],
            [
                "12345",
                "child jiro"]
        ]
        def subtable2 = exportRecordSet.getRecords().get(0).subTables.get(fk +'_children2')
        subtable2.getInsertValues() == [
            ["12345", "child taro"],
            [
                "12345",
                "child jiro"]
        ]
    }
}
