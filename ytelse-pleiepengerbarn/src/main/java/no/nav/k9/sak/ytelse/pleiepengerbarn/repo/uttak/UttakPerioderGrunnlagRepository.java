package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

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
public class UttakPerioderGrunnlagRepository {

    private final EntityManager entityManager;

    @Inject
    public UttakPerioderGrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Set<UttakPerioderDokumenter> hentPerioderKnyttetTilJournalpost(Long behandlingId, Set<JournalpostId> journalpostIder) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        if (eksisterendeGrunnlag.isEmpty()) {
            return Set.of();
        }
        return eksisterendeGrunnlag.get().getOppgitteSøknadsperioder()
            .getPerioderFraSøknadene()
            .stream()
            .filter(it -> journalpostIder.contains(it.getJournalpostId()))
            .map(it -> new UttakPerioderDokumenter(it.getJournalpostId(), it.getArbeidPerioder()))
            .collect(Collectors.toSet());
    }

    public Optional<UttaksPerioderGrunnlag> hentGrunnlag(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagre(Long behandlingId, PerioderFraSøknad perioderFraSøknad) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new UttaksPerioderGrunnlag(behandlingId, it))
            .orElse(new UttaksPerioderGrunnlag(behandlingId));
        nyttGrunnlag.leggTil(perioderFraSøknad);

        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }

    /**
     * Perioder som er knyttet til denne behandlingen. Mao dokumentet periodene kom inn i er knyttet til behandlingen
     * Eventuelt en revurdering som
     * @param behandlingId
     * @param søknadsperioder
     */
    public void lagreRelevantePerioder(Long behandlingId, UttakPerioderHolder søknadsperioder) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new UttaksPerioderGrunnlag(behandlingId, it))
            .orElse(new UttaksPerioderGrunnlag(behandlingId));
        nyttGrunnlag.setRelevanteSøknadsperioder(søknadsperioder);

        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }

    private void persister(Optional<UttaksPerioderGrunnlag> eksisterendeGrunnlag, UttaksPerioderGrunnlag nyttGrunnlag) {
        eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);

        if (nyttGrunnlag.getOppgitteSøknadsperioder() != null) {
            entityManager.persist(nyttGrunnlag.getOppgitteSøknadsperioder());
        }
        if (nyttGrunnlag.getRelevantSøknadsperioder() != null) {
            entityManager.persist(nyttGrunnlag.getRelevantSøknadsperioder());
        }
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void deaktiverEksisterende(UttaksPerioderGrunnlag gr) {
        gr.setAktiv(false);
        entityManager.persist(gr);
        entityManager.flush();
    }

    private Optional<UttaksPerioderGrunnlag> hentEksisterendeGrunnlag(Long id) {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM UttakPerioderGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId " +
                "AND s.aktiv = true", UttaksPerioderGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(gammelBehandlingId);
        eksisterendeGrunnlag.ifPresent(entitet -> {
            persister(Optional.empty(), new UttaksPerioderGrunnlag(nyBehandlingId, entitet));
        });
    }
}
