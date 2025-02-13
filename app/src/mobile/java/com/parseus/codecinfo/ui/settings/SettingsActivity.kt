package com.parseus.codecinfo.ui.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.preference.*
import com.google.android.material.color.DynamicColors
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.parseus.codecinfo.R
import com.parseus.codecinfo.databinding.SettingsMainBinding
import com.parseus.codecinfo.ui.fragments.AboutFragment
import com.parseus.codecinfo.utils.getAttributeColor
import com.parseus.codecinfo.utils.getDefaultThemeOption
import com.parseus.codecinfo.utils.isBatterySaverDisallowed
import com.parseus.codecinfo.utils.sendFeedbackEmail

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: SettingsMainBinding

    private val useDynamicTheme: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("dynamic_theme", true)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_CodecInfo)
        DynamicColors.applyIfAvailable(this) { _, _ -> useDynamicTheme }
        if (Build.VERSION.SDK_INT >= 31) {
            window.statusBarColor = if (useDynamicTheme) {
                ContextCompat.getColor(this, android.R.color.system_accent1_700)
            } else {
                getAttributeColor(com.google.android.material.R.attr.colorPrimaryVariant)
            }
        }
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 21) {
            val enter = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
                excludeTarget(android.R.id.statusBarBackground, true)
                excludeTarget(android.R.id.navigationBarBackground, true)
            }
            val exit = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
                excludeTarget(android.R.id.statusBarBackground, true)
                excludeTarget(android.R.id.navigationBarBackground, true)
            }
            window.apply {
                requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
                enterTransition = enter
                exitTransition = exit
                allowReturnTransitionOverlap = true
            }
        }

        binding = SettingsMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        if (savedInstanceState == null) {
            supportFragmentManager.commit { replace(R.id.content, SettingsFragment::class.java, null) }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            supportActionBar!!.title = getString(R.string.action_settings)
            supportFragmentManager.popBackStack()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(ALIASES_CHANGED, aliasesChanged)
            putExtra(FILTER_TYPE_CHANGED, filterTypeChanged)
            putExtra(SORTING_CHANGED, sortingChanged)
            putExtra(IMMERSIVE_CHANGED, immersiveChanged)
            putExtra(DYNAMIC_THEME_CHANGED, dynamicThemeChanged)
        })
        super.finish()
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT == 29 && isTaskRoot && supportFragmentManager.backStackEntryCount == 0) {
            // Workaround for a memory leak from https://issuetracker.google.com/issues/139738913
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            findPreference<CheckBoxPreference>("dynamic_theme")?.apply {
                if (Build.VERSION.SDK_INT >= 31) {
                    setOnPreferenceChangeListener { _, _ ->
                        activity?.recreate()
                        dynamicThemeChanged = true
                        true
                    }
                } else {
                    isVisible = false
                }
            }

            findPreference<CheckBoxPreference>("immersive_mode")?.apply {
                if (Build.VERSION.SDK_INT >= 19) {
                    setOnPreferenceChangeListener { _, _ ->
                        immersiveChanged = true
                        true
                    }
                } else {
                    isVisible = false
                }
            }

            findPreference<CheckBoxPreference>("show_aliases")?.apply {
                if (Build.VERSION.SDK_INT >= 29) {
                    setOnPreferenceChangeListener { _, _ ->
                        aliasesChanged = true
                        true
                    }
                } else {
                    isVisible = false
                }
            }

            findPreference<ListPreference>("dark_theme")!!.apply {
                setDarkThemeOptions(this)
                val currentTheme = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getString("dark_theme", getDefaultThemeOption(requireContext()).toString())!!
                icon = AppCompatResources.getDrawable(requireContext(),
                        getCurrentThemeIcon(currentTheme.toInt()))
                setOnPreferenceChangeListener { pref, newValue ->
                    (newValue as String)
                    pref.icon = AppCompatResources.getDrawable(requireContext(),
                            getCurrentThemeIcon(newValue.toInt()))
                    AppCompatDelegate.setDefaultNightMode(DarkTheme.getAppCompatValue(newValue.toInt()))
                    true
                }
            }

            val filterType = findPreference<ListPreference>("filter_type")
            filterType!!.setOnPreferenceChangeListener { _, _ ->
                filterTypeChanged = true
                true
            }

            val sortingType = findPreference<ListPreference>("sort_type")
            sortingType!!.setOnPreferenceChangeListener { _, _ ->
                sortingChanged = true
                true
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preferences_screen)
        }

        @SuppressLint("InflateParams")
        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            return when (preference.key) {
                "feedback" -> {
                    sendFeedbackEmail()
                    true
                }

                "help" -> {
                    if (activity != null) {
                        parentFragmentManager.commit {
                            replace(R.id.content, AboutFragment())
                            addToBackStack(null)
                            (requireActivity() as AppCompatActivity).supportActionBar!!.apply {
                                title = getString(R.string.about_app)
                                setDisplayHomeAsUpEnabled(true)
                            }
                        }
                    }
                    true
                }

                else -> super.onPreferenceTreeClick(preference)
            }
        }

        private fun getCurrentThemeIcon(type: Int) = when (type) {
            DarkTheme.SystemDefault.value -> R.drawable.ic_app_theme_system
            DarkTheme.BatterySaver.value -> R.drawable.ic_app_theme_battery
            DarkTheme.Dark.value -> R.drawable.ic_app_theme_dark
            else -> R.drawable.ic_app_theme_light
        }

        private fun setDarkThemeOptions(listPreference: ListPreference) {
            val entries = mutableListOf<CharSequence>()
            val entryValues = mutableListOf<CharSequence>()

            // Light and Dark (always present)
            entries.add(getString(R.string.app_theme_light))
            entries.add(getString(R.string.app_theme_dark))
            entryValues.add(DarkTheme.Light.value.toString())
            entryValues.add(DarkTheme.Dark.value.toString())

            // Set by battery saver (if not blacklisted)
            if (!isBatterySaverDisallowed(requireContext())) {
                entries.add(getString(R.string.app_theme_battery_saver))
                entryValues.add(DarkTheme.BatterySaver.value.toString())
            }

            // System default (Android 9.0+)
            if (Build.VERSION.SDK_INT >= 28) {
                entries.add(getString(R.string.app_theme_system_default))
                entryValues.add(DarkTheme.SystemDefault.value.toString())
            }

            listPreference.apply {
                this.entries = entries.toTypedArray()
                this.entryValues = entryValues.toTypedArray()
                setDefaultValue(getDefaultThemeOption(requireContext()).toString())
                value = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getString("dark_theme", getDefaultThemeOption(requireContext()).toString())
            }
        }

    }

    companion object {
        var aliasesChanged = false
        var filterTypeChanged = false
        var sortingChanged = false
        var immersiveChanged = false
        var dynamicThemeChanged = false
        const val ALIASES_CHANGED = "aliases_changed"
        const val FILTER_TYPE_CHANGED = "filter_type_changed"
        const val SORTING_CHANGED = "sorting_changed"
        const val IMMERSIVE_CHANGED = "immersive_changed"
        const val DYNAMIC_THEME_CHANGED = "dynamic_theme_changed"
    }

}