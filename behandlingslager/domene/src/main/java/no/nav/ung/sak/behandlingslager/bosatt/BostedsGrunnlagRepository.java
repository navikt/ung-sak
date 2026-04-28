package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;

import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import java.time.LocalDate;
import java.util.LinkedHashSet;
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
     * Ytre nøkkel er skjæringstidspunkt (= vilkårsperiode fom); indre map er sub-avklaringer
     * fra {@code BostedAvklaringUtil.splittAvklaring}.
     * Fastsatt holder settes alltid til null – bruk {@link #fastsettForeslåtteAvklaringer} for å fastsette.
     * SøknadHolder beholdes fra eksisterende grunnlag dersom det finnes.
     *
     * @return Map fra skjæringstidspunkt til periodeAvklaring.referanse
     */
    public Map<LocalDate, UUID> lagreAvklaringer(Long behandlingId,
                                                  Map<LocalDate, Map<LocalDate, Boolean>> avklaringerPerSkjæringstidspunkt) {
        var eksisterende = hentGrunnlagHvisEksisterer(behandlingId);

        var nyForeslåttHolder = byggHolder(avklaringerPerSkjæringstidspunkt);

        if (eksisterende.isPresent() && eksisterende.get().getForeslåttHolder().equals(nyForeslåttHolder)) {
            return eksisterende.get().getForeslåttHolder().getPeriodeAvklaringer().stream()
                .collect(Collectors.toMap(
                    BostedsPeriodeAvklaring::getSkjæringstidspunkt,
                    BostedsPeriodeAvklaring::getReferanse));
        }

        var søknadHolder = eksisterende.map(BostedsGrunnlag::getSøknadHolder).orElse(null);
        var nyttGrunnlag = new BostedsGrunnlag(behandlingId, nyForeslåttHolder, null, søknadHolder);
        eksisterende.ifPresent(this::deaktiverEksisterende);
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();

        return nyForeslåttHolder.getPeriodeAvklaringer().stream()
            .collect(Collectors.toMap(
                BostedsPeriodeAvklaring::getSkjæringstidspunkt,
                BostedsPeriodeAvklaring::getReferanse));
    }

    /**
     * Lagrer bostedsopplysninger oppgitt i søknaden for en behandling.
     * Foreslått- og fastsatt-holder beholdes fra eksisterende grunnlag (eller tomme holders opprettes).
     */
    public void lagreSøknadBosted(Long behandlingId, LocalDate fomDato, boolean erBosattITrondheim) {
        var eksisterende = hentGrunnlagHvisEksisterer(behandlingId);

        var nySøknadHolder = new BostedsAvklaringHolder();
        eksisterende.map(BostedsGrunnlag::getSøknadHolder)
            .ifPresent(eksisterendeSøknad -> eksisterendeSøknad.getPeriodeAvklaringer()
                .forEach(p -> nySøknadHolder.leggTilPeriodeAvklaring(kopierPeriodeAvklaring(p))));
        nySøknadHolder.leggTilPeriodeAvklaring(new BostedsPeriodeAvklaring(
            fomDato,
            new LinkedHashSet<>(Set.of(new BostedsAvklaring(fomDato, erBosattITrondheim)))));

        var foreslåttHolder = eksisterende.map(BostedsGrunnlag::getForeslåttHolder).orElseGet(BostedsAvklaringHolder::new);
        var fastsattHolder = eksisterende.map(BostedsGrunnlag::getFastsattHolder).orElse(null);

        var nyttGrunnlag = new BostedsGrunnlag(behandlingId, foreslåttHolder, fastsattHolder, nySøknadHolder);
        eksisterende.ifPresent(this::deaktiverEksisterende);
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    /**
     * Fastsetter avklaringer direkte for angitte skjæringstidspunkter.
     * Ytre nøkkel er skjæringstidspunkt (= vilkårsperiode fom); indre map er sub-avklaringer.
     * Beholder foreslåttHolder uendret.
     * Brukes av FASTSETT_BOSTED-aksjonspunktet.
     */
    public void fastsettAvklaringerDirekte(Long behandlingId,
                                           Map<LocalDate, Map<LocalDate, Boolean>> avklaringerPerSkjæringstidspunkt) {
        var eksisterende = hentGrunnlagHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer bostedsgrunnlag ved fastsetting, behandlingId=" + behandlingId));

        var nyFastsattHolder = byggHolder(avklaringerPerSkjæringstidspunkt);

        var nyttGrunnlag = new BostedsGrunnlag(behandlingId, eksisterende.getForeslåttHolder(), nyFastsattHolder, eksisterende.getSøknadHolder());
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
        var nyttGrunnlag = new BostedsGrunnlag(behandlingId, eksisterende.getForeslåttHolder(), nyFastsattHolder, eksisterende.getSøknadHolder());
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
            .map(BostedsGrunnlagRepository::kopierPeriodeAvklaring)
            .forEach(nyHolder::leggTilPeriodeAvklaring);

        return nyHolder;
    }

    private static BostedsAvklaringHolder byggHolder(Map<LocalDate, Map<LocalDate, Boolean>> avklaringerPerSkjæringstidspunkt) {
        var holder = new BostedsAvklaringHolder();
        for (var entry : avklaringerPerSkjæringstidspunkt.entrySet()) {
            var skjæringstidspunkt = entry.getKey();
            var avklaringer = entry.getValue().entrySet().stream()
                .map(e -> new BostedsAvklaring(e.getKey(), e.getValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
            holder.leggTilPeriodeAvklaring(new BostedsPeriodeAvklaring(skjæringstidspunkt, avklaringer));
        }
        return holder;
    }

    private static BostedsPeriodeAvklaring kopierPeriodeAvklaring(BostedsPeriodeAvklaring p) {
        return new BostedsPeriodeAvklaring(
            p.getSkjæringstidspunkt(),
            p.getAvklaringer().stream()
                .map(a -> new BostedsAvklaring(a.getFomDato(), a.erBosattITrondheim()))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    /**
     * Kopierer grunnlag fra en eksisterende behandling til en ny behandling.
     * Begge holders (foreslått og fastsatt) refereres – ingen kopiering av data.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        hentGrunnlagHvisEksisterer(gammelBehandlingId).ifPresent(eksisterende -> {
            var nyttGrunnlag = new BostedsGrunnlag(nyBehandlingId, eksisterende.getForeslåttHolder(), eksisterende.getFastsattHolder(), eksisterende.getSøknadHolder());
            entityManager.persist(nyttGrunnlag);
            entityManager.flush();
        });
    }

    public Optional<BostedsGrunnlag> hentGrunnlagFraGrunnlagsReferanse(UUID grunnlagsreferanse) {
        var query = entityManager.createQuery(
            "SELECT g FROM BostedsGrunnlag g WHERE g.grunnlagsreferanse = :grunnlagsreferanse",
            BostedsGrunnlag.class);
        query.setParameter("grunnlagsreferanse", grunnlagsreferanse);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public Optional<BostedsPeriodeAvklaring> hentPeriodeAvklaringFraReferanse(UUID referanse) {
        var query = entityManager.createQuery(
            "SELECT p FROM BostedsPeriodeAvklaring p WHERE p.referanse = :referanse",
            BostedsPeriodeAvklaring.class);
        query.setParameter("referanse", referanse);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void deaktiverEksisterende(BostedsGrunnlag gr) {
        gr.deaktiver();
        entityManager.persist(gr);
        entityManager.flush();
    }
}
