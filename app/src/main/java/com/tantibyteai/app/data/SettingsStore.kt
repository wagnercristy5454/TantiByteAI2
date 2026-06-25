package com.tantibyteai.app.data

import com.tantibyteai.app.model.PersonaProfile

object SettingsStore {
    private var _profile = PersonaProfile()

    val profile: PersonaProfile
        get() = _profile

    fun updateProfile(newProfile: PersonaProfile) {
        _profile = newProfile
    }

    fun resetToDefault() {
        _profile = PersonaProfile()
    }
}
