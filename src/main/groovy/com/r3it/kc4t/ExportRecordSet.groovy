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

    def current = null
    def records = new ArrayList<ExportRecord>()

    def next() {
        current = new ExportRecord()
        records.add(current)
    }

    def nextSubTableRecord(fk) {
        current = new ExportRecord()
        current.foreignKey = fk
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
                if (! current.subTables.containsKey(rs.getId() +'_'+ name)) {
                    def subTable = new ExportRecordSet()
                    subTable.nextSubTableRecord(rs.getId())
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

    def ExportRecordSet getSubTable(name) {
        if (this.current.subTables.containsKey(name)) {
            return this.current.subTables.get(name)
        }
        return new ExportRecordSet()
    }
}
