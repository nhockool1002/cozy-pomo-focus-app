package com.cozypomo.app.data.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bus tối giản báo "dữ liệu sở hữu (loài/trứng/kho đồ) vừa đổi" — dùng để các ViewModel không
 * cùng phạm vi tự tải lại khi có thay đổi đến từ nơi khác mà không cần tham chiếu thẳng nhau:
 * [ForestViewModel] (cấp loài/trứng qua cheat bubble ở [TesterCheatViewModel], hoặc hoàn thành
 * phiên thật ở [com.cozypomo.app.data.timer.TimerRepository]), `InventoryViewModel` (trang bị
 * bình/nhạc từ chính nó cũng tự thông báo lại cho mình qua bus này để nhất quán 1 cơ chế duy nhất).
 */
@Singleton
class CollectionEventBus @Inject constructor() {
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    fun notifyChanged() {
        _changes.tryEmit(Unit)
    }
}
