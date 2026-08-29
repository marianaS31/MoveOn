package pt.ipp.estg.moveon.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import pt.ipp.estg.moveon.data.local.entities.RaceEntity
import pt.ipp.estg.moveon.data.repository.RaceRepository

class RaceViewModel : ViewModel() {

    private val repository = RaceRepository()

    var isLoading = false
        private set

    var errorMessage: String? = null
        private set

    fun createRace(
        raceName: String,
        raceDescription: String,
        raceType: String,
        raceDate: Long,
        startLatitude: Double?,
        startLongitude: Double?,
        isPublic: Boolean,
        onSuccess: () -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        viewModelScope.launch {

            isLoading = true
            errorMessage = null

            try {

                val race = RaceEntity(
                    raceName = raceName,
                    raceDescription = raceDescription,
                    raceType = raceType,
                    raceDate = raceDate,
                    creatorEmail = user.email ?: "",
                    startLatitude = startLatitude,
                    startLongitude = startLongitude,
                    isPublic = isPublic
                )

                repository.createRace(race)

                onSuccess()

            } catch (e: Exception) {

                errorMessage = e.message

            } finally {

                isLoading = false

            }
        }
    }
}