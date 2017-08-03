package org.blokada.ui.app.android

import org.blokada.app.android.APrefsPersistence
import org.blokada.framework.KContext
import org.blokada.framework.newPersistedProperty
import org.blokada.ui.app.Info
import org.blokada.ui.app.UiState
import android.content.Context
import org.blokada.framework.newProperty

class AUiState(
        private val ctx: Context,
        private val kctx: KContext
) : UiState() {

    override val seenWelcome = newPersistedProperty(kctx, APrefsPersistence(ctx, "seenWelcome"),
            { false }
    )

    override val notifications = newPersistedProperty(kctx, APrefsPersistence(ctx, "notifications"),
            { true }
    )

    override val editUi = newProperty(kctx, { false })

    override val dashes = newPersistedProperty(kctx, ADashesPersistence(ctx), { listOf(
            UpdateDash(ctx).activate(false),
            StatusDash(ctx).activate(false),
            TunnelDashAdsBlocked(ctx).activate(true),
            DataSavedDash(ctx).activate(true),
            DashFilterBlacklist(ctx).activate(false),
            DashFilterWhitelist(ctx).activate(false),
            NotificationDashOn(ctx).activate(true),
            NotificationDashKeepAlive(ctx).activate(false),
            AutoStartDash(ctx).activate(true),
            ConnectivityDash(ctx).activate(true),
            TunnelDashHostsCount(ctx).activate(false),
            TunnelDashEngineSelected(ctx).activate(false),
            DonateDash(ctx).activate(true),
            FaqDash(ctx).activate(false).activate(true),
            BugReportDash(ctx).activate(false),
            FeedbackDash(ctx).activate(false),
            BlogDash(ctx).activate(false),
            AboutDash(ctx).activate(false)
    ) })

    override val infoQueue = newProperty(kctx, { listOf<Info>() })

    override val lastSeenUpdateMillis = newPersistedProperty(kctx, APrefsPersistence(ctx, "lastSeenUpdate"),
            { 0L }
    )

}

