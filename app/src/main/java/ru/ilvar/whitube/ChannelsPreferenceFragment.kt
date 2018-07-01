package ru.ilvar.whitube

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import org.json.JSONArray
import java.net.URL

class ChannelsPreferenceFragment : PreferenceFragment() {
    fun parseLists(json: String): MutableList<Channel> {
        val allLists = mutableListOf<Channel>()
        val jsonList = JSONArray(json)

        for (i in 0..(jsonList.length() - 1)) {
            val vidObj = jsonList.getJSONObject(i)
            val listId = vidObj.getString("list_id")
            val title = vidObj.getString("title")
            val description = vidObj.getString("description")
            val thumbnail = vidObj.getString("thumbnail")
            allLists.add(i, Channel(listId, title, description, thumbnail))
        }
        return allLists
    }

    fun downloadLists(sharedPref: SharedPreferences, activity: Activity): MutableList<Channel> {
        val json = URL("https://pacific-cove-93657.herokuapp.com/lists/").readText()
        with (sharedPref.edit()) {
            putString(activity.getString(R.string.all_channels), json)
            apply()
        }
        return parseLists(json)
    }

    fun loadChannels(activity: Activity) {
        val sharedPref = activity.application.getSharedPreferences("ALP", Context.MODE_PRIVATE) ?: return
        val defaultValue: MutableSet<String> = hashSetOf()
        val selectedLists: MutableSet<String> = sharedPref.getStringSet(activity.getString(R.string.selected_channels), defaultValue)

        val allListsDefault = ""
        val allListsJson = sharedPref.getString(activity.getString(R.string.all_channels), allListsDefault)

        val allLists = if (allListsJson == allListsDefault) {
            downloadLists(sharedPref, activity)
        } else {
            Thread({
                downloadLists(sharedPref, activity)
            }).start()
            parseLists(allListsJson)
        }

        addPreferencesFromResource(R.xml.pref_general)

        //fetch the item where you wish to insert the CheckBoxPreference, in this case a PreferenceCategory with key "targetCategory"
        val targetCategory = findPreference("alp_channels") as PreferenceCategory

        allLists.forEach { l ->
            //create one check box for each setting you need
            val checkBoxPreference = CheckBoxPreference(this.activity)
            //make sure each key is unique
            checkBoxPreference.key = l.listId
            checkBoxPreference.title = l.title
            checkBoxPreference.isChecked = selectedLists.contains(l.listId)
            checkBoxPreference.setOnPreferenceChangeListener { preference, newValue ->
                if (newValue.equals(true)) {
                    selectedLists.add(preference.key)
                }
                if (newValue.equals(false)) {
                    selectedLists.remove(preference.key)
                }

                with (sharedPref.edit()) {
                    putStringSet(activity.getString(R.string.selected_channels), selectedLists)
                    apply()
                }
                true
            }

            targetCategory.addPreference(checkBoxPreference)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadChannels(this.activity)
    }
}