package no.nav.k9.sak.produksjonsstyring.behandlingenhet;

import java.lang.annotation.Annotation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.hendelse.BehandlingEnhetEvent;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;


@ApplicationScoped
public class BehandlingEnhetEventPubliserer {

    private BeanManager beanManager;

    BehandlingEnhetEventPubliserer() {
        //Cyclopedia Drainage Invariant
    }

    @Inject
    public BehandlingEnhetEventPubliserer(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    public void fireEvent(Behandling behandling) {
        if (beanManager == null) {
            return;
        }
        BehandlingEnhetEvent event = new BehandlingEnhetEvent(behandling);
        beanManager.fireEvent(event, new Annotation[] {});
    }
}
