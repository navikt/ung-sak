package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Dependent
public class BosattSøknadGrunnlagRepository {

    private final EntityManager entityManager;

    @Inject
    public BosattSøknadGrunnlagRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<BosattSøknadGrunnlag> hentSøknadGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT g FROM BosattSøknadGrunnlag g WHERE g.behandlingId = :behandlingId",
            BosattSøknadGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Lagrer søknadsbasert bostedsinformasjon. Erstatter eventuelt eksisterende innslag med samme journalpostId.
     */
    public void lagreSøknadBosted(Long behandlingId, String journalpostId, LocalDate fomDato, boolean erBosattITrondheim) {
        var grunnlag = hentSøknadGrunnlag(behandlingId)
            .orElseGet(() -> new BosattSøknadGrunnlag(behandlingId));
        grunnlag.leggTilInformasjon(new BostedsinformasjonFraSøknad(journalpostId, fomDato, erBosattITrondheim));
        entityManager.persist(grunnlag);
        entityManager.flush();
    }

    /**
     * Returnerer map fra fomDato til erBosattITrondheim for alle søknadsinnslag på behandlingen.
     * Dersom to innslag har samme fomDato brukes det sist registrerte (innsatt sist i LinkedHashSet).
     */
    public Map<LocalDate, Boolean> hentSøknadBostedPerFom(Long behandlingId) {
        return hentSøknadGrunnlag(behandlingId)
            .map(g -> g.getInformasjon().stream()
                .collect(Collectors.toMap(
                    BostedsinformasjonFraSøknad::getFomDato,
                    BostedsinformasjonFraSøknad::isErBosattITrondheim,
                    (a, b) -> b)))
            .orElse(Map.of());
    }

    /**
     * Kopierer søknadsgrunnlag fra en eksisterende behandling til en ny behandling.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        hentSøknadGrunnlag(gammelBehandlingId).ifPresent(eksisterende -> {
            var nyttGrunnlag = new BosattSøknadGrunnlag(nyBehandlingId);
            eksisterende.getInformasjon()
                .forEach(i -> nyttGrunnlag.leggTilInformasjon(
                    new BostedsinformasjonFraSøknad(i.getJournalpostId(), i.getFomDato(), i.isErBosattITrondheim())));
            entityManager.persist(nyttGrunnlag);
            entityManager.flush();
        });
    }
}
