package com.bodyscope3d.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity data class SessionEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val createdAt: Long)
@Entity(foreignKeys = [ForeignKey(SessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE)], indices = [Index("sessionId")])
data class PainEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val sessionId: Long,
    val regionId: String, val intensity: Int, val nature: String,
    val onset: String, val duration: String, val symptoms: String,
)
@Entity data class FavoriteEntity(@PrimaryKey val regionId: String)

data class SessionWithEntries(@Embedded val session: SessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId") val entries: List<PainEntryEntity>)

@Dao interface AppDao {
    @Insert suspend fun insertSession(s: SessionEntity): Long
    @Insert suspend fun insertEntries(e: List<PainEntryEntity>)
    @Transaction @Query("SELECT * FROM SessionEntity ORDER BY createdAt DESC")
    fun sessions(): Flow<List<SessionWithEntries>>
    @Query("DELETE FROM SessionEntity WHERE id = :id") suspend fun deleteSession(id: Long)
    @Query("DELETE FROM SessionEntity") suspend fun clearSessions()
    @Query("SELECT regionId FROM FavoriteEntity") fun favorites(): Flow<List<String>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addFavorite(f: FavoriteEntity)
    @Query("DELETE FROM FavoriteEntity WHERE regionId = :id") suspend fun removeFavorite(id: String)
    @Query("DELETE FROM FavoriteEntity") suspend fun clearFavorites()
}

@Database(entities = [SessionEntity::class, PainEntryEntity::class, FavoriteEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao
    companion object {
        fun create(c: Context) = Room.databaseBuilder(c, AppDb::class.java, "bodyscope.db").build()
    }
}

/** واجهات تجريدية: يمكن لاحقًا إضافة مزامنة Firebase بتنفيذ آخر دون تغيير منطق التطبيق */
interface PainRepository {
    fun sessions(): Flow<List<SessionWithEntries>>
    suspend fun save(entries: List<PainEntryEntity>, now: Long = System.currentTimeMillis()): Long
    suspend fun delete(sessionId: Long)
    suspend fun clearAll()
}
interface FavoritesRepository {
    fun favorites(): Flow<List<String>>
    suspend fun set(regionId: String, fav: Boolean)
}

class LocalRepository(private val dao: AppDao) : PainRepository, FavoritesRepository {
    override fun sessions() = dao.sessions()
    override suspend fun save(entries: List<PainEntryEntity>, now: Long): Long {
        val id = dao.insertSession(SessionEntity(createdAt = now))
        dao.insertEntries(entries.map { it.copy(sessionId = id) })
        return id
    }
    override suspend fun delete(sessionId: Long) = dao.deleteSession(sessionId)
    override suspend fun clearAll() { dao.clearSessions(); dao.clearFavorites() }
    override fun favorites() = dao.favorites()
    override suspend fun set(regionId: String, fav: Boolean) =
        if (fav) dao.addFavorite(FavoriteEntity(regionId)) else dao.removeFavorite(regionId)
}
