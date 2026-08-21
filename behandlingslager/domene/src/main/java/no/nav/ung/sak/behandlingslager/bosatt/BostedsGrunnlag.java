package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.AvklaringStatus;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

import java.time.LocalDate;
import java.util.*;

/**
 * Grunnlag som kobler en behandling til bostedsavklarings-aggregatet.
 * Grunnlagsreferansen brukes som nøkkel i Etterlysning-tabellen.
 */
@Entity(name = "BostedsGrunnlag")
@Table(name = "GR_BOSATT_AVKLARING")
public class BostedsGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_BOSATT_AVKLARING")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "bostedsinformasjon_soeknad_holder_id", nullable = false)
    private BostedsinformasjonFraSøknadHolder oppgittFraSøknad;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "foreslatt_holder_id", updatable = false)
    private BostedsAvklaringHolder foreslått;

    @Column(name = "grunnlag_ref", nullable = false, updatable = false)
    private UUID grunnlagsreferanse;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public BostedsGrunnlag() {
    }

    BostedsGrunnlag(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        this.behandlingId = behandlingId;
        this.grunnlagsreferanse = UUID.randomUUID();
    }

    BostedsGrunnlag(Long behandlingId, BostedsinformasjonFraSøknadHolder oppgittFraSøknad, BostedsAvklaringHolder foreslått) {
        this.behandlingId = behandlingId;
        this.oppgittFraSøknad = oppgittFraSøknad;
        this.foreslått = foreslått;
        this.grunnlagsreferanse = UUID.randomUUID();
    }

    // Oppretter en ny holder ved hver endring av innhold, slik at vi er sikker på å ikke mutere data fra tidligere behandlinger
    void leggTilInformasjonFraSøknad(BostedsinformasjonFraSøknad info) {
        var holder = new BostedsinformasjonFraSøknadHolder(oppgittFraSøknad);
        holder.leggTilInformasjon(info);

        // Beholder den gamle holder hvis det viser seg at ingen endringer har skjedd
        if (holder.equals(oppgittFraSøknad)) {
            return;
        }
        this.oppgittFraSøknad = holder;
    }

    /**
     * Bygger ny holder fra avklaringene og setter foreslått — kun hvis innholdet faktisk er endret.
     * Beholder gammel holder-referanse ved ingen endring (tilsvarende {@link #leggTilInformasjonFraSøknad}).
     */
    void setForeslåttAvklaring(Set<BostedsPeriodeAvklaring> avklaringer) {
        var nyHolder = BostedsAvklaringHolder.lagSkrivbarKopi(this.foreslått);
        nyHolder.leggTilEllerErstattPeriodeAvklaringerUnderArbeid(avklaringer);

        if (nyHolder.equals(this.foreslått)) {
            return;
        }
        this.foreslått = nyHolder;
    }

    void settAlleAvklaringerTilFerdig() {
        var nyHolder = BostedsAvklaringHolder.lagSkrivbarKopi(this.foreslått);
        nyHolder.settAlleAvklaringerTilFerdig();

        if (nyHolder.equals(this.foreslått)) {
            return;
        }
        this.foreslått = nyHolder;
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public BostedsAvklaringHolderSkrivebeskyttet getForeslått() {
        return foreslått;
    }

    public Set<BostedsPeriodeAvklaring> getForeslåtteAvklaringerEllerTomListe() {
        return Optional.ofNullable(getForeslått())
            .map(BostedsAvklaringHolderSkrivebeskyttet::hentPeriodeAvklaringer)
            .orElse(Set.of());
    }

    public List<BostedsPeriodeAvklaring> getForeslåtteAvklaringerMedStatus(AvklaringStatus... status) {
        return Optional.ofNullable(getForeslått())
            .map(f -> f.hentPeriodeAvklaringerMedStatus(status))
            .orElse(List.of());
    }

    public LocalDateTimeline<BostedsPeriodeAvklaring> getForeslåtteAvklaringerMedStatusSomTidslinje(AvklaringStatus... status) {
        return BostedsAvklaringHolder.byggAvklaringTidslinje(getForeslåtteAvklaringerMedStatus(status));
    }

    BostedsinformasjonFraSøknadHolder getOppgittFraSøknad() {
        return oppgittFraSøknad;
    }

    /**
     * Bygger en tidslinje av {@link BostedsinformasjonFraSøknad}. Hver søknad dekker fra sin fomDato til dagen før neste søknads fomDato.
     * Den siste søknaden får tom = {@link LocalDateInterval#TIDENES_ENDE} (åpen slutt) istedenfor 260 dager for å ikke ta stilling til eventuell kortere søknadsperiode her.
     * Denne metoden forutsetter at søknadene kommer inn med økende fom dato.
     */
    public LocalDateTimeline<BostedsinformasjonFraSøknad> hentSøknadsfaktaSomTidslinje() {
        if (oppgittFraSøknad == null) {
            return new LocalDateTimeline<>(Collections.emptyList());
        }

        Map<LocalDate, BostedsinformasjonFraSøknad> søknadPerFom = oppgittFraSøknad.hentSomMap();

        List<LocalDate> sortertFom = søknadPerFom.keySet()
            .stream()
            .sorted()
            .toList();

        List<LocalDateSegment<BostedsinformasjonFraSøknad>> segmenter = new ArrayList<>();
        for (int i = 0; i < sortertFom.size(); i++) {
            LocalDate fom = sortertFom.get(i);
            LocalDate tom = (i < sortertFom.size() - 1)
                ? sortertFom.get(i + 1).minusDays(1)
                : LocalDateInterval.TIDENES_ENDE;
            segmenter.add(new LocalDateSegment<>(fom, tom, søknadPerFom.get(fom)));
        }

        return new LocalDateTimeline<>(segmenter);
    }

    /**
     * Bygger en tidslinje for vurdert periode der oppgitt fakta fra søknad flettes sammen med eventuell foreslått
     * avklaring fra saksbehandler. Foreslått avklaring er kilde til sannhet der de overlapper.
     * Baserer seg på {@link #hentSøknadsfaktaSomTidslinje()} og mapper til {@link BostedsfaktaOgAvklaring}.
     */
    public LocalDateTimeline<BostedsfaktaOgAvklaring> hentOppgittOgForeslåttFaktaMedStatusSomTidslinje(AvklaringStatus... status) {
        var søknadsTidslinje = hentSøknadsfaktaSomTidslinje();
        var foreslåttTidslinje = getForeslåtteAvklaringerMedStatusSomTidslinje(status);

        return søknadsTidslinje.combine(foreslåttTidslinje,
            (di, søknad, avklaring) -> new LocalDateSegment<>(di, new BostedsfaktaOgAvklaring(
                søknad == null ? null : søknad.getValue(),
                avklaring == null ? null : avklaring.getValue()
            )),
            LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    public UUID getGrunnlagsreferanse() {
        return grunnlagsreferanse;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsGrunnlag that)) return false;
        return Objects.equals(oppgittFraSøknad, that.oppgittFraSøknad) &&
            Objects.equals(foreslått, that.foreslått);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgittFraSøknad, foreslått);
    }

    @Override
    public String toString() {
        return "BostedsGrunnlag{behandlingId=" + behandlingId
            + ", grunnlagsreferanse=" + grunnlagsreferanse
            + ", aktiv=" + aktiv + '}';
    }

    public static BostedsGrunnlag nyttGrunnlagMedReferanserFra(BostedsGrunnlag grunnlag) {
        return new BostedsGrunnlag(
            grunnlag.getBehandlingId(),
            grunnlag.getOppgittFraSøknad(),
            (BostedsAvklaringHolder) grunnlag.getForeslått()
        );
    }

    public static BostedsGrunnlag nyttGrunnlagForBehandlingMedReferanserFra(Long behandlingId, BostedsGrunnlag grunnlag) {
        return new BostedsGrunnlag(
            behandlingId,
            grunnlag.getOppgittFraSøknad(),
            (BostedsAvklaringHolder) grunnlag.getForeslått()
        );
    }
}
