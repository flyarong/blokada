package org.blokada.app

import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.instance

/**
 * Translates internal MainState changes into higher level events used by topbar and fab.
 */
class EnabledStateActor(
        val di: LazyKodein,
        val listeners: MutableList<IEnabledStateActorListener> = mutableListOf()
) {

    // Refs to ensure listeners live only as long as this class
    private val listener1: Any
    private val listener2: Any
    private val listener3: Any

    init {
        val s: State = di().instance()

        listener1 = s.enabled.doOnUiWhenChanged().then { update(s) }
        listener2 = s.active.doOnUiWhenChanged().then { update(s) }
        listener3 = s.tunnelState.doOnUiWhenChanged().then { update(s) }
        update(s)
    }

    fun update(s: State) {
        when {
            s.tunnelState(TunnelState.ACTIVATING) -> startActivating()
            s.tunnelState(TunnelState.DEACTIVATING) -> startDeactivating()
            s.tunnelState(TunnelState.ACTIVE) -> finishActivating()
            s.active() -> startActivating()
            else -> finishDeactivating()
        }
    }

    private fun startActivating() {
        listeners.forEach { it.startActivating() }
    }

    private fun finishActivating() {
        listeners.forEach { it.finishActivating() }
    }

    private fun startDeactivating() {
        listeners.forEach { it.startDeactivating() }
    }

    private fun finishDeactivating() {
        listeners.forEach { it.finishDeactivating() }
    }
}

interface IEnabledStateActorListener {
    fun startActivating()
    fun finishActivating()
    fun startDeactivating()
    fun finishDeactivating()
}
