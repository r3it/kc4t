package com.r3it.kc4t

import spock.lang.*

class KintoneConnectorForTalendSpec extends Specification {
  def config = new KintoneConnectorConfig()

  def "JDBCのURLが正しくセットされたか？"() {
	setup:
	config.jdbcUrl = jdbcUrl
	
	expect:
	new KintoneConnectorForTalend().init(config) == result
	
	where:
	jdbcUrl | result
	'jdbc://foobar/val?a=b' | 'jdbc://foobar/val?a=b'
  }
}
