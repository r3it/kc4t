package com.r3it.kc4t

import spock.lang.Specification

import com.cybozu.kintone.database.FieldType
import com.cybozu.kintone.database.Record
import com.cybozu.kintone.database.ResultSet

class ExportRecordSetSpec extends Specification {

    def "addResultSet-1record"() {
        setup:
        def exportRecordSet = new ExportRecordSet()
        def rs = new ResultSet(null)
        def record = new Record()
        record.setString("userName", "test taro")
        record.setLong("userId", 123)
        rs.add(record)

        when:
        rs.next()
        exportRecordSet.start()
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
        def expected = [
            "45,678",
            """今日は
いい天気でした""",
            """["選択肢1","選択肢2"]"""
        ]

        def exportRecordSet = new ExportRecordSet()
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
        exportRecordSet.start()
        exportRecordSet.addResultSet(rs, FieldType.CALC, 'total')
        exportRecordSet.addResultSet(rs, FieldType.MULTI_LINE_TEXT, 'msg')
        exportRecordSet.addResultSet(rs, FieldType.CHECK_BOX, 'likes')
        rs.next()
        exportRecordSet.start()
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
}
