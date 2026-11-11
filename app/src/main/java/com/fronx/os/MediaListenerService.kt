package com.fronx.os

import android.service.notification.NotificationListenerService

/**
 * Intentionally empty. Declaring a notification listener (and the user enabling it in
 * system settings) is what lets Android give FRONX OS access to active media sessions.
 * FRONX OS does not read notification content.
 */
class MediaListenerService : NotificationListenerService()
