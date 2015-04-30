package com.r3it.kc4t




/**
 * kintone Connector for Talend
 * 
 * @author nishijima
 */
class KintoneConnectorForTalend {
    def config

    KintoneConnectorForTalend(KintoneConnectorConfig config) {
        this.config = config
    }

    def createJobId(dateTime = new Date()) {
        return dateTime.format("yyyyMMddHHmmss")
    }
}
