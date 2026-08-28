package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

@Dependent
public class BostedsGrunnlagRepository {

    private static final Logger LOG = LoggerFactory.getLogger(BostedsGrunnlagRepository.class);

    private final EntityManager entityManager;

    @Inject
    public BostedsGrunnlagRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<BostedsGrunnlag> hentGrunnlagHvisEksisterer(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT g FROM BostedsGrunnlag g " +
                "WHERE g.behandlingId = :behandlingId AND g.aktiv = true",
            BostedsGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Lagrer bostedsopplysning oppgitt av bruker i søknaden på behandlingens bostedsgrunnlag.
     * Oppretter et bostedsgrunnlag dersom det ikke finnes fra før. Eksisterende opplysning for
     * samme journalpostId erstattes.
     */
    public void lagreInformasjonFraSøknad(Long behandlingId, String journalpostId, LocalDate startdato, boolean erBosattITrondheim) {
        var eksisterendeGrunnlag = hentGrunnlagHvisEksisterer(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag
            .map(BostedsGrunnlag::nyttGrunnlagMedReferanserFra)
            .orElse(new BostedsGrunnlag(behandlingId));

        nyttGrunnlag.leggTilInformasjonFraSøknad(new BostedsinformasjonFraSøknad(journalpostId, startdato, erBosattITrondheim));

        if (eksisterendeGrunnlag.isPresent()) {
            if (eksisterendeGrunnlag.get().equals(nyttGrunnlag)) {
                LOG.info("lagreInformasjonFraSøknad ga ingen endring i bostedsgrunnlag for behandlingId={}", behandlingId);
                return;
            }
            deaktiverEksisterende(eksisterendeGrunnlag.get());
        }
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    /**
     * Lagrer saksbehandlers bostedsavklaringer for en behandling.
     * Beholder referanser til {@code oppgittFraSøknad} og {@code resultat} på grunnlaget.
     *
     * @return avklaringene som nå er foreslått på behandlingen
     */
    public Set<BostedsPeriodeAvklaring> lagreForeslåtteAvklaringer(Long behandlingId, Set<BostedsPeriodeAvklaringForeslått> nyeAvklaringer) {
        var eksisterendeGrunnlag = hentGrunnlagHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer at grunnlag allerede eksisterer ved lagring av avklaring"));

        var nyttGrunnlag = BostedsGrunnlag.nyttGrunnlagMedReferanserFra(eksisterendeGrunnlag);
        nyttGrunnlag.setForeslåtteAvklaringer(nyeAvklaringer);

        if (eksisterendeGrunnlag.equals(nyttGrunnlag)) {
            LOG.info("lagreForeslåtteAvklaringer ga ingen endring i bostedsgrunnlag for behandlingId={}", behandlingId);
            return new HashSet<>(eksisterendeGrunnlag.getForeslåtteAvklaringer());
        }
        deaktiverEksisterende(eksisterendeGrunnlag);

        entityManager.persist(nyttGrunnlag);
        entityManager.flush();

        return new HashSet<>(nyttGrunnlag.getForeslåtteAvklaringer());
    }

    public void ferdigstillForeslåtteAvklaringer(long behandlingId) {
        var bleEndret = lagre(behandlingId, BostedsGrunnlag::ferdigstillForeslåtteAvklaringer);
        if (!bleEndret) {
            LOG.info("ferdigstillForeslåtteAvklaringer ga ingen endring i bostedsgrunnlag for behandlingId={}", behandlingId);
        }
    }

    public boolean lagre(Long behandlingId, Consumer<BostedsGrunnlag> grunnlagsoperasjon) {
        var eksisterendeGrunnlag = hentGrunnlagHvisEksisterer(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag
            .map(BostedsGrunnlag::nyttGrunnlagMedReferanserFra)
            .orElse(new BostedsGrunnlag(behandlingId));

        grunnlagsoperasjon.accept(nyttGrunnlag);

        if (eksisterendeGrunnlag.isPresent()) {
            if (eksisterendeGrunnlag.get().equals(nyttGrunnlag)) {
                return false;
            }
            deaktiverEksisterende(eksisterendeGrunnlag.get());
        }
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
        return true;
    }

    /**
     * Kopierer grunnlag fra en eksisterende behandling til en ny behandling.
     * Holder refereres — ingen kopiering av data, med mindre den forrige behandlingen har foreslåtte avklaringer
     * som ikke skal gjelde for den nye behandlingen.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        hentGrunnlagHvisEksisterer(gammelBehandlingId).ifPresent(eksisterende -> {
            var nyttGrunnlag = BostedsGrunnlag.nyttGrunnlagForBehandlingMedReferanserFra(nyBehandlingId, eksisterende);
            entityManager.persist(nyttGrunnlag);
            entityManager.flush();
        });
    }

    private void deaktiverEksisterende(BostedsGrunnlag gr) {
        gr.deaktiver();
        entityManager.persist(gr);
        entityManager.flush();
    }
}
