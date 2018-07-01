package ru.ilvar.whitube

import android.os.Bundle
import android.app.Activity


class ChannelsActivity : Activity() {
    var cpf: ChannelsPreferenceFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display the fragment as the main content.
        cpf = ChannelsPreferenceFragment()
        fragmentManager.beginTransaction().replace(android.R.id.content, cpf).commit()

    }

}
