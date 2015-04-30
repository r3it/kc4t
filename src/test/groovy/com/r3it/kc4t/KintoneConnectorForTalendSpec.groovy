package com.r3it.kc4t

import spock.lang.*

class KintoneConnectorForTalendSpec extends Specification {
    def config = new KintoneConnectorConfig()

    def "JDBCのURLが正しくセットされたか？"() {
        setup:
        config.jdbcUrl = jdbcUrl

        expect:
        def con = new KintoneConnectorForTalend(config)
        con.config.jdbcUrl == result

        where:
        jdbcUrl | result
        'jdbc://foobar/val?a=b' | 'jdbc://foobar/val?a=b'
    }

    def "createJobJdテスト"() {
        setup:
        def con = new KintoneConnectorForTalend(config)

        expect:
        con.createJobId(date) == result

        where:
        date | result
        new Date("2015/05/01 12:34:56") | "20150501123456"
    }
}
