package com.peng.power.memo.preference

import com.peng.power.memo.util.l
import kotlin.properties.Delegates

class DataUser(dataClass: PreferenceValue.DataUserPreference) {
    private var callback:(()->Unit)?=null
    fun setValueChangedListener(callback:()->Unit){
        this.callback = callback
    }

    var enSeq:Int by Delegates.observable(dataClass.enSeq){ _, _, newValue ->
        l.d("DataUser value changed enseq : $newValue")
        onValueChanged()
    }

    var hqSeq:Int by Delegates.observable(dataClass.hqSeq){_, _, newValue ->
        l.d("DataUser value changed hqSeq : $newValue")
        onValueChanged()
    }

    var brSeq:Int by Delegates.observable(dataClass.brSeq){_,_,newValue ->
        l.d("DataUser value changed brSeq : $newValue")
        onValueChanged()
    }

    var userName:String by Delegates.observable(dataClass.userName){ property, oldValue, newValue ->
        onValueChanged()
    }
    var userId:String by Delegates.observable(dataClass.userId) { property, oldValue, newValue ->
        onValueChanged()
    }

    var sort:Int by Delegates.observable(dataClass.sort) { property, oldValue, newValue ->
        onValueChanged()
    }
    var page:Int by Delegates.observable(dataClass.page) { property, oldValue, newValue ->
        onValueChanged()
    }
    var zoom:Int by Delegates.observable(dataClass.zoom) { property, oldValue, newValue ->
        onValueChanged()
    }
    var menuStatus:Boolean by Delegates.observable(dataClass.menuStatus) { property, oldValue, newValue ->
        onValueChanged()
    }
    var thermal:Int by Delegates.observable(dataClass.thermal) { property, oldValue, newValue ->
        onValueChanged()
    }

    private fun onValueChanged(){
        callback?.let {
            it()
        }
    }

    fun getDataPreference(): PreferenceValue.DataUserPreference {
        val dataUserPreference = PreferenceValue.DataUserPreference()
        dataUserPreference.userName = userName
        dataUserPreference.userId = userId
        dataUserPreference.enSeq = enSeq
        dataUserPreference.hqSeq = hqSeq
        dataUserPreference.brSeq = brSeq
        dataUserPreference.zoom = zoom
        dataUserPreference.page = page
        dataUserPreference.sort = sort
        dataUserPreference.menuStatus = menuStatus
        dataUserPreference.thermal = thermal
        return dataUserPreference
    }
}