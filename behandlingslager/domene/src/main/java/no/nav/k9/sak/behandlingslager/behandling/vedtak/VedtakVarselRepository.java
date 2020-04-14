package no.nav.k9.sak.behandlingslager.behandling.vedtak;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@Dependent
public class VedtakVarselRepository {

    private EntityManager entityManager;

    @Inject
    public VedtakVarselRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected VedtakVarselRepository() {
    }

    public Optional<VedtakVarsel> hentHvisEksisterer(Long behandlingId) {
        TypedQuery<VedtakVarsel> query = entityManager
            .createQuery("from VedtakVarsel where behandlingId = :behandlingId", VedtakVarsel.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public VedtakVarsel hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Forventet behandlingsresultat"));
    }

    public void lagre(Long behandlingId, VedtakVarsel vedtakVarsel) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(vedtakVarsel, "vedtakVarsel");
        vedtakVarsel.setBehandlingId(behandlingId);
        entityManager.persist(vedtakVarsel);
        entityManager.flush();
    }

    public Optional<VedtakVarsel> hentHvisEksisterer(UUID behandlingUuid) {
        TypedQuery<VedtakVarsel> query = entityManager
                .createQuery("SELECT v from VedtakVarsel v INNER JOIN Behandling b on b.id=v.behandlingId where b.uuid = :uuid", VedtakVarsel.class);
            query.setParameter("uuid", behandlingUuid);
            return HibernateVerktøy.hentUniktResultat(query);
    }

}
