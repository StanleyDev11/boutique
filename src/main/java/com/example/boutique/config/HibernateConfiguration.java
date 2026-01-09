package com.example.boutique.config;

import com.example.boutique.multitenancy.TenantAwareEventListener;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class HibernateConfiguration {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private TenantAwareEventListener tenantAwareEventListener;

    @PostConstruct
    public void registerListeners() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.PRE_INSERT).appendListener(tenantAwareEventListener);
        registry.getEventListenerGroup(EventType.PRE_UPDATE).appendListener(tenantAwareEventListener);
    }
}
