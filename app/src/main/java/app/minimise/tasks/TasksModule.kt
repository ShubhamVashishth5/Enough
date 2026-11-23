package app.minimise.tasks

import app.minimise.tasks.data.RoomTaskRepository
import app.minimise.tasks.domain.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TasksModule {
    @Binds
    abstract fun taskRepository(impl: RoomTaskRepository): TaskRepository
}
