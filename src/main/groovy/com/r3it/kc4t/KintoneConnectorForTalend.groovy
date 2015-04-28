package com.r3it.kc4t

/**
 * kintone Connector for Talend
 */
class KintoneConnectorForTalend {
  def config

  /**
	 configオブジェクトで初期化
   */
  def init(KintoneConnectorConfig config) {
	this.config = config
	
	this.config.jdbcUrl
  }
  
}
