package uk.gov.moj.cpp.workmanagement.proxy.healthchecks;

import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class NonFunctionalEventStoreDataSourceProviderTest {


    @InjectMocks
    private NonFunctionalEventStoreDataSourceProvider nonFunctionalEventStoreDataSourceProvider;

    @Test
    public void shouldFailIfGetDataSourceIsCalled() throws Exception {

        final UnsupportedOperationException unsupportedOperationException = assertThrows(UnsupportedOperationException.class,
                () -> nonFunctionalEventStoreDataSourceProvider.getDataSource("some jndi name"));

        assertThat(unsupportedOperationException.getMessage(), is("Do not use. Class only exists to keep CDI happy"));
    }

    @Test
    public void shouldFailIfGetDefaultDataSourceIsCalled() throws Exception {

        final UnsupportedOperationException unsupportedOperationException = assertThrows(UnsupportedOperationException.class,
                () -> nonFunctionalEventStoreDataSourceProvider.getDefaultDataSource());

        assertThat(unsupportedOperationException.getMessage(), is("Do not use. Class only exists to keep CDI happy"));
    }
}