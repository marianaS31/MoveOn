package pt.ipp.estg.moveon.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pt.ipp.estg.moveon.data.local.entities.AthleteAlert
import pt.ipp.estg.moveon.data.local.entities.RaceEntity
import pt.ipp.estg.moveon.data.repository.RaceRepository

class RaceViewModel(
    private val repository: RaceRepository
) : ViewModel() {

    // --- PROVAS (LIVEDATA) ---
    private val _races = MutableLiveData<List<RaceEntity>>(emptyList())
    val races: LiveData<List<RaceEntity>> = _races

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // --- ALERTAS (CROWDSOURCING) ---
    private val _alertsLiveData = MutableLiveData<List<AthleteAlert>>(emptyList())
    val alertsLiveData: LiveData<List<AthleteAlert>> = _alertsLiveData

    private val _statusMessage = MutableLiveData<String?>()
    val statusMessage: LiveData<String?> = _statusMessage

    private val _isSubscribed = MutableLiveData(false)
    val isSubscribed: LiveData<Boolean> = _isSubscribed

    init {
        loadRaces()
    }

    fun loadRaces() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getPublicRaces().collect { raceList ->
                    _races.postValue(raceList)
                    _isLoading.postValue(false)
                }
            } catch (e: Exception) {
                _isLoading.postValue(false)
            }
        }
    }

    fun createRace(
        race: RaceEntity,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.createRace(race)
                _isLoading.postValue(false)
                onSuccess()
            } catch (e: Exception) {
                _isLoading.postValue(false)
                _statusMessage.postValue("Erro ao criar prova: ${e.localizedMessage}")
            }
        }
    }

    // Sobrecarga para suportar chamadas com parâmetros soltos
    fun createRace(
        raceName: String,
        raceDescription: String,
        raceType: String,
        raceDate: Long,
        creatorEmail: String = "",
        startLatitude: Double? = null,
        startLongitude: Double? = null,
        isPublic: Boolean = true,
        onSuccess: () -> Unit = {}
    ) {
        createRace(
            RaceEntity(
                raceName = raceName,
                raceDescription = raceDescription,
                raceType = raceType,
                raceDate = raceDate,
                creatorEmail = creatorEmail,
                startLatitude = startLatitude,
                startLongitude = startLongitude,
                isPublic = isPublic
            ),

            onSuccess
        )
    }

    fun loadAlerts(raceId: String) {
        viewModelScope.launch {
            repository.getAlertsForRace(raceId).collect { alerts ->
                _alertsLiveData.postValue(alerts)
            }
        }
    }

    fun registerPassage(
        raceId: String,
        reporterId: String,
        athleteNumber: Int,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            try {
                val alert = AthleteAlert(
                    raceId = raceId,
                    reporterId = reporterId,
                    athleteNumber = athleteNumber,
                    alertType = "PASSAGE",
                    latitude = latitude,
                    longitude = longitude,
                    timestamp = System.currentTimeMillis()
                )
                repository.registerAthleteAlert(alert)
                _statusMessage.postValue("Passagem do Atleta #$athleteNumber registada!")
            } catch (e: Exception) {
                _statusMessage.postValue("Erro ao registar alerta: ${e.localizedMessage}")
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }


    fun checkSubscriptionStatus(raceId: String, userId: String) {
        viewModelScope.launch {
            try {
                val status = repository.isSubscribed(raceId, userId)
                _isSubscribed.postValue(status)
            } catch (e: Exception) {
                _isSubscribed.postValue(false)
            }
        }
    }

    fun toggleSubscription(raceId: String, userId: String) {
        viewModelScope.launch {
            val currentStatus = _isSubscribed.value ?: false
            try {
                if (currentStatus) {
                    repository.unsubscribeFromRace(raceId, userId)
                    _isSubscribed.postValue(false)
                    _statusMessage.postValue("Subscrição cancelada.")
                } else {
                    repository.subscribeToRace(raceId, userId)
                    _isSubscribed.postValue(true)
                    _statusMessage.postValue("Prova subscrita com sucesso!")
                }
            } catch (e: Exception) {
                _statusMessage.postValue("Erro ao atualizar subscrição: ${e.localizedMessage}")
            }
        }
    }
}