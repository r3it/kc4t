package com.r3it.kc4t


class KintoneUser {
    def code;

    static KintoneUser createUser(String code) {
        return new KintoneUser(code: code)
    }

    static List<KintoneUser> createUserList(String... codes) {
        def result = new ArrayList<KintoneUser>()
        codes.each {
            result.add(KintoneUser.createUser(it))
        }
        return result
    }
}
