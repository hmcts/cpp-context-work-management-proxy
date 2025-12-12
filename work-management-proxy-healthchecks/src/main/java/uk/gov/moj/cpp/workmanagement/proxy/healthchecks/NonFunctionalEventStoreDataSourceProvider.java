package uk.gov.moj.cpp.workmanagement.proxy.healthchecks;

import uk.gov.justice.services.eventsourcing.source.core.EventStoreDataSourceProvider;

import javax.sql.DataSource;

/**
 * This class is not intended to be used and only exists to keep CDI from complaining and without
 * us needing to add dependencies on modules we don't want
 */
public class NonFunctionalEventStoreDataSourceProvider implements EventStoreDataSourceProvider {

    @Override
    public DataSource getDefaultDataSource() {
        throw new UnsupportedOperationException("Do not use. Class only exists to keep CDI happy");
    }

    @Override
    public DataSource getDataSource(final String jndiName) {
        throw new UnsupportedOperationException("Do not use. Class only exists to keep CDI happy");
    }
}
