package com.r3it.kc4t

/**
 * KintoneConnector Job Result
 * 
 * @author nishijima
 *
 */
class KintoneConnectorJobResult {
    def startTime
    def endTime

    def success = false
    def count = 0

    def jobId
    def schema
    def exportRecordSet
    def Throwable exception
}
