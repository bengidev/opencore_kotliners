package io.github.bengidev.opencore.shared.persistence.room

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update

@Entity(tableName = "atoms")
internal data class AtomEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "created_at") val createdAt: Double,
    @ColumnInfo(name = "updated_at") val updatedAt: Double,
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean,
    @ColumnInfo(name = "group_name") val groupName: String?,
    @ColumnInfo(name = "leaf_entry_id") val leafEntryId: String?,
)

@Entity(
    tableName = "session_entries",
    foreignKeys = [
        ForeignKey(
            entity = AtomEntity::class,
            parentColumns = ["id"],
            childColumns = ["atom_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("atom_id"),
        Index("parent_id"),
    ],
)
internal data class SessionEntryEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "atom_id") val atomId: String,
    @ColumnInfo(name = "parent_id") val parentId: String?,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "payload") val payload: ByteArray,
    @ColumnInfo(name = "timestamp") val timestamp: Double,
    @ColumnInfo(name = "sort_index") val sortIndex: Int,
)

@Entity(tableName = "app_metadata")
internal data class AppMetadataEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String,
)

@Dao
internal interface AtomDao {
    @Query("SELECT * FROM atoms")
    suspend fun listAtoms(): List<AtomEntity>

    @Query("SELECT * FROM atoms WHERE id = :atomId LIMIT 1")
    suspend fun findAtom(atomId: String): AtomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAtom(atom: AtomEntity)

    @Query("DELETE FROM atoms WHERE id = :atomId")
    suspend fun deleteAtom(atomId: String)

    @Update
    suspend fun updateAtom(atom: AtomEntity)

    @Query(
        """
        SELECT * FROM session_entries
        WHERE atom_id = :atomId
        ORDER BY sort_index ASC
        """,
    )
    suspend fun listSessionEntries(atomId: String): List<SessionEntryEntity>

    @Query("SELECT * FROM session_entries WHERE id = :entryId LIMIT 1")
    suspend fun findSessionEntry(entryId: String): SessionEntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSessionEntry(entry: SessionEntryEntity)

    @Update
    suspend fun updateSessionEntry(entry: SessionEntryEntity)

    @Query(
        """
        SELECT COALESCE(MAX(sort_index), -1) FROM session_entries WHERE atom_id = :atomId
        """,
    )
    suspend fun maxSortIndex(atomId: String): Int

    @Query("SELECT value FROM app_metadata WHERE key = :key LIMIT 1")
    suspend fun metadataValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMetadata(entity: AppMetadataEntity)

    @Transaction
    suspend fun updateAtomLeaf(atomId: String, leafEntryId: String, updatedAt: Double) {
        val atom = findAtom(atomId) ?: return
        updateAtom(
            atom.copy(
                leafEntryId = leafEntryId,
                updatedAt = updatedAt,
            ),
        )
    }
}

@Database(
    entities = [AtomEntity::class, SessionEntryEntity::class, AppMetadataEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class AtomDatabase : RoomDatabase() {
    abstract fun atomDao(): AtomDao
}
