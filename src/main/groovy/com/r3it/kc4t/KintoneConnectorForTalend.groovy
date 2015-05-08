package com.r3it.kc4t

import groovy.sql.Sql

import com.cybozu.kintone.database.Connection
import com.cybozu.kintone.database.FieldType
import com.cybozu.kintone.database.Record
import com.cybozu.kintone.database.ResultSet
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive




/**
 * kintone Connector for Talend
 * 
 * @author nishijima
 */
class KintoneConnectorForTalend {

    KintoneConnectorForTalend() {
    }

    def createJobId(dateTime = new Date()) {
        return dateTime.format("yyyyMMddHHmmss")
    }

    def KintoneConnectorJobResult exportFromKintone(KintoneConnectorConfig config, query) {
        def startTime = new Date()
        def result = execExportRestClient(config, query)
        result.startTime = startTime

        // 結果をレポートDBに保存
        reportJobResult(config, result)

        return result
    }

    def KintoneConnectorJobResult exportAllFromKintone(KintoneConnectorConfig config) {
        def startTime = new Date()
        def offset = 0
        def query = "order by レコード番号 asc limit 100 offset 0"
        def totalCount = 0

        try {
            def result = select(createJobId(), config, query)

            def doLoop = result.count > 0
            while (doLoop) {
                totalCount += result.count
                dumpToTable(config, result)

                offset += result.count
                query = "order by レコード番号 asc limit 100 offset $offset"
                def offsetResult = select(createJobId(), config, query)
                doLoop = offsetResult.count > 0
            }
            result.startTime = startTime
            result.count = totalCount
            reportJobResult(config, result)
            return result
        } catch (Throwable t) {
            def result = new KintoneConnectorJobResult()
            result.startTime = startTime
            result.success = false
            result.exception = t

            reportJobResult(config, result)

            return result
        }
    }

    def KintoneConnectorJobResult execExportRestClient(KintoneConnectorConfig config, query) {
        def result = select(createJobId(), config, query)

        if (result.success) {
            try {
                dumpToTable(config, result)
            } catch (Throwable t) {
                result.success = false
                result.exception = t
            }
        }

        return result
    }

    def KintoneConnectorJobResult select(jobId, KintoneConnectorConfig config, query) {
        def result = new KintoneConnectorJobResult()
        result.jobId = jobId

        def schema = new ExportTableSchema()
        def exportRecordSet = new ExportRecordSet(config, jobId)

        def count = 0
        Connection db = null;
        try {
            db = new Connection(config.subDomain, config.apiToken);
            ResultSet rs = db.select(config.appId, query, null);
            while (rs.next()) {
                count++
                exportRecordSet.next()

                if (config.useRevision) {
                    schema.addField(FieldType.__REVISION__, '$revision')
                    exportRecordSet.addResultSet(rs, FieldType.__REVISION__, '$revision')
                    schema.addField(FieldType.__ID__, '$id')
                    exportRecordSet.addResultSet(rs, FieldType.__ID__, '$id')
                }

                Set<String> fieldNames = rs.getFieldNames();
                for (String name : fieldNames) {
                    FieldType fieldType = rs.getFieldType(name);
                    schema.addField(fieldType, name)
                    exportRecordSet.addResultSet(rs, fieldType, name)

                    switch (fieldType) {
                        case FieldType.SUBTABLE:
                            def subtable = exportRecordSet.getSubTable(rs.getId() + '_' + name)
                            def subtableSchema = schema.getSubTable(name)
                            List<Record> subtableRecord = rs.getSubtable(name);
                            for (Record record : subtableRecord) {
                                def subRs = new ResultSet(null)
                                subRs.add(record)
                                subRs.next()
                                subtable.nextSubTableRecord(rs.getId(), name)

                                Set<String> recFields = record.getFieldNames()
                                for (String recFieldName : recFields) {
                                    def subTableRecordFieldType = record.getField(recFieldName).getFieldType()
                                    subtableSchema.addField(subTableRecordFieldType, recFieldName)
                                    subtable.addResultSet(subRs, subTableRecordFieldType, recFieldName)
                                }
                            }

                        default:
                            break;
                    }
                }
            }

            result.schema = schema
            result.exportRecordSet = exportRecordSet
            result.success = true
        } catch (Throwable t) {
            result.exception = t
        } finally {
            if (db != null) {
                db.close()
            }
            result.count = count
        }

        return result
    }

