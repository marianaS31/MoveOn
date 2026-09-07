package pt.ipp.estg.moveon.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pt.ipp.estg.moveon.data.local.dao.RaceDao
import pt.ipp.estg.moveon.data.local.entities.AthleteAlert
import pt.ipp.estg.moveon.data.local.entities.RaceEntity

class RaceRepository(
    private val raceDao: RaceDao
) {

    private val db = Firebase.firestore
    private val racesCollection = db.collection("races")

    suspend fun createRace(race: RaceEntity): String {
        val raceData = hashMapOf(
            "name" to race.raceName,
            "description" to race.raceDescription,
            "type" to race.raceType,
            "date" to race.raceDate,
            "creatorEmail" to race.creatorEmail,
            "startLatitude" to race.startLatitude,
            "startLongitude" to race.startLongitude,
            "isPublic" to race.isPublic
        )

        val document = racesCollection.add(raceData).await()
        return document.id
    }

    fun getPublicRaces(): Flow<List<RaceEntity>> = callbackFlow {
        val listener: ListenerRegistration =
            racesCollection
                .whereEqualTo("isPublic", true)
                .orderBy("date")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val races = snapshot?.documents?.mapNotNull { document ->
                        try {
                            RaceEntity(
                                raceId = 0,
                                raceName = document.getString("name") ?: "",
                                raceDescription = document.getString("description") ?: "",
                                raceType = document.getString("type") ?: "",
                                raceDate = document.getLong("date") ?: 0L,
                                creatorEmail = document.getString("creatorEmail") ?: "",
                                startLatitude = document.getDouble("startLatitude"),
                                startLongitude = document.getDouble("startLongitude"),
                                isPublic = document.getBoolean("isPublic") ?: true,
                                firebaseId = document.id
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()

                    trySend(races)
                }

        awaitClose {
            listener.remove()
        }
    }

    fun getAlertsForRace(raceId: String) = raceDao.getAlertsForRace(raceId)

    suspend fun registerAthleteAlert(alert: AthleteAlert) {
        val firestore = FirebaseFirestore.getInstance()
        val docRef = firestore.collection("races")
            .document(alert.raceId)
            .collection("alerts")
            .document()

        val alertWithId = alert.copy(id = docRef.id)

        // Grava no Firestore (online)
        docRef.set(alertWithId).await()

        // Grava no Room (cache local)
        raceDao.insertAlert(alertWithId)
    }
}