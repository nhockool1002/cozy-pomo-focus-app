package com.cozypomo.app.ui.forceupdate

import androidx.lifecycle.ViewModel
import com.cozypomo.app.data.network.AppVersionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ForceUpdateViewModel @Inject constructor(
    holder: ForceUpdateHolder,
) : ViewModel() {
    val config: AppVersionDto = holder.config
}
