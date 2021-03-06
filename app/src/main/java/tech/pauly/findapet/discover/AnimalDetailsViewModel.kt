package tech.pauly.findapet.discover

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Intent
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import android.databinding.ObservableInt
import android.net.Uri
import android.view.View
import com.google.android.gms.maps.model.LatLng
import io.reactivex.subjects.PublishSubject
import tech.pauly.findapet.R
import tech.pauly.findapet.data.FavoriteRepository
import tech.pauly.findapet.data.ShelterRepository
import tech.pauly.findapet.data.models.Animal
import tech.pauly.findapet.data.models.Option
import tech.pauly.findapet.data.models.AnimalUrl
import tech.pauly.findapet.data.models.Shelter
import tech.pauly.findapet.shared.BaseViewModel
import tech.pauly.findapet.shared.LocationHelper
import tech.pauly.findapet.shared.ResourceProvider
import tech.pauly.findapet.shared.datastore.AnimalDetailsUseCase
import tech.pauly.findapet.shared.datastore.TransientDataStore
import tech.pauly.findapet.shared.events.*
import tech.pauly.findapet.utils.ObservableString
import javax.inject.Inject

class AnimalDetailsViewModel @Inject
internal constructor(private val dataStore: TransientDataStore,
                     private val resourceProvider: ResourceProvider,
                     private val favoriteRepository: FavoriteRepository,
                     private val eventBus: ViewEventBus,
                     private val shelterRepository: ShelterRepository,
                     private val locationHelper: LocationHelper) : BaseViewModel() {

    var name = ObservableString()
    var age = ObservableInt(R.string.missing)
    var breeds = ObservableString()
    var sex = ObservableInt(R.string.missing)
    var size = ObservableInt(R.string.missing)
    var options = ObservableString()
    var description = ObservableString()
    var descriptionVisibility = ObservableBoolean(false)
    var optionsVisibility = ObservableBoolean(false)
    var imagesPageLimit = ObservableInt(4)
    var imagesCount = ObservableInt(0)
    var currentImagePosition = ObservableInt(0)
    var contactName = ObservableString()
    var contactAddress = ObservableString()
    var contactPhone = ObservableString()
    var contactEmail = ObservableString()
    var contactDistance = ObservableString()
    var contactPhoneVisibility = ObservableBoolean(false)
    var contactEmailVisibility = ObservableBoolean(false)
    var partialContact = ObservableBoolean(false)

    var animalImagesSubject = PublishSubject.create<List<AnimalImageViewModel>>()
    var tabSwitchSubject = PublishSubject.create<Int>()

    private var animal: Animal? = null
    private var latLng: LatLng? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun setupPage() {
        dataStore[AnimalDetailsUseCase::class]?.let {
            val animal = it.animal
            name.set(animal.name)
            sex.set(animal.sex.formattedName)
            size.set(animal.size.formattedName)
            age.set(animal.age.formattedName)
            breeds.set(animal.formattedBreedList)

            setPhotos(animal.photoUrlList)
            setOptions(animal.options)
            setDescription(animal.description)
            updateShelter(animal)
            this.animal = animal

            if (it.tab == AnimalDetailsUseCase.Tab.CONTACT) {
                tabSwitchSubject.onNext(1)
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun checkFavorite() {
        animal?.let {
            favoriteRepository.isAnimalFavorited(it.id)
                    .quickSubscribe(this::showFavoriteInOptions)
        }
    }

    fun imagePageChange(position: Int) {
        currentImagePosition.set(position)
    }

    @Suppress("UNUSED_PARAMETER")
    fun clickAddress(v: View) {
        val uriString = "geo:" + if (latLng != null) {
            "${latLng?.latitude},${latLng?.longitude}"
        } else {
            "0,0"
        } + "?q=${Uri.encode(contactAddress.get())}"

        eventBus += ActivityEvent(this,
                customIntent = Intent(Intent.ACTION_VIEW,
                        Uri.parse(uriString)))
    }

    @Suppress("UNUSED_PARAMETER")
    fun clickPhone(v: View) {
        eventBus += ActivityEvent(this,
                customIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${contactPhone.get()}")
                })
    }

    @Suppress("UNUSED_PARAMETER")
    fun clickEmail(v: View) {
        eventBus += ActivityEvent(this,
                customIntent = Intent(Intent.ACTION_SENDTO).apply {
                    type = "text/plain"
                    data = Uri.parse("mailto:${contactEmail.get()}")
                    flags += Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Intent.EXTRA_SUBJECT, "Interested in adoption - ${animal?.name}")
                })
    }

    private fun showFavoriteInOptions(favorited: Boolean) {
        eventBus += OptionsMenuEvent(this, if (favorited) OptionsMenuState.FAVORITE else OptionsMenuState.NOT_FAVORITE)
    }

    private fun showFavoriteSnackbar(favorited: Boolean) {
        eventBus += SnackbarEvent(this, if (favorited) R.string.favorite_snackbar_message else R.string.unfavorite_snackbar_message)
    }

    internal fun changeFavorite(favorite: Boolean) {
        animal?.let {
            showFavoriteInOptions(favorite)
            val update = if (favorite) {
                favoriteRepository.favoriteAnimal(it)
            } else {
                favoriteRepository.unfavoriteAnimal(it)
            }
            update.quickSubscribe { showFavoriteSnackbar(favorite) }
        }
    }

    private fun setDescription(description: String?) {
        if (!description.isNullOrBlank()) {
            descriptionVisibility.set(true)
            this.description.set(description)
        }
    }

    private fun setOptions(options: List<Option>) {
        if (options.isNotEmpty()) {
            optionsVisibility.set(true)
            this.options.set(options.joinToString("\n") {
                resourceProvider.getString(it.formattedName)
            })
        }
    }

    private fun setPhotos(photoList: List<AnimalUrl>?) {
        photoList?.map {
            AnimalImageViewModel(it)
        }?.also {
            imagesCount.set(it.size)
            animalImagesSubject.onNext(it)
        }
    }

    private fun updateShelter(animal: Animal) {
        shelterRepository.fetchShelter(animal)
                .flatMap {
                    setContactFields(it)
                    locationHelper.getCurrentDistanceToContactInfo(it)
                }.quickSubscribe(this::setDistanceText)
    }

    private fun setContactFields(it: Shelter) {
        partialContact.set(it.name == null)
        contactName.set(it.name)
        contactAddress.set(it.formattedAddress)
        contactPhoneVisibility.set(it.phone != null)
        contactPhone.set(it.phone)
        contactEmailVisibility.set(it.email != null)
        contactEmail.set(it.email)
        if (it.latitude != null && it.longitude != null) {
            latLng = LatLng(it.latitude!!, it.longitude!!)
        }
    }

    private fun setDistanceText(it: Int) {
        contactDistance.set(when (it) {
            -1 -> resourceProvider.getString(R.string.empty_string)
            0 -> resourceProvider.getString(R.string.distance_less_than_one)
            else -> resourceProvider.getQuantityString(R.plurals.distance, it)
        })
    }
}
