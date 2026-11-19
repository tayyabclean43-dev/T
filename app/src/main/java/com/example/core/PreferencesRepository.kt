package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rankpulse_prefs")

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ThemeColorPalette(val label: String, val labelHi: String, val previewHex: Long) {
    EMERALD("Emerald", "एमरल्ड", 0xFF10B981),
    OCEAN_BLUE("Ocean Blue", "ओशन ब्लू", 0xFF3B82F6),
    SAFFRON_CORAL("Saffron", "केसरिया", 0xFFF97316),
    ROYAL_VIOLET("Royal Violet", "बैंगनी", 0xFF8B5CF6),
    ROSE_CRIMSON("Crimson Rose", "गुलाबी रूबी", 0xFFF43F5E),
    TEAL_SLATE("Midnight Teal", "टीयल", 0xFF14B8A6)
}

enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी")
}

enum class WorkspaceRole(val label: String) {
    OWNER("Owner"),
    MANAGER("Manager"),
    EDITOR("Editor"),
    VIEWER("Viewer");

    val canEditContent: Boolean
        get() = this == OWNER || this == MANAGER || this == EDITOR

    val canReplyOrAnalyze: Boolean
        get() = this == OWNER || this == MANAGER

    val canManageBillingAndTeam: Boolean
        get() = this == OWNER
}

data class UserPreferences(
    val theme: ThemePreference = ThemePreference.DARK,
    val colorPalette: ThemeColorPalette = ThemeColorPalette.EMERALD,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val onboardingShownForUid: String = "",
    val activePreviewRole: WorkspaceRole = WorkspaceRole.OWNER,
    val encryptedVaultNote: String = ""
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_pref")
        val COLOR_PALETTE = stringPreferencesKey("color_palette_pref")
        val LANGUAGE = stringPreferencesKey("language_pref")
        val ONBOARDING_UID = stringPreferencesKey("onboarding_shown_uid")
        val PREVIEW_ROLE = stringPreferencesKey("active_preview_role")
        val ENCRYPTED_VAULT = stringPreferencesKey("encrypted_vault_note")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME] ?: ThemePreference.DARK.name
        val paletteName = prefs[Keys.COLOR_PALETTE] ?: ThemeColorPalette.EMERALD.name
        val langName = prefs[Keys.LANGUAGE] ?: AppLanguage.ENGLISH.name
        val roleName = prefs[Keys.PREVIEW_ROLE] ?: WorkspaceRole.OWNER.name
        UserPreferences(
            theme = ThemePreference.entries.firstOrNull { it.name == themeName } ?: ThemePreference.DARK,
            colorPalette = ThemeColorPalette.entries.firstOrNull { it.name == paletteName } ?: ThemeColorPalette.EMERALD,
            language = AppLanguage.entries.firstOrNull { it.name == langName } ?: AppLanguage.ENGLISH,
            onboardingShownForUid = prefs[Keys.ONBOARDING_UID] ?: "",
            activePreviewRole = WorkspaceRole.entries.firstOrNull { it.name == roleName } ?: WorkspaceRole.OWNER,
            encryptedVaultNote = prefs[Keys.ENCRYPTED_VAULT] ?: ""
        )
    }

    suspend fun setTheme(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setColorPalette(palette: ThemeColorPalette) {
        context.dataStore.edit { it[Keys.COLOR_PALETTE] = palette.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun markOnboardingSeen(uid: String) {
        context.dataStore.edit { it[Keys.ONBOARDING_UID] = uid }
    }

    suspend fun setPreviewRole(role: WorkspaceRole) {
        context.dataStore.edit { it[Keys.PREVIEW_ROLE] = role.name }
    }

    suspend fun saveEncryptedVaultNote(encryptedPayload: String) {
        context.dataStore.edit { it[Keys.ENCRYPTED_VAULT] = encryptedPayload }
    }
}
