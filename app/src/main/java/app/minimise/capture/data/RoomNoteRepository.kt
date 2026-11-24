package app.minimise.capture.data

import app.minimise.capture.domain.Note
import app.minimise.capture.domain.NoteRepository
import app.minimise.core.db.NoteDao
import app.minimise.core.db.NoteEntity
import app.minimise.core.time.TimeSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNoteRepository @Inject constructor(
    private val dao: NoteDao,
    private val timeSource: TimeSource,
) : NoteRepository {
    override val notes: Flow<List<Note>> = dao.observeAll().map { rows -> rows.map { Note(it.id, it.text, it.createdAt, it.done) } }

    override val openCount: Flow<Int> = dao.observeOpenCount()

    override suspend fun add(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) dao.insert(NoteEntity(text = trimmed, createdAt = timeSource.now()))
    }

    override suspend fun setDone(id: Long, done: Boolean) = dao.setDone(id, done)

    override suspend fun delete(id: Long) = dao.delete(id)
}
