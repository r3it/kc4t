package com.r3it.kc4t

/**
 * Export Record DTO
 * 
 * @author nishijima
 *
 */
class ExportRecord {
    def colNames = new ArrayList<String>()
    def colValues = new ArrayList<String>()

    def foreignKey = 0l
    def subTables = new HashMap<String, ExportRecordSet>()
    def subTableColName
}
