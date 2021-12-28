package com.nbow.advanceeditorpro

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.nbow.advanceeditorpro.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.File
import java.lang.StringBuilder

class MyViewModel(application: Application) : AndroidViewModel(application) {
    private val fragmentList = MutableLiveData<MutableList<EditorFragment>>(arrayListOf())

    private val repository : Repository = Repository(application)
    private  var recentFileList = MutableLiveData(mutableListOf<RecentFile>())
    var saveCount = 0;

    //    var currentTab : Int = -1
    var currentTabIndex = -1
    var isWrap = false
    var isHistoryLoaded = MutableLiveData(false)

    private val TAG = "MyViewModel"

    init {
        loadHistory(application.applicationContext)
        loadRecentFile()
        val preferences = PreferenceManager.getDefaultSharedPreferences(application)
        isWrap = preferences.getBoolean("word_wrap",true)
    }

    fun getRecentFileList(): LiveData<MutableList<RecentFile>> {
        return recentFileList
    }

    private fun loadRecentFile() {
        viewModelScope.launch(Dispatchers.IO) {
            recentFileList.postValue(repository.getRecentFileList())
            Log.e(TAG,"recent file list size ${recentFileList.value!!.size}")

        }
    }

    fun addHistories(context: Context){

        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllHistory(context)
            for(frag : Fragment in fragmentList.value!!){
                val editorFragment = frag as EditorFragment
                val uniqueFileName = editorFragment.getFileName() + (0..10000).random()
                val file = File(context.filesDir, uniqueFileName)
                if (!file.exists()) file.createNewFile()
                context.openFileOutput(uniqueFileName, Context.MODE_PRIVATE).use {
                    it.write(
                        editorFragment.getEditTextData().toString().toByteArray())
                }

                val uriString=editorFragment.getUri().toString()
                val history = History(
                    0,
                   uriString= uriString,
                    fileName = uniqueFileName,
                    realFileName = editorFragment.getFileName(),
                    hasUnsavedData = editorFragment.hasUnsavedChanges.value ?: true,
                )
                Log.e(TAG, "saving new file to databse: file id ${history.historyId}")
                repository.addHistory(history)
            }

        }
    }


    fun loadHistory( context: Context){

        viewModelScope.launch(Dispatchers.IO) {

            val historyList:MutableList<History> = repository.getHistory()
            Log.e("view model","size history ${historyList.size} fragment list size : ${fragmentList.value!!.size}")

            for(history in historyList) {
                val uri: Uri = Uri.parse(history.uriString)
                var path = uri.path!!

                if(uri==Uri.parse(""))
                    path="note"

                val listOfLines: MutableList<String> = arrayListOf()
                val listOfPageData: MutableList<StringBuilder> = arrayListOf()

                context.openFileInput(history.fileName).bufferedReader().forEachLine { line ->
                    listOfLines.add(line)
                }
                var temp = StringBuilder("")
                var count = 0
                for (line in listOfLines) {
                    temp.append(line)
                    count++
                    if (count >= 500 || temp.length >= 100000) {
                        listOfPageData.add(temp)
                        count = 0
                        temp = StringBuilder()
                    } else temp.append("\n")
                }
                if (temp.isNotEmpty()) {
                    listOfPageData.add(temp)
                }
                if (listOfLines.size == 0) {
                    listOfPageData.add(temp)
                }


                Log.e(TAG, "loadHistory: total pages ${listOfPageData.size}", )
                val datafile = DataFile(
                    history.realFileName,
                    path,
                    uri,listOfPageData)
                val frag = EditorFragment(datafile,  history.hasUnsavedData)
                Log.e(TAG, "loadHistory: hasUnsavedData : ${history.hasUnsavedData}")
                (fragmentList.value ?: arrayListOf()).add(frag)
            }
            isHistoryLoaded.postValue(true)

        }

    }



    fun getFragmentList(): LiveData<MutableList<EditorFragment>> {
        return fragmentList
    }

    fun setFragmentList(fragmentList : MutableList<EditorFragment>){
        this.fragmentList.value = fragmentList
    }

    fun addRecentFile(recentFile: RecentFile)
    {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveToRecentFile(recentFile)
        }
    }

    fun deleteRecentFile(recentFile: RecentFile)
    {
        viewModelScope.launch(IO){
            repository.deleteRecentFile(recentFile)
        }
    }

    fun deleteAllRecentFile() {
        viewModelScope.launch(IO) {
            repository.deleteAllRecentFile()
        }
    }


}