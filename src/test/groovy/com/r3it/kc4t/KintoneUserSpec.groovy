package com.r3it.kc4t

import spock.lang.Specification

class KintoneUserSpec extends Specification {

    def "createUser"() {
        setup:
        def code = "taro"
        def user

        when:
        user = KintoneUser.createUser(code)

        then:
        user.code == "taro"
    }

    def "createSingleUser"() {
        setup:
        def codes = "taro"
        def list

        when:
        list = KintoneUser.createUserList(codes)

        then:
        !list.empty
        list.get(0).code == "taro"
    }

    def "createUserList"() {
        setup:
        def codes = ["111", "222", "taro"] as String[]
        def list

        when:
        list = KintoneUser.createUserList(codes)

        then:
        !list.empty
        list.get(0).code == "111"
        list.get(1).code == "222"
        list.get(2).code == "taro"
    }
}
