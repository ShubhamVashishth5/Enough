package app.minimise.capture.domain

import java.time.Instant
import kotlinx.coroutines.flow.Flow

data class Note(val id: Long, val text: String, val createdAt: Instant, val done: Boolean)

interface NoteRepository {
    /** Open notes first. */
    val notes: Flow<List<Note>>

    /** Notes not yet ticked off. */
    val openCount: Flow<Int>

    /** Blank text is ignored. */
    suspend fun add(text: String)

    suspend fun setDone(id: Long, done: Boolean)

    suspend fun delete(id: Long)
}
