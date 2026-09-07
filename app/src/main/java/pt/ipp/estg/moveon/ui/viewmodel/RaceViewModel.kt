package pt.ipp.estg.moveon.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pt.ipp.estg.moveon.data.local.entities.AthleteAlert
import pt.ipp.estg.moveon.data.repository.RaceRepository

class RaceViewModel(
    private val repository: RaceRepository
) : ViewModel() {

    // --- LiveData para Alertas da Prova ---
    private val _alertsLiveData = MutableLiveData<List<AthleteAlert>>(emptyList())
    val alertsLiveData: LiveData<List<AthleteAlert>> = _alertsLiveData

    // --- LiveData de Feedback / Mensagens ---
    private val _statusMessage = MutableLiveData<String?>()
    val statusMessage: LiveData<String?> = _statusMessage

    // Carregar e observar alertas em tempo real convertendo para LiveData
    fun loadAlerts(raceId: String) {
        viewModelScope.launch {
            repository.getAlertsForRace(raceId).collect { alerts ->
                _alertsLiveData.postValue(alerts)
            }
        }
    }

    // Registar passagem com LiveData de retorno
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
}