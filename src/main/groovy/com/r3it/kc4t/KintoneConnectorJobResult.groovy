package com.r3it.kc4t

/**
 * KintoneConnector Job Result
 * 
 * @author nishijima
 *
 */
class KintoneConnectorJobResult {
    def success = false
    def jobId
    def schema
    def exportRecordSet
    def Throwable exception
}
