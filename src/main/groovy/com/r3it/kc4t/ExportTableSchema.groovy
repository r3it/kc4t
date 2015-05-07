package com.r3it.kc4t

import com.cybozu.kintone.database.FieldType
import com.google.gson.Gson


/**
 * Export table schema object
 * 
 * @author nishijima
 *
 */
class ExportTableSchema {
    def fieldNames = new ArrayList<String>()
    def schema = new ArrayList<String>()
    def fieldTypes = new ArrayList<FieldType>()

    def subTables = new HashMap<String, ExportTableSchema>()

    def addField(FieldType type, name) {
        if (! fieldNames.contains(name)) {
            fieldNames.add(name)
            fieldTypes.add(type)

            switch (type) {
                case FieldType.SINGLE_LINE_TEXT:
                    schema.add("`"+ name +"` text")
                    break
                case FieldType.NUMBER:
                    schema.add("`"+ name +"` bigint(20) DEFAULT NULL")
                    break
                case FieldType.CALC:
                    schema.add("`"+ name +"` text")
                    break
                case FieldType.MULTI_LINE_TEXT:
                    schema.add("`"+ name +"` text")
                    break
                case FieldType.RICH_TEXT:
                    schema.add("`"+ name +"` text")
                    break
                case FieldType.CHECK_BOX:
                    schema.add("`"+ name +"` text")
                    break
                case FieldType.RADIO_BUTTON:
                    schema.add("`"+ name +"` text")
                    break
                case FieldType.DROP_DOWN:
                    schema.add("`"+ name +"` text")
                    break
                case FieldType.MULTI_SELECT:
                    schema.add("`"+ name +"` text")
                    break
                case FieldType.FILE:
                // TODO FILE field handling
                    break
                case FieldType.DATE:
                    schema.add("`"+ name +"` date DEFAULT NULL")
                    break
                case FieldType.TIME:
                    schema.add("`"+ name +"` time DEFAULT NULL")
                    break
                case FieldType.DATETIME:
                    schema.add("`"+ name +"` datetime DEFAULT NULL")
                    break
                case FieldType.USER_SELECT:
                    schema.add("`"+ name +"` text")
                    break
                case FieldType.LINK:
                    schema.add("`"+ name +"` text")
                    break
                case FieldType.CATEGORY:
                    schema.add("`"+ name +"` text")
                    break
                case FieldType.STATUS:
                    schema.add("`"+ name +"` text")
                    break
                case FieldType.RECORD_NUMBER:
                    schema.add("`"+ name +"` bigint(20) DEFAULT NULL")
                    break
                case FieldType.CREATOR:
                    schema.add("`"+ name +"` text")
                    break
                case FieldType.CREATED_TIME:
                    schema.add("`"+ name +"` datetime DEFAULT NULL")
                    break
                case FieldType.MODIFIER:
                    schema.add("`"+ name +"` text")
                    break
                case FieldType.UPDATED_TIME:
                    schema.add("`"+ name +"` datetime DEFAULT NULL")
                    break
                case FieldType.STATUS_ASSIGNEE:
                    schema.add("`"+ name +"` text")
                    break
                case FieldType.SUBTABLE:
                    if (! subTables.containsKey(name)) {
                        subTables.put(name, new ExportTableSchema())
                    }
                    break
                case FieldType.__REVISION__:
                    schema.add("`"+ name +"` bigint(20) DEFAULT NULL")
                    break
                case FieldType.__ID__:
                    schema.add("`"+ name +"` bigint(20) DEFAULT NULL")
                    break

                default:
                    break
            }
        }
    }


    def getTableName(KintoneConnectorConfig config, jobId) {
        return config.tablePrefix + jobId
    }

    def getSchema(KintoneConnectorConfig config, jobId) {
        def header = "CREATE TABLE `"+ getTableName(config, jobId) +"` (\n"

        def buf = new StringBuilder()
        this.schema.each {
            if (buf.length() > 0) {
                buf.append(",\n")
            }
            buf.append(it)
        }

        def footer = "\n)"
        return header + buf.toString() + footer
    }

    def hasSubtables() {
        return this.subTables.size() > 0
    }

    def getSubtableNames() {
        return this.subTables.keySet().toArray()
    }

    def getSubtableSchema(KintoneConnectorConfig config, jobId, name) {
        def uniqName = config.tablePrefix + jobId +"_"+ name
        def header = "CREATE TABLE `"+ uniqName +"` (\n"

        def buf = new StringBuilder()
        buf.append("`"+ uniqName +"_fk` bigint(20) NOT NULL")
        this.schema.each {
            buf.append(",\n")
            buf.append(it)
        }

        def footer = "\n)"
        return header + buf.toString() + footer
    }

    def getFieldTypes() {
        def gson = new Gson()
        return gson.toJson(this.fieldTypes)
    }

    def ExportTableSchema getSubTable(name) {
        if (this.subTables.containsKey(name)) {
            return this.subTables.get(name)
        }
        return new ExportTableSchema()
    }
}
