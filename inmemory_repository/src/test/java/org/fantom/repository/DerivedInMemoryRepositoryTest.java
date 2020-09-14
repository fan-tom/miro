package org.fantom.repository;

import org.fantom.repositories.widget.IdGenerator;
import org.fantom.repositories.widget.RepositoryTest;

public class DerivedInMemoryRepositoryTest extends RepositoryTest<Integer> {
    private static class IntegerIdGenerator implements IdGenerator<Integer> {
        private int nextValue = Integer.MIN_VALUE;

        @Override
        public Integer generate() {
            return nextValue++;
        }
    }

    public DerivedInMemoryRepositoryTest() {
        super(new InMemoryWidgetRepository<>(new IntegerIdGenerator()));
    }

    @Override
    public void resetRepo() {
        repository = new InMemoryWidgetRepository<>(new IntegerIdGenerator());
    }
}
