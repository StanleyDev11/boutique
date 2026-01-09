package com.example.boutique.multitenancy;

import com.example.boutique.model.Caisse;
import com.example.boutique.model.Client;
import com.example.boutique.model.Personnel;
import com.example.boutique.model.Produit;
import com.example.boutique.model.Vente;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class TenantAwareEventListener implements PreInsertEventListener, PreUpdateEventListener {

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        if (event.getEntity() instanceof Produit) {
            ((Produit) event.getEntity()).setClientId(TenantContext.getCurrentTenant());
        } else if (event.getEntity() instanceof Caisse) {
            ((Caisse) event.getEntity()).setClientId(TenantContext.getCurrentTenant());
        } else if (event.getEntity() instanceof Vente) {
            ((Vente) event.getEntity()).setClientId(TenantContext.getCurrentTenant());
        } else if (event.getEntity() instanceof Client) {
            ((Client) event.getEntity()).setClientId(TenantContext.getCurrentTenant());
        } else if (event.getEntity() instanceof Personnel) {
            ((Personnel) event.getEntity()).setClientId(TenantContext.getCurrentTenant());
        }
        return false;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        if (event.getEntity() instanceof Produit) {
            ((Produit) event.getEntity()).setClientId(TenantContext.getCurrentTenant());
        } else if (event.getEntity() instanceof Caisse) {
            ((Caisse) event.getEntity()).setClientId(TenantContext.getCurrentTenant());
        } else if (event.getEntity() instanceof Vente) {
            ((Vente) event.getEntity()).setClientId(TenantContext.getCurrentTenant());
        } else if (event.getEntity() instanceof Client) {
            ((Client) event.getEntity()).setClientId(TenantContext.getCurrentTenant());
        } else if (event.getEntity() instanceof Personnel) {
            ((Personnel) event.getEntity()).setClientId(TenantContext.getCurrentTenant());
        }
        return false;
    }
}
