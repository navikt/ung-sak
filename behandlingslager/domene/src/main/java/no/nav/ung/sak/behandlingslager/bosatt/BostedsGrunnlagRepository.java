package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;

import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
     * Returnerer map fra fomDato til erBosattITrondheim for alle søknadsoppgitte bostedsopplysninger på behandlingen.
     * Dersom to opplysninger har samme fomDato brukes den sist registrerte.
     */
    public Map<LocalDate, Boolean> hentSøknadBostedPerFom(Long behandlingId) {
        return hentGrunnlagHvisEksisterer(behandlingId)
            .map(g -> g.getOppgittFraSøknad().getInformasjon().stream()
                .collect(Collectors.toMap(
                    BostedsinformasjonFraSøknad::getFomDato,
                    BostedsinformasjonFraSøknad::isErBosattITrondheim,
                    (a, b) -> b)))
            .orElse(Map.of());
    }

    /**
     * Lagrer bostedsopplysning oppgitt av bruker i søknaden på behandlingens bostedsgrunnlag.
     * Oppretter et bostedsgrunnlag dersom det ikke finnes fra før. Eksisterende opplysning for
     * samme journalpostId erstattes.
     */
    public void lagreInformasjonFraSøknad(Long behandlingId, String journalpostId, Periode periode, boolean erBosattITrondheim) {
        var eksisterendeGrunnlag = hentGrunnlagHvisEksisterer(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag
            .map(BostedsGrunnlag::nyttGrunnlagMedReferanserFra)
            .orElse(new BostedsGrunnlag(behandlingId));

        nyttGrunnlag.leggTilInformasjonFraSøknad(new BostedsinformasjonFraSøknad(journalpostId, periode.getFom(), erBosattITrondheim));

        // Opprette bostedfaktaforslag ved persistering av søknaddata?
        //        nyttGrunnlag.medForeslåttAvklaring(Map.of(periode, new BostedAvklaringData(
        //            erBosattITrondheim,
        //            null,
        //            null,
        //            Kilde.SØKNAD))
        //        );

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
     * Lagrer saksbehandlers bostedsavklaringer for en behandling (kilde=SAKSBEHANDLER).
     * Nøkkel er skjæringstidspunkt (= vilkårsperiode fom); verdi er {@link BostedAvklaringData}.
     * Beholder referanser til {@code oppgittFraSøknad} og {@code resultat} på grunnlaget.
     *
     * @return Map fra skjæringstidspunkt til periodeAvklaring.referanse
     */
    public Map<LocalDate, UUID> lagreForeslåtteAvklaringerOgFjernOverlappendeResultat(
        Long behandlingId,
        Map<Periode, BostedAvklaringData> avklaringerPerSkjæringstidspunkt) {

        var eksisterendeGrunnlag = hentGrunnlagHvisEksisterer(behandlingId);

        var nyttGrunnlag = eksisterendeGrunnlag
            .map(BostedsGrunnlag::nyttGrunnlagMedReferanserFra)
            .orElse(new BostedsGrunnlag(behandlingId));

        nyttGrunnlag.setForeslåttAvklaring(avklaringerPerSkjæringstidspunkt);
        nyttGrunnlag.fjernOverlappendeResultat(avklaringerPerSkjæringstidspunkt.keySet());

        if (eksisterendeGrunnlag.isPresent()) {
            if (eksisterendeGrunnlag.get().equals(nyttGrunnlag)) {
                return hentForeslåttAvklaringsreferanser(eksisterendeGrunnlag.get().getForeslått());
            }
            deaktiverEksisterende(eksisterendeGrunnlag.get());
        }
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();

        return hentForeslåttAvklaringsreferanser(nyttGrunnlag.getForeslått());
    }

    private static Map<LocalDate, UUID> hentForeslåttAvklaringsreferanser(BostedsAvklaringHolder holder) {
        return holder.getPeriodeAvklaringer().stream()
            .collect(Collectors.toMap(
                p -> p.getPeriode().getFomDato(),
                BostedsPeriodeAvklaring::getReferanse));
    }


    /**
     * Lagrer endelig resultat av bostedsavklaringer på behandlingens grunnlag.
     * Beholder referanser til {@code oppgittFraSøknad} og {@code foreslått} på grunnlaget.
     *
     * @return Map fra skjæringstidspunkt til periodeAvklaring.referanse
     */
    public void lagreAvklaringResultat(Long behandlingId,
                                                       Map<Periode, BostedAvklaringData> avklaringerPerSkjæringstidspunkt) {
        var eksisterendeGrunnlag = hentGrunnlagHvisEksisterer(behandlingId);

        var nyttGrunnlag = eksisterendeGrunnlag
            .map(BostedsGrunnlag::nyttGrunnlagMedReferanserFra)
            .orElse(new BostedsGrunnlag(behandlingId));
        nyttGrunnlag.setResultat(avklaringerPerSkjæringstidspunkt);

        if (eksisterendeGrunnlag.isPresent()) {
            if (eksisterendeGrunnlag.get().getResultat() == nyttGrunnlag.getResultat()) {
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
