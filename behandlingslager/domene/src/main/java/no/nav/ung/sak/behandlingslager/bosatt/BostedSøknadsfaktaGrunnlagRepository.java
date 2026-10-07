package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Optional;

@Dependent
public class BostedSøknadsfaktaGrunnlagRepository {

    private static final Logger LOG = LoggerFactory.getLogger(BostedSøknadsfaktaGrunnlagRepository.class);

    private final EntityManager entityManager;

    @Inject
    public BostedSøknadsfaktaGrunnlagRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<BostedSøknadsfaktaGrunnlag> hentGrunnlagHvisEksisterer(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT g FROM BostedSøknadsfaktaGrunnlag g " +
                "WHERE g.behandlingId = :behandlingId AND g.aktiv = true",
            BostedSøknadsfaktaGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Lagrer bostedsopplysning oppgitt av bruker i søknaden på behandlingens søknadsfaktagrunnlag.
     * Oppretter et grunnlag dersom det ikke finnes fra før. Eksisterende opplysning for samme journalpostId erstattes.
     */
    public void lagreInformasjonFraSøknad(Long behandlingId, String journalpostId, LocalDate startdato, boolean erBosattITrondheim) {
        var eksisterendeGrunnlag = hentGrunnlagHvisEksisterer(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag
            .map(BostedSøknadsfaktaGrunnlag::nyttGrunnlagMedReferanserFra)
            .orElseGet(() -> new BostedSøknadsfaktaGrunnlag(behandlingId));

        nyttGrunnlag.leggTilInformasjonFraSøknad(new BostedsinformasjonFraSøknad(journalpostId, startdato, erBosattITrondheim));

        if (eksisterendeGrunnlag.isPresent()) {
            if (eksisterendeGrunnlag.get().equals(nyttGrunnlag)) {
                LOG.info("lagreInformasjonFraSøknad ga ingen endring i bostedsfaktagrunnlag for behandlingId={}", behandlingId);
                return;
            }
            deaktiverEksisterende(eksisterendeGrunnlag.get());
        }
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    /**
     * Kopierer grunnlag fra en eksisterende behandling til en ny behandling.
     * Holderen refereres — søknadsfakta er additivt og endres aldri i etterkant.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        hentGrunnlagHvisEksisterer(gammelBehandlingId).ifPresent(eksisterende -> {
            var nyttGrunnlag = BostedSøknadsfaktaGrunnlag.nyttGrunnlagForBehandlingMedReferanserFra(nyBehandlingId, eksisterende);
            entityManager.persist(nyttGrunnlag);
            entityManager.flush();
        });
    }

    private void deaktiverEksisterende(BostedSøknadsfaktaGrunnlag gr) {
        gr.deaktiver();
        entityManager.persist(gr);
        entityManager.flush();
    }
}
