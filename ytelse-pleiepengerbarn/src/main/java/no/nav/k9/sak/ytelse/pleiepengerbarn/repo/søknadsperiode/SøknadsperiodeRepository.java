package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@Dependent
public class SøknadsperiodeRepository {

    private final EntityManager entityManager;

    @Inject
    public SøknadsperiodeRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Set<SøknadsPeriodeDokumenter> hentPerioderKnyttetTilJournalpost(Long behandlingId, Set<JournalpostId> journalpostIder) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        if (eksisterendeGrunnlag.isEmpty()) {
            return Set.of();
        }
        return eksisterendeGrunnlag.get().getSøknadsperioder()
            .getPerioder()
            .stream()
            .filter(it -> journalpostIder.contains(it.getJournalpostId()))
            .map(it -> new SøknadsPeriodeDokumenter(it.getJournalpostId(), it.getPerioder()))
            .collect(Collectors.toSet());
    }

    public void lagre(Long behandlingId, Søknadsperioder søknadsperioder) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new SøknadsperiodeGrunnlag(behandlingId, it))
            .orElse(new SøknadsperiodeGrunnlag());
        nyttGrunnlag.leggTil(søknadsperioder);

        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }

    private void persister(Optional<SøknadsperiodeGrunnlag> eksisterendeGrunnlag, SøknadsperiodeGrunnlag nyttGrunnlag) {
        eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);

        entityManager.persist(nyttGrunnlag.getSøknadsperioder());
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void deaktiverEksisterende(SøknadsperiodeGrunnlag gr) {
        gr.setAktiv(false);
        entityManager.persist(gr);
        entityManager.flush();
    }

    private Optional<SøknadsperiodeGrunnlag> hentEksisterendeGrunnlag(Long id) {
        var query = entityManager.createQuery(
            "FROM SøknadsperiodeGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId " +
                "AND s.aktiv = true", SøknadsperiodeGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(gammelBehandlingId);
        eksisterendeGrunnlag.ifPresent(entitet -> {
            persister(Optional.empty(), new SøknadsperiodeGrunnlag(nyBehandlingId, entitet));
        });
    }
}
