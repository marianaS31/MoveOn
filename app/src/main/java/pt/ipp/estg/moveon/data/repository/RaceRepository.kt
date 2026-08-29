package pt.ipp.estg.moveon.data.repository

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import pt.ipp.estg.moveon.data.local.entities.RaceEntity

class RaceRepository {

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
}