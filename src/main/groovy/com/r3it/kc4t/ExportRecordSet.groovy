package com.r3it.kc4t

import com.cybozu.kintone.database.FieldType
import com.cybozu.kintone.database.ResultSet
import com.google.gson.Gson


/**
 * Export table RecordSet object
 * 
 * @author nishijima
 *
 */
class ExportRecordSet {
    def DATE_FORMAT = "yyyy-MM-dd"
    def TIME_FORMAT = "HH:mm"
    def DATETIME_FORMAT = "yyyy-MM-ddTHH:mm:ssZ"

    def config, jobId
    def current = null
    def records = new ArrayList<ExportRecord>()

    ExportRecordSet(KintoneConnectorConfig config, jobId) {
        this.config = config
        this.jobId = jobId
    }

    def next() {
        current = new ExportRecord()
        records.add(current)
    }

    def nextSubTableRecord(fk, subTableColName) {
        current = new ExportRecord()
        current.foreignKey = fk
        current.subTableColName = subTableColName
        records.add(current)
    }

    def addResultSet(ResultSet rs, FieldType type, name) {
        def gson = new Gson()

        current.getColNames().add(name)
        def currentValues = current.getColValues()
        switch (type) {
            case FieldType.SINGLE_LINE_TEXT:
                currentValues.add(rs.getString(name))
                break
            case FieldType.NUMBER:
                currentValues.add(rs.getLong(name).toString())
                break
            case FieldType.CALC:
                currentValues.add(rs.getString(name))
                break
            case FieldType.MULTI_LINE_TEXT:
                currentValues.add(rs.getString(name))
                break
            case FieldType.RICH_TEXT:
                currentValues.add(rs.getString(name))
                break
            case FieldType.CHECK_BOX:
                currentValues.add(rs.getString(name))
                break
            case FieldType.RADIO_BUTTON:
                currentValues.add(rs.getString(name))
                break
            case FieldType.DROP_DOWN:
                currentValues.add(rs.getString(name))
                break
            case FieldType.MULTI_SELECT:
                currentValues.add(rs.getString(name))
                break
            case FieldType.FILE:
            // TODO FILE field handling
                break
            case FieldType.DATE:
                currentValues.add(rs.getDate(name).toString(DATE_FORMAT))
                break
            case FieldType.TIME:
                currentValues.add(rs.getDateTime(name).toString(TIME_FORMAT))
                break
            case FieldType.DATETIME:
                currentValues.add(rs.getDateTime(name).toString(DATETIME_FORMAT))
                break
            case FieldType.USER_SELECT:
                currentValues.add(rs.getString(name))
                break
            case FieldType.LINK:
                currentValues.add(rs.getString(name))
                break
            case FieldType.CATEGORY:
                currentValues.add(rs.getString(name))
                break
            case FieldType.STATUS:
                currentValues.add(rs.getString(name))
                break
            case FieldType.RECORD_NUMBER:
                currentValues.add(rs.getLong(name).toString())
                break
            case FieldType.CREATOR:
                currentValues.add(rs.getString(name))
                break
            case FieldType.CREATED_TIME:
                currentValues.add(rs.getDateTime(name).toString(DATETIME_FORMAT))
                break
            case FieldType.MODIFIER:
                currentValues.add(rs.getString(name))
                break
            case FieldType.UPDATED_TIME:
                currentValues.add(rs.getDateTime(name).toString(DATETIME_FORMAT))
                break
            case FieldType.STATUS_ASSIGNEE:
                currentValues.add(rs.getString(name))
                break
            case FieldType.SUBTABLE:
                current.getColNames().remove(current.getColNames().size() - 1)
                if (! current.subTables.containsKey(rs.getId() +'_'+ name)) {
                    def subTable = new ExportRecordSet(this.config, this.jobId)
                    subTable.nextSubTableRecord(rs.getId(), name)
                    current.subTables.put(rs.getId() +'_'+ name, subTable)
                }
                break
            case FieldType.__REVISION__:
                currentValues.add(rs.getRevision().toString())
                break
            case FieldType.__ID__:
                currentValues.add(rs.getId().toString())
                break

            default:
                break
        }

    }

    def List<String> getInsertSQL(KintoneConnectorConfig config, jobId) {
        def result = new ArrayList<String>()

        this.records.each { record ->
            def insertPrefix = "INSERT INTO `"+ config.tablePrefix + jobId +"` (\n"
            def names = new StringBuilder()

            if (current.foreignKey > 0) {
                def uniqName = this.config.tablePrefix + this.jobId +"_"+ current.subTableColName
                names.append("`"+ uniqName +"_fk`")
            }
            record.colNames.each { colName ->
                if (names.length() > 0) {
                    names.append(", ")
                }
                names.append("`"+ colName +"`")
            }
            def insertSuffix = " ) VALUES (\n"

            def values = new StringBuilder()
            record.colValues.each { colValue ->
                if (values.length() > 0) {
                    values.append(", ")
                }
                values.append("?")
            }
            if (current.foreignKey > 0) {
                values.append(", ?")
            }
            def footer = ")\n"
            result.add(insertPrefix + names.toString() + insertSuffix + values.toString() + footer)
        }

        return result
    }

    def List<List<String>> getInsertValues() {
        def result = new ArrayList<List<String>>()

        this.records.each { record ->
            def recordValues = new ArrayList<String>()
            if (current.foreignKey > 0) {
                recordValues.add(current.foreignKey.toString())
            }
            record.colValues.each { colValue ->
                recordValues.add(colValue)
            }
            result.add(recordValues)
        }

        return result

    }

    def ExportRecordSet getSubTable(name) {
        if (this.current.subTables.containsKey(name)) {
            return this.current.subTables.get(name)
        }
        return new ExportRecordSet()
    }
}
