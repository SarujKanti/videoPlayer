package com.skd.videoplayer.core.common

import android.content.IntentSender
import com.skd.videoplayer.domain.model.UserConsentRequest

/** Android's form of a consent request: a system dialog launched through an [IntentSender]. */
class AndroidConsentRequest(val intentSender: IntentSender) : UserConsentRequest
