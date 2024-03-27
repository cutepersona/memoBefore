package com.peng.power.memo.broadcastreceiver

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.Network

import android.net.NetworkCapabilities

import android.net.NetworkRequest
import android.view.Window
import com.peng.power.memo.util.l
import com.peng.power.memo.dialog.NotNetworkDialog


class ConnectionStateMonitor : ConnectivityManager.NetworkCallback() {

    private var notNetWorkDialog: NotNetworkDialog? = null

    private var mContext:Context?=null

    private var networkRequest: NetworkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()

    private var connectivityManager:ConnectivityManager ?= null


    fun enable(context: Context) {
        mContext = context
        connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager?.registerNetworkCallback(networkRequest, this)
    }


    fun disable(){
        connectivityManager?.unregisterNetworkCallback(this)
    }

    // Likewise, you can have a disable method that simply calls ConnectivityManager.unregisterNetworkCallback(NetworkCallback) too.
    override fun onAvailable(network: Network) {
        l.d("on Network Available : $network")
        // Do what you need to do here
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        l.d("on Lost Network : $network")
        showNotNetworkDialog(mContext!!)
    }


    private fun isNetworkAvailable(context: Context) =
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).run {
            getNetworkCapabilities(activeNetwork)?.run {
                hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } ?: false
        }


    private fun showNotNetworkDialog(context: Context) {
        if(notNetWorkDialog == null)
            notNetWorkDialog = NotNetworkDialog(context)

        notNetWorkDialog?.let{
            val window: Window = it.window!!
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setOnDismissListener {
                l.e("Dialog not network onDissmiss")
                notNetWorkDialog = null
            }
            if(!it.isShowing)
                it.show()
        }
    }
}