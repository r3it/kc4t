package com.r3it.kc4t

import groovy.sql.Sql

import java.text.SimpleDateFormat

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
        def query = " order by $config.orderByField asc limit 100 offset 0"
        def totalCount = 0

        try {
            def result = select(createJobId(), config, query)

            def doLoop = result.count > 0
            while (doLoop) {
                totalCount += result.count
                dumpToTable(config, result)

                offset += result.count
                query = " order by $config.orderByField asc limit 100 offset $offset"
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
            if (config.guestSpaceId > 0) {
                db.setGuestSpaceId(config.guestSpaceId)
            }
            ResultSet rs = db.select(config.appId, query, null);
            while (rs.next()) {
                count++
                exportRecordSet.next()

                if (config.useRevision) {
                    schema.addField(FieldType.__REVISION__, '$revision')
                    exportRecordSet.addResultSet(rs, FieldType.__REVISION__, '$revision')
                }
                schema.addField(FieldType.__ID__, '$id')
                exportRecordSet.addResultSet(rs, FieldType.__ID__, '$id')

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

                                // revision always return -1. why?
                                //                                if (config.useRevision) {
                                //                                    subtableSchema.addField(FieldType.__REVISION__, '$revision')
                                //                                    subtable.addResultSet(subRs, FieldType.__REVISION__, '$revision')
                                //                                }
                                subtableSchema.addField(FieldType.__ID__, '$id')
                                subtable.addResultSet(subRs, FieldType.__ID__, '$id')

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
            t.printStackTrace()
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
                result.mainTableName = result.schema.getTableName(config, result.jobId)
                sql.execute(result.schema.getSchema(config, result.jobId))
                if (result.schema.hasSubtables()) {
                    def subTableNames = []
                    result.schema.getSubtableNames().each {
                        subTableNames << result.mainTableName +'_'+ it
                        sql.execute(result.schema.getSubTable(it).getSubtableSchema(config, result.jobId, it))
                    }
                    result.subTableNames = subTableNames.join(',')
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

    def KintoneConnectorJobResult upsertKintone(KintoneConnectorConfig config, Map<String, String> columns) {
        def result = new KintoneConnectorJobResult()

        def value = columns.get(config.keyFieldCode)
        def query = """$config.keyFieldCode = "$value" """
        Connection db = null;
        try {
            db = new Connection(config.subDomain, config.apiToken);
            if (config.guestSpaceId > 0) {
                db.setGuestSpaceId(config.guestSpaceId)
            }
            ResultSet rs = db.select(config.appId, query, null);
            if (rs.next()) {
                updateKintone(config, db, rs, columns)
            } else {
                result.insertedId = insertKintone(config, db, rs, columns)
            }

        } catch (Throwable t) {
            t.printStackTrace()
            throw t

        } finally {
            if (db != null) {
                db.close()
            }
        }

        result.success = true
        return result
    }

    def updateKintone(KintoneConnectorConfig config, Connection db, ResultSet rs, Map<String, String> columns) {
        def record = new Record(rs.getId())
        refillToRecord(false, config, rs, record, columns);
        record.setRevision(-1)
        db.update(config.getAppId(), rs.getId(), record)
    }

    def insertKintone(KintoneConnectorConfig config, Connection db, ResultSet rs, Map<String, String> columns) {
        def record = new Record()
        refillToRecord(false, config, null, record, columns);
        return db.insert(config.getAppId(), record)
    }

    def refillToRecord(insert = true, KintoneConnectorConfig config, ResultSet rs, Record record, Map<String, String> columns) {
        def dateFormat, timeFormat, dateTimeFormat
        if (config.getImportDateFormat()) {
            dateFormat = new SimpleDateFormat(config.getImportDateFormat());
        }
        if (config.getImportTimeFormat()) {
            timeFormat = new SimpleDateFormat(config.getImportTimeFormat());
        }
        if (config.getImportDateTimeFormat()) {
            dateTimeFormat = new SimpleDateFormat(config.getImportDateTimeFormat());
        }

        columns.each { k, v ->
            def strValue = v.toString()
            if (rs == null) {
                record.setString(k, strValue)
            } else {
                if (rs.getFieldNames().contains(k)) {
                    FieldType fieldType = rs.getFieldType(k)

                    switch (fieldType) {
                        case FieldType.SINGLE_LINE_TEXT:
                        case FieldType.NUMBER:
                        case FieldType.CALC:
                        case FieldType.MULTI_LINE_TEXT:
                        case FieldType.RICH_TEXT:
                        case FieldType.CHECK_BOX:
                        case FieldType.RADIO_BUTTON:
                        case FieldType.DROP_DOWN:
                        case FieldType.MULTI_SELECT:
                        case FieldType.USER_SELECT:
                        case FieldType.LINK:
                        case FieldType.CATEGORY:
                        case FieldType.STATUS:
                        case FieldType.STATUS_ASSIGNEE:
                            record.setString(k, strValue)
                            break

                        case FieldType.FILE:
                        case FieldType.SUBTABLE:
                        // TODO not support
                            break

                        case FieldType.DATE:
                            record.setDate(k, dateFormat.parse(strValue))
                            break
                        case FieldType.TIME:
                        // FIXME
                            record.setString(k, strValue)
                            break
                        case FieldType.DATETIME:
                            record.setDateTime(k, dateTimeFormat.parse(strValue))
                            break

                        case FieldType.CREATOR:
                        case FieldType.MODIFIER:
                        case FieldType.CREATED_TIME:
                        case FieldType.UPDATED_TIME:
                        // TODO if insert mode, i can set values
                            break

                        case FieldType.RECORD_NUMBER:
                        case FieldType.__REVISION__:
                        case FieldType.__ID__:
                        // not writable
                            break

                        default:
                            break
                    }
                }
            }

        }
    }
}
