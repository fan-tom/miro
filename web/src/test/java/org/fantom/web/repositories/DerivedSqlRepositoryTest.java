package org.fantom.web.repositories;

import org.fantom.repositories.widget.RepositoryTest;
import org.fantom.web.repositories.widget.SqlWidgetRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(SqlWidgetRepository.class)
public class DerivedSqlRepositoryTest extends RepositoryTest<Long> {

    @Autowired
    private SqlWidgetRepository widgetRepository;

    public DerivedSqlRepositoryTest() {
        super(null);
    }

    @BeforeAll
    public void init() {
        super.repository = widgetRepository;
    }

    @Override
    public void resetRepo() {
        repository.deleteAll();
    }
}
