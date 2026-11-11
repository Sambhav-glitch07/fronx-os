package com.fronx.os

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings

/** Follows the currently active Android media session (Spotify, YouTube Music, etc.). */
class MediaBridge(private val ctx: Context) {
    fun interface Listener { fun onMediaChanged() }

    private val listeners = LinkedHashSet<Listener>()
    private val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val component = ComponentName(ctx, MediaListenerService::class.java)
    private var registered = false

    var controller: MediaController? = null
        private set

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list -> pick(list) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = notifyChanged()
        override fun onPlaybackStateChanged(state: PlaybackState?) = notifyChanged()
        override fun onSessionDestroyed() { pick(safeSessions()) }
    }

    fun add(l: Listener) { listeners.add(l) }
    fun remove(l: Listener) { listeners.remove(l) }
    private fun notifyChanged() { for (l in ArrayList(listeners)) l.onMediaChanged() }

    fun hasAccess(): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(":").any { ComponentName.unflattenFromString(it)?.packageName == ctx.packageName }
    }

    fun start() {
        stop()
        if (!hasAccess()) { notifyChanged(); return }
        try {
            msm.addOnActiveSessionsChangedListener(sessionsListener, component)
            registered = true
            pick(safeSessions())
        } catch (e: SecurityException) {
            notifyChanged()
        }
    }

    fun stop() {
        if (registered) {
            try { msm.removeOnActiveSessionsChangedListener(sessionsListener) } catch (e: Exception) { }
            registered = false
        }
        setController(null)
    }

    private fun safeSessions(): List<MediaController> =
        try { msm.getActiveSessions(component) } catch (e: SecurityException) { emptyList() }

    private fun pick(list: List<MediaController>?) {
        val best = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list?.firstOrNull()
        setController(best)
    }

    private fun setController(c: MediaController?) {
        controller?.unregisterCallback(callback)
        controller = c
        c?.registerCallback(callback)
        notifyChanged()
    }

    fun title(): String? {
        val m = controller?.metadata ?: return null
        return m.getString(MediaMetadata.METADATA_KEY_TITLE) ?: m.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
    }

    fun artist(): String? {
        val m = controller?.metadata ?: return null
        return m.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: m.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
    }

    fun art(): Bitmap? {
        val m = controller?.metadata ?: return null
        return m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
    }

    fun isPlaying(): Boolean = controller?.playbackState?.state == PlaybackState.STATE_PLAYING

    fun durationMs(): Long = controller?.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

    fun positionMs(): Long {
        val s = controller?.playbackState ?: return 0L
        if (s.state == PlaybackState.STATE_PLAYING) {
            val delta = SystemClock.elapsedRealtime() - s.lastPositionUpdateTime
            return s.position + (delta * s.playbackSpeed).toLong()
        }
        return s.position
    }

    fun toggle() { val tc = controller?.transportControls ?: return; if (isPlaying()) tc.pause() else tc.play() }
    fun next() { controller?.transportControls?.skipToNext() }
    fun previous() { controller?.transportControls?.skipToPrevious() }
}
