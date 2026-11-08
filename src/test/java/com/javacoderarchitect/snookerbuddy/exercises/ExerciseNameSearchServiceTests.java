package com.javacoderarchitect.snookerbuddy.exercises;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.UsersAPI;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ExerciseNameSearchServiceTests {
    @Mock
    private ExerciseRepository repository;

    @Mock
    private UsersAPI users;

    private ExerciseService service;

    @BeforeEach
    void setUp() {
        service = new ExerciseService(repository, users);
    }

    @Test
    void normalizesNameSearchIndependentlyOfTheMachinesLocale() {
        var original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            var exercise = new ExerciseDto(9L, "Straight pot", "Setup", "Pot", Difficulty.EASY, ExerciseType.TECHNIQUE);
            when(repository.findLibrary(1L, "straight", null, null)).thenReturn(List.of(exercise));
            assertThat(service.findLibrary(1L, "  STRAIGHT  ", null, null)).containsExactly(exercise);
            verify(repository).findLibrary(1L, "straight", null, null);
            verifyNoMoreInteractions(repository);
        } finally {
            Locale.setDefault(original);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void clearingTheQueryRemovesTheNameRestrictionAndKeepsOtherFilters(String query) {
        when(repository.findLibrary(1L, "", Difficulty.EASY, ExerciseType.TECHNIQUE))
                .thenReturn(List.of());
        assertThat(service.findLibrary(1L, query, Difficulty.EASY, ExerciseType.TECHNIQUE))
                .isEmpty();
        verify(repository).findLibrary(1L, "", Difficulty.EASY, ExerciseType.TECHNIQUE);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsNonCoachesBeforeSearchingAnyLibrary() {
        doThrow(new AccessDeniedException("Access denied")).when(users).requireRole(2L, Role.ROLE_COACH);
        assertThatThrownBy(() -> service.findLibrary(2L, "straight", null, null))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository);
    }
}
