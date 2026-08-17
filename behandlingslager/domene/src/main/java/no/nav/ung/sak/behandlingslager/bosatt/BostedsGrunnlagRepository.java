package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;

import no.nav.ung.kodeverk.vilkår.AvklaringStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Dependent
public class BostedsGrunnlagRepository {

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
     * @return Map fra periodestart til periodeAvklaring.referanse
     */
    public Set<UUID> lagreForeslåtteAvklaringer(Long behandlingId, List<BostedsPeriodeAvklaring> nyeAvklaringer) {
        var eksisterendeGrunnlag = hentGrunnlagHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer at grunnlag allerede eksisterer ved lagring av avklaring"));

        var nyttGrunnlag = BostedsGrunnlag.nyttGrunnlagMedReferanserFra(eksisterendeGrunnlag);
        nyttGrunnlag.setForeslåttAvklaring(nyeAvklaringer);

        if (eksisterendeGrunnlag.equals(nyttGrunnlag)) {
            return hentForeslåttAvklaringsreferanser(eksisterendeGrunnlag.getForeslått());
        }
        deaktiverEksisterende(eksisterendeGrunnlag);

        entityManager.persist(nyttGrunnlag);
        entityManager.flush();

        return hentForeslåttAvklaringsreferanser(nyttGrunnlag.getForeslått());
    }

    private static Set<UUID> hentForeslåttAvklaringsreferanser(BostedsAvklaringHolder holder) {
        return holder.hentAvklaringerMedStatus(AvklaringStatus.AVKLARES).stream().map(BostedsPeriodeAvklaring::getReferanse).collect(Collectors.toSet());
    }

    public void lagre(Long behandlingId, Consumer<BostedsGrunnlag> grunnlagsoperasjon) {
        var eksisterendeGrunnlag = hentGrunnlagHvisEksisterer(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag
            .map(BostedsGrunnlag::nyttGrunnlagMedReferanserFra)
            .orElse(new BostedsGrunnlag(behandlingId));

        grunnlagsoperasjon.accept(nyttGrunnlag);

        if (eksisterendeGrunnlag.isPresent()) {
            if (eksisterendeGrunnlag.get().equals(nyttGrunnlag)) {
                return;
            }
            deaktiverEksisterende(eksisterendeGrunnlag.get());
        }
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    /**
     * Kopierer grunnlag fra en eksisterende behandling til en ny behandling.
     * Holder refereres — ingen kopiering av data.
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
