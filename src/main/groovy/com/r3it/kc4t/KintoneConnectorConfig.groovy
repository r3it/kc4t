package com.r3it.kc4t

class KintoneConnectorConfig {
    def jdbcUrl
    def jdbcDriverClass
    def jdbcUser
    def jdbcPassword

    def tablePrefix
    def jobStatusReportTableName
    def saveTmpTable = true

    def apiToken
    def subDomain
    def appId
    def Long getAppId() {
        return Long.parseLong(appId)
    }

    def guestSpaceId
    def Long getGuestSpaceId() {
        try {
            return Long.parseLong(guestSpaceId)
        } catch (Throwable t) {
            return 0
        }
    }

    def orderByField = 'レコード番号'
    def useRevision = true

    def keyFieldCode
    def importDateFormat
    def importTimeFormat
    def importDateTimeFormat
}
