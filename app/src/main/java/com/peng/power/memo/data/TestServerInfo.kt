package com.peng.power.memo.data

class TestServerInfo {
    // SamrtG 내부테스트 서버 (그룹 : 와트)
    private val smartgAppDetailJson = """
                {
                    "socket_url":"http://smartg.powertalk.kr:8301",
                    "sftp_host":"106.10.33.94",
                    "sftp_user":"sftpuser01",
                    "sftp_user_pw":"Powereng#2501",
                    "sftp_port":22,
                    "sftp_root_folder":"smartg/",
                    "sftp_url":"https://smartgcloud.powertalk.kr/sftp/smartg/"
                }
            """.trimIndent()

    //"sftp_url":"https://smartgcloud.powertalk.kr/sftp/smartg/"




    private val wattAppDetailJson = """
                {
                    "socket_url":"http://wattcloud.powertalk.kr:8351",
                    "sftp_host":"106.10.33.94",
                    "sftp_user":"sftpuser01",
                    "sftp_user_pw":"Powereng#2501",
                    "sftp_port":22,
                    "sftp_root_folder":"watt",
                    "sftp_url":"https://wattcloud.powertalk.kr/sftp/watt/"
                }
            """.trimIndent()


    private val utcTimeTestAppDetailJson = """
                {
                    "socket_url":"http://powertalktwo.powertalk.kr:8132",
                    "sftp_host":"106.10.41.166",
                    "sftp_user":"sftpuser01",
                    "sftp_user_pw":"Powereng#2501",
                    "sftp_port":22,
                    "sftp_root_folder":"powertalktwo",
                    "sftp_url":"https://powertalktwo.powertalk.kr:8322/storage/powermemo/powertalktwo/"
                }
            """.trimIndent()


    //106.10.33.94
    private val powertalktwoAppDetailJson = """
                {
                    "socket_url":"http://106.10.41.166:8132",
                    "sftp_host":"106.10.41.166",
                    "sftp_user":"sftpuser01",
                    "sftp_user_pw":"Powereng#2501",
                    "sftp_port":22,
                    "sftp_root_folder":"powertalktwo",
                    "sftp_url":"https://powertalktwo.powertalk.kr/storage/powermemo/powertalktwo/"
                }
            """.trimIndent()

    private val tanjungjatyAppDetailJson = """
                {
                    "socket_url":"http://smart.kpjb.co.id:8500",
                    "sftp_host":"49.128.180.116",
                    "sftp_user":"sftpuser01",
                    "sftp_user_pw":"Powereng#2501",
                    "sftp_port":8800,
                    "sftp_root_folder":"kpjb",
                    "sftp_url":"https://smartcloud.powertalk.co.kr:8200/cloud/sftp/kpjb/" 
                }
            """.trimIndent()


    private val meetAppDetailJson = """
                {
                    "socket_url":"http://meetcloud.watttalk.kr:8332",
                    "sftp_host":"106.10.33.94",
                    "sftp_user":"sftpuser01",
                    "sftp_user_pw":"Powereng#2501",
                    "sftp_port":"7975",
                    "sftp_root_folder":"meet",
                    "sftp_url":"https://meetcloud.watttalk.kr/sftp/meet/"
                    "thermal":"0"
                }
            """.trimIndent()

    private val hanilAppDetailJson = """
                {
                    "socket_url":"http://safetymanager.hanil.com:8203",
                    "sftp_host":"49.50.173.32",
                    "sftp_user":"sftpuser01",
                    "sftp_user_pw":"Powereng#2501",
                    "sftp_port":"22",
                    "sftp_root_folder":"hanil",
                    "sftp_url":"https://safetymanager.hanil.com/sftp/hanil/"
                }
            """.trimIndent()

    private val korailAppDetailJson = """
                {
                    "socket_url":"http://koraildev.watttalk.kr:8203",
                    "sftp_host":"210.97.43.45",
                    "sftp_user":"sftpuser01",
                    "sftp_user_pw":"Watt#030e",
                    "sftp_port":"7975",
                    "sftp_root_folder":"sftpuser01",
                    "sftp_url":"https://koraildev.watttalk.kr/sftp/watt/"
                }
            """.trimIndent()




    val smartg = serverInfo("yhkim", "김윤호", 3, 3,3, smartgAppDetailJson)

    val smartg_glass1 = serverInfo("exsg1", "글라스1", 1,1,1, smartgAppDetailJson)


    // Watt 내부테스트 서버 (그룹 : s=oil)
    val watt_soil = serverInfo("ksh", "와트지원", 5, 4,4, wattAppDetailJson)


    // WAtt 내부테스트 서버 (그룹 : 파워)
    val watt_dev1 = serverInfo("dev1", "개발1", 1, 1,1, wattAppDetailJson)


    // UTC Time Test 임시 서버
    val watt_test_utc = serverInfo("admin","관리자", 1, 1, 1, utcTimeTestAppDetailJson)

    // Powertalk Two 서버
    val powertalk_two = serverInfo("p2test2","p2test2", 1,1,1, powertalktwoAppDetailJson)

    // 탄중자티 서버
    val tanjungjaty = serverInfo("noah","Noah",1,1,1,tanjungjatyAppDetailJson)


    // Meet서버
//    val meet_test = serverInfo("hmkim", "김홍민", 13,13,22, meetAppDetailJson)
    val meet_test = serverInfo("hong", "홍길동", 13,13,22, meetAppDetailJson)

    // hanil서버
    val hanil_test = serverInfo("sslee", "이순신", 3,8,11, hanilAppDetailJson)

    // korailDEV서버
    val korail_dev = serverInfo("test6", "KorailWeb", 3,4,5, korailAppDetailJson)

    data class serverInfo(val userId:String, val userName:String, val enSeq:Int=-1, val hqSeq:Int=-1, val brSeq:Int=-1, val appDetailJson:String)

}