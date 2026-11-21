package com.consoleemulationtoggle

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ExecutionDataKeys
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware

/**
 * Reads and writes the "Emulate terminal in output console" option of a run configuration.
 *
 * The option is per run configuration: `PythonRunConfiguration.emulateTerminal()` and
 * `setEmulateTerminal(boolean)`. Those are called reflectively on the live instance because
 * the class moved into the `intellij.python.community.impl` content module in newer PyCharm
 * builds. That module doesn't exist in 2024.3, so it can't be declared as a dependency, and
 * calling through the instance's own class needs no classloader access to it at all.
 *
 * Any configuration without these methods (pytest, Django, shell scripts, ...) is unsupported.
 */
private object EmulateTerminalOption {
    fun isSupported(profile: RunProfile): Boolean = try {
        profile.javaClass.getMethod("emulateTerminal")
        profile.javaClass.getMethod("setEmulateTerminal", Boolean::class.javaPrimitiveType)
        true
    } catch (_: NoSuchMethodException) {
        false
    }

    fun get(profile: RunProfile): Boolean =
        profile.javaClass.getMethod("emulateTerminal").invoke(profile) as Boolean

    fun set(profile: RunProfile, value: Boolean) {
        profile.javaClass.getMethod("setEmulateTerminal", Boolean::class.javaPrimitiveType)
            .invoke(profile, value)
    }
}

/**
 * The run configuration a toggle acts on.
 *
 * [profiles] normally holds a single instance, but a Run tab's environment can hold on to an
 * older instance than its settings do. Rerun reads the former and saving reads the latter.
 */
internal class ToggleTarget(
    val profiles: List<RunProfile>,
    val settings: RunnerAndConfigurationSettings?,
) {
    val isSupported: Boolean get() = profiles.all(EmulateTerminalOption::isSupported)
    val displayName: String get() = settings?.name ?: profiles.first().name
}

/** Shared toggle behaviour. Subclasses decide which run configuration they act on. */
abstract class EmulateTerminalToggle : ToggleAction(), DumbAware {

    private fun supportedTarget(e: AnActionEvent): ToggleTarget? =
        findTarget(e)?.takeIf { it.isSupported }

    internal abstract fun findTarget(e: AnActionEvent): ToggleTarget?

    override fun isSelected(e: AnActionEvent): Boolean {
        val target = supportedTarget(e) ?: return false
        return EmulateTerminalOption.get(target.profiles.first())
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val target = supportedTarget(e) ?: return
        target.profiles.forEach { EmulateTerminalOption.set(it, state) }

        // Notifies listeners that the configuration changed. Guarded because re-adding one
        // that has since been deleted (or evicted, if temporary) would bring it back.
        val settings = target.settings ?: return
        val runManager = RunManager.getInstance(e.project ?: return)
        if (runManager.allSettings.any { it === settings }) {
            runManager.addConfiguration(settings)
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val target = supportedTarget(e)
        if (target != null) {
            val state = if (EmulateTerminalOption.get(target.profiles.first())) "on" else "off"
            e.presentation.description =
                "'${target.displayName}': $state. Takes effect the next time it runs."
        }
        updateAvailability(e, target != null)
    }

    protected abstract fun updateAvailability(e: AnActionEvent, available: Boolean)

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

/**
 * Main toolbar button: acts on the configuration selected in the run widget.
 *
 * Stays visible but disabled when that isn't a Python script configuration, so the toolbar
 * doesn't shift around as the selection changes.
 */
class ToggleEmulateTerminalForSelectedAction : EmulateTerminalToggle() {

    override fun findTarget(e: AnActionEvent): ToggleTarget? {
        val project = e.project ?: return null
        val settings = RunManager.getInstance(project).selectedConfiguration ?: return null
        return ToggleTarget(listOf(settings.configuration), settings)
    }

    override fun updateAvailability(e: AnActionEvent, available: Boolean) {
        e.presentation.isVisible = e.project != null
        e.presentation.isEnabled = available
        if (!available) {
            e.presentation.description = "Select a Python script run configuration to use this toggle."
        }
    }
}

/**
 * Run/Debug tool window button: acts on the configuration that tab was started from, whatever
 * is currently selected in the run widget. Hidden on tabs for other kinds of configuration.
 */
class ToggleEmulateTerminalInRunTabAction : EmulateTerminalToggle() {

    override fun findTarget(e: AnActionEvent): ToggleTarget? {
        val environment = e.getData(ExecutionDataKeys.EXECUTION_ENVIRONMENT) ?: return null
        val settings = environment.runnerAndConfigurationSettings
        val tabProfile = environment.runProfile
        val currentProfile = settings?.configuration
        val profiles =
            if (currentProfile == null || currentProfile === tabProfile) listOf(tabProfile)
            else listOf(currentProfile, tabProfile)
        return ToggleTarget(profiles, settings)
    }

    override fun updateAvailability(e: AnActionEvent, available: Boolean) {
        e.presentation.isEnabledAndVisible = available
    }
}
