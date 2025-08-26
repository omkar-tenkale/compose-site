package org.blrdroid

import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GenericError(
    val code: String? = null,
    val message: String
)

// -------- event-details --------
@Serializable
data class EventDetailsRequest(val eventId: Long)

@Serializable
data class EventDetailsData(
    val eventName: String,
    val registrationsOpen: Boolean,
    val description: String,
    val date: String,
    val title: String
)

@Serializable
data class EventDetailsResponse(
    val error: GenericError? = null,
    val data: EventDetailsData? = null
)

// -------- register-for-event --------
@Serializable
data class RegisterForEventRequest(val eventId: Long)

@Serializable
data class RegisterForEventData(
    @SerialName("registrationId") val registrationId: Long
)

@Serializable
data class RegisterForEventResponse(
    val error: GenericError? = null,
    val data: RegisterForEventData? = null
)

// -------- event-registration-details --------
@Serializable
data class EventRegistrationDetailsRequest(val eventId: Long)

@Serializable
data class EventRegistrationDetailsData(
    val status: String,                       //  | "CANCELLED"
    val meta: JsonElement? = null,            // as-is from DB
    val checked_in_at: String? = null         // ISO date or null
){
    fun isConfirmed() = status == "CONFIRMED"
    fun isCancelled() = status == "CANCELLED"
}

@Serializable
data class EventRegistrationDetailsResponse(
    val error: GenericError? = null,
    val data: EventRegistrationDetailsData? = null
)

// -------- cancel-event-registration --------
@Serializable
data class CancelEventRegistrationRequest(val eventId: Long)

@Serializable
data class CancelEventRegistrationData(val cancelled: Boolean = true)

@Serializable
data class CancelEventRegistrationResponse(
    val error: GenericError? = null,
    val data: CancelEventRegistrationData? = null
)

suspend fun eventDetails(
    eventId: Long,
): EventDetailsResponse {
    return supabase.functions.invoke("event-details", body = EventDetailsRequest(eventId))
        .body<EventDetailsResponse>()
}

suspend fun registerForEvent(
    eventId: Long,
): RegisterForEventResponse {
    return supabase.functions.invoke("register-for-event", body = RegisterForEventRequest(eventId))
        .body<RegisterForEventResponse>()
}

suspend fun eventRegistrationDetails(
    eventId: Long,
): EventRegistrationDetailsResponse {
    return supabase.functions.invoke(
        "event-registration-details",
        body = EventRegistrationDetailsRequest(eventId)
    )
        .body<EventRegistrationDetailsResponse>()
}

suspend fun cancelEventRegistration(
    eventId: Long,
): CancelEventRegistrationResponse {
    return supabase.functions.invoke(
        "cancel-event-registration",
        body = CancelEventRegistrationRequest(eventId)
    )
        .body<CancelEventRegistrationResponse>()
}