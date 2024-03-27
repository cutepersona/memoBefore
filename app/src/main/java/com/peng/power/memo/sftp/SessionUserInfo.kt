package com.peng.power.memo.sftp

import com.jcraft.jsch.UserInfo


class SessionUserInfo(user: String?, host: String?, password: String?, port: Int) : UserInfo {

    /**
     * Creates an instance of SessionUserInfo with the user name, host name,
     * user password and port number, to gain SSH access to remote host.
     *
     * @param user     username for SSH access to remote server
     * @param host     remote server host name
     * @param password password to access server
     * @param port     port number
     */
    private var mPassword: String? = password
    private var mUser: String? = user
    private var mHost: String? = host
    private var mPort = port



    override fun getPassphrase(): String? {
        // TODO
        return null
    }


    /**
     * Gets the username for SSH session login.
     * @return username
     */
    fun getUser(): String? {
        return mUser
    }

    /**
     * Gets the remote host name, or IP address.
     * @return host name, or IP address
     */
    fun getHost(): String? {
        return mHost
    }

    /**
     * Gets the user password for SSH login to server.
     * @return SSH password
     */
    override fun getPassword(): String? {
        return mPassword
    }

    /**
     * Gets the port number of the remote server for SSH access.
     * Usually this is port 22.
     * @return port number
     */
    fun getPort(): Int {
        return mPort
    }

    override fun promptPassphrase(arg0: String?): Boolean {
        // TODO
        return true
    }

    override fun promptPassword(arg0: String?): Boolean {
        // TODO
        return true
    }

    override fun promptYesNo(arg0: String?): Boolean {
        // TODO Auto-generated method stub
        return true
    }

    override fun showMessage(arg0: String?) {
        // TODO Auto-generated method stub
    }

}