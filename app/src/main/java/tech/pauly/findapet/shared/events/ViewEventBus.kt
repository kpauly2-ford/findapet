package tech.pauly.findapet.shared.events

import android.support.annotation.IdRes
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import tech.pauly.findapet.shared.BaseFragment
import tech.pauly.findapet.shared.PermissionRequestResponse
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A Singleton class to manage events that the Presentation layer needs the View layer to handle.
 *
 * This is workaround to get strong MVVM where the Presentation objects (View Models) don't have
 * references to their View counterparts (Activities/Fragments). This is made difficult by the
 * platform sometimes necessitating the View objects to do more than they should in traditional
 * MVVM.
 *
 * Using this pattern, the ViewModels can create Event objects with all of the business logic inside
 * and simply enter the Event into the bus. Then the corresponding View object can observe the bus
 * and respond to that object, usually just sending the Event to a function in the BaseActivity/BaseFragment
 * (see [BaseFragment.activityEvent]) which then performs standard logic on the
 * event. Note one field must exist in all events: the emitter class. This is to provide the functionality
 * for the View to act only on certain objects' events, or act differently on different objects' events.
 */
@Singleton
open class ViewEventBus @Inject constructor() {

    private val bus = PublishSubject.create<BaseViewEvent>()

    open fun send(event: BaseViewEvent) {
        bus.onNext(event)
    }

    open fun activity(emitterClass: Class<*>): Observable<ActivityEvent> {
        return bus.filter { event -> event is ActivityEvent && event.fromEmitter(emitterClass) }
                .map { event -> event as ActivityEvent }
    }

    open fun fragment(emitterClass: Class<*>): Observable<FragmentEvent> {
        return bus.filter { event -> event is FragmentEvent && event.fromEmitter(emitterClass) }
                .map { event -> event as FragmentEvent }
    }

    open fun permission(emitterClass: Class<*>): Observable<PermissionEvent> {
        return bus.filter { event -> event is PermissionEvent && event.fromEmitter(emitterClass) }
                .map { event -> event as PermissionEvent }
    }
}

open class BaseViewEvent(private val emitter: Class<*>) {
    fun fromEmitter(clazz: Class<*>): Boolean {
        return this.emitter.name == clazz.name
    }
}