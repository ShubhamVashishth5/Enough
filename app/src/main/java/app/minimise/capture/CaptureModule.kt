package app.minimise.capture

import app.minimise.capture.data.RoomNoteRepository
import app.minimise.capture.domain.NoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CaptureModule {
    @Binds
    abstract fun noteRepository(impl: RoomNoteRepository): NoteRepository
}
