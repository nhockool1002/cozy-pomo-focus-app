package com.cozypomo.app.data.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bus tối giản báo "bộ sưu tập loài / trứng sở hữu vừa đổi" — dùng để [ForestViewModel] tự tải
 * lại khi có thay đổi đến từ nơi khác (VD bubble cheat cấp loài/trứng ở [TesterCheatViewModel]).
 * 2 ViewModel này không cùng phạm vi (cheat sống ở NavHost, Forest sống theo từng lần vào tab) nên
 * không thể tham chiếu thẳng nhau — dùng 1 singleton dùng chung qua Hilt thay vì ràng buộc trực tiếp.
 */
@Singleton
class CollectionEventBus @Inject constructor() {
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    fun notifyChanged() {
        _changes.tryEmit(Unit)
    }
}
