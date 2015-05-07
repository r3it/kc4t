package com.r3it.kc4t

import groovy.sql.Sql

import com.cybozu.kintone.database.Connection
import com.cybozu.kintone.database.FieldType
import com.cybozu.kintone.database.Record
import com.cybozu.kintone.database.ResultSet




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

    def exportFromKintone(KintoneConnectorConfig config, query) {
        def jobResult = execExportRestClient(config, query)

        // TODO 結果をレポートDBに保存
    }

    def KintoneConnectorJobResult execExportRestClient(KintoneConnectorConfig config, query) {
        def result = select(createJobId(), config, query)
        if (result.success) {
            // dump to RDB
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

        Connection db = null;
        try {
            db = new Connection(config.subDomain, config.apiToken);
            ResultSet rs = db.select(config.appId, query, null);
            while (rs.next()) {
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
        }

        return result
    }

    def dumpToTable(KintoneConnectorConfig config, KintoneConnectorJobResult result) {
        def sql = Sql.newInstance(config.jdbcUrl, config.jdbcUser, config.jdbcPassword, config.jdbcDriverClass)

        try {
            sql.getConnection().setAutoCommit(false)
            sql.withTransaction {
                sql.execute('SET AUTOCOMMIT FALSE')
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
                    result.exportRecordSet.getSubtableNames().each { subtableName ->
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
            }
        } catch (e) {
            sql.getConnection().rollback()
            throw e
        }
    }
}
