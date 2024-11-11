package no.nav.k9.sak.behandlingslager.grunnbeløp;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Dependent
public class GrunnbeløpRepository {

    private EntityManager entityManager;

    @Inject
    public GrunnbeløpRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<GrunnbeløpSats> hentGrunnbeløpForPeriode(DatoIntervallEntitet periode) {
        TypedQuery<GrunnbeløpSats> query = entityManager.createQuery("from GrunnbeløpSats where periode.fomDato <= :tomDato and periode.tomDato >= :fomDato", GrunnbeløpSats.class);
        query.setParameter("fomDato", periode.getFomDato()); // NOSONAR
        query.setParameter("tomDato", periode.getTomDato()); // NOSONAR
        return query.getResultList();
    }

}
