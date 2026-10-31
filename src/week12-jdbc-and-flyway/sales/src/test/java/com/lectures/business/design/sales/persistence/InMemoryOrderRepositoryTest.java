package com.lectures.business.design.sales.persistence;

class InMemoryOrderRepositoryTest extends OrderRepositoryContract {

    @Override
    protected OrderRepository createRepository() {
        return new InMemoryOrderRepository();
    }
}