    def dumpToTable(KintoneConnectorConfig config, KintoneConnectorJobResult result) {
        def sql = Sql.newInstance(config.jdbcUrl, config.jdbcUser, config.jdbcPassword, config.jdbcDriverClass)

        try {
            sql.connection.setAutoCommit(false)
            sql.withTransaction {
                // create tables
                sql.execute(result.schema.getSchema(config, result.jobId))
                if (result.schema.hasSubtables()) {
                    result.schema.getSubtableNames().each {
                        sql.execute(result.schema.getSubTable(it).getSubtableSchema(config, result.jobId, it))
                    }
                }

                // insert
                for (def i = 0; i < result.exportRecordSet.getInsertValues().size(); i++) {
                    sql.execute(result.exportRecordSet.getInsertSQL(config, result.jobId)[i],
                            result.exportRecordSet.getInsertValues()[i])
                }
                if (result.exportRecordSet.hasSubtables()) {
                    // 各レコードに紐づくsubTableからRecordを取り出す
                    result.exportRecordSet.getRecords().each { subTblRecord ->
                        subTblRecord.subTables.each { key, subtable ->
                            for (def i = 0; i < subtable.getInsertValues().size(); i++) {
                                sql.execute(subtable.getInsertSQL(config, result.jobId)[i],
                                        subtable.getInsertValues()[i])
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            def closeSql = Sql.newInstance(config.jdbcUrl, config.jdbcUser, config.jdbcPassword, config.jdbcDriverClass)
            def mainTableName = result.schema.getTableName(config, result.jobId)
            closeSql.execute("DROP TABLE "+ mainTableName)
            result.schema.getSubtableNames().each {
                closeSql.execute("DROP TABLE "+ mainTableName + "_" + it)
            }
            throw t
        }
    }

    def reportJobResult(KintoneConnectorConfig config, KintoneConnectorJobResult result) {
        def sql = Sql.newInstance(config.jdbcUrl, config.jdbcUser, config.jdbcPassword, config.jdbcDriverClass)

        try {
            sql.connection.setAutoCommit(false)
            sql.withTransaction {
                sql.execute """|CREATE TABLE IF NOT EXISTS `$config.jobStatusReportTableName` (
                    |  `pk` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
                    |  `jobId` varchar(255) DEFAULT NULL,
                    |  `success` varchar(10) DEFAULT NULL,
                    |  `startTime` datetime DEFAULT NULL,
                    |  `endTime` datetime NOT NULL,
                    |  `tableName` varchar(512) DEFAULT NULL,
                    |  `subTableNames` text,
                    |  `recordCount` bigint(20) DEFAULT '0',
                    |  `exception` text,
                    |  PRIMARY KEY (`pk`)
                    |)""".stripMargin()

                def insert = """|INSERT INTO `$config.jobStatusReportTableName` 
                    | (`pk`, `jobId`, `success`, `startTime`, `endTime`, `tableName`, `subTableNames`, `recordCount`, `exception`) 
                    | VALUES 
                    | (NULL, ?, ?, ?, NOW(), ?, ?, ?, ?)""".stripMargin()

                def values = [
                    result.jobId,
                    result.success.toString(),
                    result.startTime?.format("yyyy-MM-dd HH:mm:ss")
                ]
                values << result.schema?.getTableName(config, result.jobId)
                if (result.schema?.hasSubtables()) {
                    JsonArray subTables = new JsonArray();
                    result.schema.getSubtableNames().each {
                        subTables.add(new JsonPrimitive(result.schema.getTableName(config, result.jobId) + "_" + it))
                    }
                    values << new Gson().toJson(subTables)
                } else {
                    values << null
                }
                values << result.count
                if (result.exception) {
                    def sw = new StringWriter()
                    def pw = new PrintWriter(sw)
                    result.exception.printStackTrace(pw)
                    values << sw.toString()
                } else {
                    values << null
                }

                sql.execute(insert, values)
            }
        } catch (Throwable t) {
            t.printStackTrace()
            throw t
        }

    }
}
