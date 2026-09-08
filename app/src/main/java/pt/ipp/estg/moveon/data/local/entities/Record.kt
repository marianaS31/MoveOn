package pt.ipp.estg.moveon.data.local.entities

data class Record(
    val id: String = "",
    val raceId: String = "",
    val userId: String = "",
    val displayName: String = "Anónimo",
    val timeMillis: Long = 0L,
    val completedAt: Long = System.currentTimeMillis()
)