package pt.ipp.estg.moveon.data.repository

import com.google.firebase.firestore.FieldValue
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
import pt.ipp.estg.moveon.data.local.entities.Record

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

    // Conversão de String para Long para cumprir a assinatura do RaceDao
    fun getAlertsForRace(raceId: String): Flow<List<AthleteAlert>> {
        return raceDao.getAlertsForRace(raceId)
    }

    suspend fun registerAthleteAlert(alert: AthleteAlert) {
        val firestore = FirebaseFirestore.getInstance()
        val docRef = firestore.collection("races")
            .document(alert.raceId)
            .collection("alerts")
            .document()

        // Grava no Firestore (online)
        docRef.set(alert).await()

        // Grava no Room (cache local)
        raceDao.insertAlert(alert)
    }

    suspend fun subscribeToRace(raceId: String, userId: String) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("races")
            .document(raceId)
            .update("subscribers", FieldValue.arrayUnion(userId))
            .await()

        firestore.collection("users")
            .document(userId)
            .collection("subscribed_races")
            .document(raceId)
            .set(mapOf("subscribedAt" to System.currentTimeMillis()))
            .await()
    }

    suspend fun unsubscribeFromRace(raceId: String, userId: String) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("races")
            .document(raceId)
            .update("subscribers", FieldValue.arrayRemove(userId))
            .await()

        firestore.collection("users")
            .document(userId)
            .collection("subscribed_races")
            .document(raceId)
            .delete()
            .await()
    }

    suspend fun isSubscribed(raceId: String, userId: String): Boolean {
        val firestore = FirebaseFirestore.getInstance()
        val doc = firestore.collection("users")
            .document(userId)
            .collection("subscribed_races")
            .document(raceId)
            .get()
            .await()
        return doc.exists()
    }

    suspend fun saveAmateurTime(record: Record) {
        val firestore = FirebaseFirestore.getInstance()
        val docRef = firestore.collection("races")
            .document(record.raceId)
            .collection("leaderboard")
            .document()

        val recordWithId = record.copy(id = docRef.id)
        docRef.set(recordWithId).await()
    }

    fun getLeaderboard(raceId: String): Flow<List<Record>> = callbackFlow {
        val firestore = FirebaseFirestore.getInstance()
        val subscription = firestore.collection("races")
            .document(raceId)
            .collection("leaderboard")
            .orderBy("timeMillis", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val records = snapshot.documents.mapNotNull { it.toObject(Record::class.java) }
                    trySend(records)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveAlert(alert: AthleteAlert) {
        // 1. Grava no Room local
        raceDao.insertAlert(alert)

        // 2. Grava no Firestore remoto
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("races")
            .document(alert.raceId)
            .collection("alerts")
            .add(alert)
            .await()
    }
}