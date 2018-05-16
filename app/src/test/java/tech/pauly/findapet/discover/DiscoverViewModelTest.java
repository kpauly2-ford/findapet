package tech.pauly.findapet.discover;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import tech.pauly.findapet.data.AnimalRepository;
import tech.pauly.findapet.data.models.Animal;
import tech.pauly.findapet.data.models.AnimalListResponse;
import tech.pauly.findapet.data.models.AnimalType;
import tech.pauly.findapet.shared.datastore.DiscoverToolbarTitleUseCase;
import tech.pauly.findapet.shared.datastore.TransientDataStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DiscoverViewModelTest {

    @Mock
    private AnimalListAdapter listAdapter;

    @Mock
    private AnimalListItemViewModel.Factory animalListItemFactory;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private TransientDataStore dataStore;

    @Mock
    private AnimalListResponse animalListResponse;

    private DiscoverViewModel subject;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(animalRepository.fetchAnimals(any(AnimalType.class), anyInt())).thenReturn(Observable.just(animalListResponse));
        subject = new DiscoverViewModel(listAdapter, animalListItemFactory, animalRepository, dataStore);
    }

    @Test
    public void reloadWithNewAnimalType_setsAnimalTypeAndResetsOffsetAndFetchesAnimals() {
        Animal animal = mock(Animal.class);
        when(animalListResponse.getLastOffset()).thenReturn(10);
        when(animalListResponse.getAnimalList()).thenReturn(Collections.singletonList(animal));
        subject.reloadWithNewAnimalType(AnimalType.CAT);
        verify(animalRepository).fetchAnimals(AnimalType.CAT, 0);
        clearInvocations(animalRepository);

        subject.reloadWithNewAnimalType(AnimalType.DOG);

        verify(dataStore).save(new DiscoverToolbarTitleUseCase(AnimalType.DOG.getToolbarName()));
        verify(animalRepository).fetchAnimals(AnimalType.DOG, 0);
    }

    @Test
    public void reloadList_fetchAnimalsOnNext_sendAnimalListToAdapter() {
        Animal animal = mock(Animal.class);
        when(animalListResponse.getAnimalList()).thenReturn(Collections.singletonList(animal));
        ArgumentCaptor<List<AnimalListItemViewModel>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        subject.reloadList();

        verify(listAdapter).setAnimalItems(argumentCaptor.capture());
        verify(animalListItemFactory).newInstance(animal);
    }

    @Test
    public void reloadList_fetchAnimalsOnNextAndAnimalListNull_setsEmptyList() {
        when(animalListResponse.getAnimalList()).thenReturn(null);
        ArgumentCaptor<List<AnimalListItemViewModel>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        subject.reloadList();

        verify(listAdapter).setAnimalItems(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().size()).isEqualTo(0);
    }

    @Test
    public void loadMoreAnimals_fetchAnimalsAtCurrentOffset() {
        Animal animal = mock(Animal.class);
        when(animalListResponse.getLastOffset()).thenReturn(10);
        when(animalListResponse.getAnimalList()).thenReturn(Collections.singletonList(animal));
        subject.reloadList();
        verify(animalRepository).fetchAnimals(AnimalType.CAT, 0);
        clearInvocations(animalRepository);

        subject.loadMoreAnimals();

        verify(animalRepository).fetchAnimals(AnimalType.CAT, 10);
    }
}