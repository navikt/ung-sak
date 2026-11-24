package no.nav.ung.sak.behandlingslager.uttalelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.kodeverk.varsel.EndringType;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class UttalelseRepository {

    private EntityManager entityManager;

    public UttalelseRepository() {}

    @Inject
    public UttalelseRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public void lagre(Long behandlingId, UttalelseV2 uttalelse) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new UttalelseGrunnlag(behandlingId, it)).orElse(new UttalelseGrunnlag(behandlingId));
        nyttGrunnlag.leggTilUttalelser(List.of(uttalelse));
        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }

    public void kopier(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(eksisterendeBehandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new UttalelseGrunnlag(nyBehandlingId, it));
        nyttGrunnlag.ifPresent(gr -> {
            entityManager.persist(gr);
            entityManager.flush();
        });
    }

    /**
     * Erstatter uttalelsene av de gitte typene på {@code tilBehandlingId} med kopier av uttalelsene av samme typer
     * fra {@code fraBehandlingId}. Uttalelser av andre typer på {@code tilBehandlingId} beholdes.
     * Brukes når en åpen behandling synkes med vedtatte data fra en annen behandling på fagsaken.
     */
    public void erstattUttalelserMedKopiFra(Long fraBehandlingId, Long tilBehandlingId, Set<EndringType> typer) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(tilBehandlingId);
        var eksisterende = eksisterendeGrunnlag.map(UttalelseRepository::uttalelserIGrunnlag).orElse(Set.of());

        var nye = new HashSet<UttalelseV2>();
        eksisterende.stream().filter(it -> !typer.contains(it.getType())).forEach(nye::add);
        hentEksisterendeGrunnlag(fraBehandlingId).map(UttalelseRepository::uttalelserIGrunnlag).orElse(Set.of()).stream()
            .filter(it -> typer.contains(it.getType()))
            .forEach(nye::add);

        if (nye.equals(eksisterende)) {
            return;
        }
        if (nye.isEmpty()) {
            eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);
            return;
        }
        var nyttGrunnlag = new UttalelseGrunnlag(tilBehandlingId);
        nyttGrunnlag.leggTilUttalelser(nye);
        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }

    private static Set<UttalelseV2> uttalelserIGrunnlag(UttalelseGrunnlag grunnlag) {
        return grunnlag.getUttalelser() == null ? Set.of() : new HashSet<>(grunnlag.getUttalelser().getUttalelser());
    }

    private void persister(Optional <UttalelseGrunnlag> eksisterendeGrunnlag, UttalelseGrunnlag nyttGrunnlag) {
        eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);
        if (nyttGrunnlag.getUttalelser() != null) {
            if (nyttGrunnlag.getUttalelser().getId() == null && nyttGrunnlag.getUttalelser().getUttalelser().stream().anyMatch(it -> it.getId() != null)) {
                throw new IllegalStateException("Forsøk på å gjenbruke eksisterende uttalelse i nytt aggregat");
            }
            entityManager.persist(nyttGrunnlag.getUttalelser());
        }
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void deaktiverEksisterende(UttalelseGrunnlag gr) {
        gr.setAktiv(false);
        entityManager.persist(gr);
        entityManager.flush();
    }

    public Optional<UttalelseGrunnlag> hentEksisterendeGrunnlag(Long behandlingId) {
        final var query = entityManager.createQuery(
            "select ug from UttalelseGrunnlag ug " +
                "where ug.behandlingId = :behandlingId " +
                "and ug.aktiv = true", UttalelseGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public List<UttalelseV2> hentUttalelser(Long behandlingId, EndringType... typer) {
        final var query = entityManager.createQuery(
            "select ug from UttalelseGrunnlag ug " +
                "where ug.behandlingId = :behandlingId " +
                "and ug.aktiv = true", UttalelseGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query).stream().map(UttalelseGrunnlag::getUttalelser).map(Uttalelser::getUttalelser).flatMap(Collection::stream)
            .filter(u -> List.of(typer).contains(u.getType()))
            .toList();
    }

    public Optional<UttalelseGrunnlag> hentGrunnlagBasertPåId(Long id) {
        final var query = entityManager.createQuery(
            "select ug from UttalelseGrunnlag ug " +
                "where ug.id = :id " , UttalelseGrunnlag.class);
        query.setParameter("id", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

}
