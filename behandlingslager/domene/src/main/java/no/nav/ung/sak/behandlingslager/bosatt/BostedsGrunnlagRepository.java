package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;

import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
     * Lagrer saksbehandlers foreslåtte bostedsavklaringer for en behandling.
     * Nøkkel er skjæringstidspunkt (= vilkårsperiode fom); verdi er {@link BostedAvklaringData}.
     * Fastsatt holder settes alltid til null – bruk {@link #fastsettForeslåtteAvklaringer} for å fastsette.
     *
     * @return Map fra skjæringstidspunkt til periodeAvklaring.referanse
     */
    public Map<LocalDate, UUID> lagreAvklaringer(Long behandlingId,
                                                  Map<LocalDate, BostedAvklaringData> avklaringerPerSkjæringstidspunkt) {
        var eksisterende = hentGrunnlagHvisEksisterer(behandlingId);

        var nyForeslåttHolder = byggHolder(avklaringerPerSkjæringstidspunkt);

        if (eksisterende.isPresent() && eksisterende.get().getForeslåttHolder().equals(nyForeslåttHolder)) {
            return eksisterende.get().getForeslåttHolder().getPeriodeAvklaringer().stream()
                .collect(Collectors.toMap(
                    BostedsPeriodeAvklaring::getSkjæringstidspunkt,
                    BostedsPeriodeAvklaring::getReferanse));
        }

        var nyttGrunnlag = new BostedsGrunnlag(behandlingId, nyForeslåttHolder, null);
        eksisterende.ifPresent(this::deaktiverEksisterende);
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();

        return nyForeslåttHolder.getPeriodeAvklaringer().stream()
            .collect(Collectors.toMap(
                BostedsPeriodeAvklaring::getSkjæringstidspunkt,
                BostedsPeriodeAvklaring::getReferanse));
    }

    /**
     * Fastsetter avklaringer direkte for angitte skjæringstidspunkter.
     * Beholder foreslåttHolder uendret.
     * Brukes av FASTSETT_BOSTED-aksjonspunktet.
     */
    public void fastsettAvklaringerDirekte(Long behandlingId,
                                           Map<LocalDate, BostedAvklaringData> avklaringerPerSkjæringstidspunkt) {
        var eksisterende = hentGrunnlagHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer bostedsgrunnlag ved fastsetting, behandlingId=" + behandlingId));

        var nyFastsattHolder = byggHolder(avklaringerPerSkjæringstidspunkt);

        var nyttGrunnlag = new BostedsGrunnlag(behandlingId, eksisterende.getForeslåttHolder(), nyFastsattHolder);
        deaktiverEksisterende(eksisterende);
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    /**
     * Fastsetter foreslåtte avklaringer for angitte perioder.
     * Kopierer alle periodeAvklaringer med skjæringstidspunkt innenfor en av de angitte periodene
     * fra foreslåttHolder til en ny fastsattHolder.
     */
    public void fastsettForeslåtteAvklaringer(Long behandlingId, Set<DatoIntervallEntitet> perioder) {
        var eksisterende = hentGrunnlagHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer bostedsgrunnlag ved fastsetting, behandlingId=" + behandlingId));

        var nyFastsattHolder = byggFastsattHolder(eksisterende, perioder);
        var nyttGrunnlag = new BostedsGrunnlag(behandlingId, eksisterende.getForeslåttHolder(), nyFastsattHolder);
        deaktiverEksisterende(eksisterende);
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private BostedsAvklaringHolder byggFastsattHolder(BostedsGrunnlag eksisterende, Set<DatoIntervallEntitet> perioder) {
        var eksisterendeFastsatt = eksisterende.getFastsattHolder();
        var nyHolder = eksisterendeFastsatt != null ? new BostedsAvklaringHolder(eksisterendeFastsatt) : new BostedsAvklaringHolder();

        eksisterende.getForeslåttHolder().getPeriodeAvklaringer().stream()
            .filter(p -> perioder.stream().anyMatch(periode ->
                !p.getSkjæringstidspunkt().isBefore(periode.getFomDato())
                    && !p.getSkjæringstidspunkt().isAfter(periode.getTomDato())))
            .map(p -> new BostedsPeriodeAvklaring(p.getSkjæringstidspunkt(), p.isErBosattITrondheim(), p.getFraflyttingsDato(), p.getFraflyttingsÅrsak()))
            .forEach(nyHolder::leggTilPeriodeAvklaring);

        return nyHolder;
    }

    private static BostedsAvklaringHolder byggHolder(Map<LocalDate, BostedAvklaringData> avklaringerPerSkjæringstidspunkt) {
        var holder = new BostedsAvklaringHolder();
        for (var entry : avklaringerPerSkjæringstidspunkt.entrySet()) {
            var skjæringstidspunkt = entry.getKey();
            var data = entry.getValue();
            holder.leggTilPeriodeAvklaring(new BostedsPeriodeAvklaring(
                skjæringstidspunkt, data.erBosattITrondheim(), data.fraflyttingsDato(), data.fraflyttingsÅrsak()));
        }
        return holder;
    }

    /**
     * Kopierer grunnlag fra en eksisterende behandling til en ny behandling.
     * Begge holders (foreslått og fastsatt) refereres – ingen kopiering av data.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        hentGrunnlagHvisEksisterer(gammelBehandlingId).ifPresent(eksisterende -> {
            var nyttGrunnlag = new BostedsGrunnlag(nyBehandlingId, eksisterende.getForeslåttHolder(), eksisterende.getFastsattHolder());
            entityManager.persist(nyttGrunnlag);
            entityManager.flush();
        });
    }

    public Optional<BostedsPeriodeAvklaring> hentPeriodeAvklaringFraReferanse(UUID referanse) {
        var query = entityManager.createQuery(
            "SELECT p FROM BostedsPeriodeAvklaring p WHERE p.referanse = :referanse",
            BostedsPeriodeAvklaring.class);
        query.setParameter("referanse", referanse);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Lagrer manuell fritekstvurdering for perioder med årsak ANNET.
     * Oppdaterer {@code begrunnelseVedAnnet} på eksisterende fastsatte periodeAvklaringer.
     * Forutsetter at fastsattHolder finnes.
     */
    public void lagreBegrunnelseVedAnnet(Long behandlingId, Map<LocalDate, String> begrunnelserPerFom) {
        var grunnlag = hentGrunnlagHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer bostedsgrunnlag ved lagring av begrunnelse, behandlingId=" + behandlingId));
        var fastsattHolder = grunnlag.getFastsattHolder();
        if (fastsattHolder == null) {
            throw new IllegalStateException("Forventer fastsattHolder ved lagring av begrunnelse, behandlingId=" + behandlingId);
        }
        fastsattHolder.getPeriodeAvklaringer().stream()
            .filter(p -> begrunnelserPerFom.containsKey(p.getSkjæringstidspunkt()))
            .forEach(p -> p.setBegrunnelseVedAnnet(begrunnelserPerFom.get(p.getSkjæringstidspunkt())));
        entityManager.flush();
    }

    private void deaktiverEksisterende(BostedsGrunnlag gr) {
        gr.deaktiver();
        entityManager.persist(gr);
        entityManager.flush();
    }
}
