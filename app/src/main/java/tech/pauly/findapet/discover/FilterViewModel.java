package tech.pauly.findapet.discover;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.OnLifecycleEvent;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableField;
import android.databinding.ObservableList;
import android.view.View;
import android.widget.ToggleButton;

import java.util.List;

import javax.inject.Inject;

import tech.pauly.findapet.data.BreedRepository;
import tech.pauly.findapet.data.FilterRepository;
import tech.pauly.findapet.data.models.Age;
import tech.pauly.findapet.data.models.AnimalSize;
import tech.pauly.findapet.data.models.BreedListResponse;
import tech.pauly.findapet.data.models.Filter;
import tech.pauly.findapet.data.models.Sex;
import tech.pauly.findapet.shared.BaseViewModel;
import tech.pauly.findapet.shared.datastore.FilterAnimalTypeUseCase;
import tech.pauly.findapet.shared.datastore.FilterUpdatedUseCase;
import tech.pauly.findapet.shared.datastore.TransientDataStore;
import tech.pauly.findapet.shared.events.ActivityEvent;
import tech.pauly.findapet.shared.events.ViewEventBus;

public class FilterViewModel extends BaseViewModel {

    public ObservableField<Sex> selectedSex = new ObservableField<>(Sex.MISSING);
    public ObservableField<Age> selectedAge = new ObservableField<>(Age.MISSING);
    public ObservableField<AnimalSize> selectedSize = new ObservableField<>(AnimalSize.MISSING);
    public ObservableList<String> breedList = new ObservableArrayList<>();

    private FilterRepository filterRepository;
    private BreedRepository breedRepository;
    private ViewEventBus eventBus;
    private TransientDataStore dataStore;

    @Inject
    FilterViewModel(FilterRepository filterRepository,
                    BreedRepository breedRepository,
                    ViewEventBus eventBus,
                    TransientDataStore dataStore) {
        this.filterRepository = filterRepository;
        this.breedRepository = breedRepository;
        this.eventBus = eventBus;
        this.dataStore = dataStore;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void loadCurrentFilter() {
        subscribeOnLifecycle(filterRepository.getCurrentFilter()
                                             .subscribe(this::populateScreenForFilter, Throwable::printStackTrace));
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void updateBreedList() {
        FilterAnimalTypeUseCase useCase = dataStore.get(FilterAnimalTypeUseCase.class);
        if (useCase != null) {
            subscribeOnLifecycle(breedRepository.getBreedList(useCase.getAnimalType())
                                                .subscribe(this::populateBreedList, Throwable::printStackTrace));
        }
    }

    public void clickBreedSearch(View v) {
        //TODO: https://www.pivotaltracker.com/story/show/157159549
    }

    public void checkSex(View view, Sex sex) {
        selectedSex.set(isViewChecked(view) ? sex : Sex.MISSING);
    }
    public void checkAge(View view, Age age) {
        selectedAge.set(isViewChecked(view) ? age : Age.MISSING);
    }

    public void checkSize(View view, AnimalSize size) {
        selectedSize.set(isViewChecked(view) ? size : AnimalSize.MISSING);
    }

    public void saveFilter(View v) {
        Filter filter = new Filter();
        filter.setSex(selectedSex.get());
        filter.setAge(selectedAge.get());
        filter.setSize(selectedSize.get());
        subscribeOnLifecycle(filterRepository.insertFilter(filter)
                                             .subscribe(this::finish, Throwable::printStackTrace));
    }

    private void finish() {
        dataStore.save(new FilterUpdatedUseCase());
        eventBus.send(ActivityEvent.build(this).finishActivity());
    }

    private void populateScreenForFilter(Filter filter) {
        selectedSex.set(filter.getSex());
        selectedAge.set(filter.getAge());
        selectedSize.set(filter.getSize());
    }

    private void populateBreedList(BreedListResponse response) {
        List<String> breedList = response.getBreedList();
        if (breedList != null) {
            this.breedList.addAll(breedList);
        }
    }

    private boolean isViewChecked(View view) {
        boolean checked = false;
        if (view instanceof ToggleButton) {
            ToggleButton button = (ToggleButton) view;
            checked = button.isChecked();
        }
        return checked;
    }
}
