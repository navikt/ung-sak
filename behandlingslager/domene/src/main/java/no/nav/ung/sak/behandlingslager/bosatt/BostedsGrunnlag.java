package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
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
    @JoinColumn(name = "avklaring_holder_id", updatable = false)
    private BostedsAvklaringHolder avklaringer;

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

    BostedsGrunnlag(Long behandlingId, BostedsinformasjonFraSøknadHolder oppgittFraSøknad, BostedsAvklaringHolder avklaringer) {
        this.behandlingId = behandlingId;
        this.oppgittFraSøknad = oppgittFraSøknad;
        this.avklaringer = avklaringer;
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
     * Bygger ny holder med de foreslåtte avklaringene — kun hvis innholdet faktisk er endret.
     * Beholder gammel holder-referanse ved ingen endring (tilsvarende {@link #leggTilInformasjonFraSøknad}).
     */
    void setForeslåtteAvklaringer(Set<BostedsPeriodeAvklaringForeslått> avklaringer) {
        var nyHolder = BostedsAvklaringHolder.lagKopi(this.avklaringer);
        nyHolder.erstattForeslåttePeriodeAvklaringer(avklaringer);

        if (nyHolder.equals(this.avklaringer)) {
            return;
        }
        this.avklaringer = nyHolder;
    }

    void ferdigstillForeslåtteAvklaringer() {
        var nyHolder = BostedsAvklaringHolder.lagKopi(this.avklaringer);
        nyHolder.ferdigstillForeslåtteAvklaringer();

        if (nyHolder.equals(this.avklaringer)) {
            return;
        }
        this.avklaringer = nyHolder;
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    /**
     * Holderen med alle bostedsavklaringer på saken — både de foreslåtte i denne behandlingen og de ferdigstilte.
     */
    BostedsAvklaringHolder getAvklaringer() {
        return avklaringer;
    }

    /**
     * Avklaringene som er foreslått og behandlet i denne behandlingen — uavhengig av om de er ferdigstilt.
     */
    public Set<BostedsPeriodeAvklaring> getForeslåtteAvklaringer() {
        return Optional.ofNullable(avklaringer)
            .map(BostedsAvklaringHolder::hentForeslåtteAvklaringer)
            .orElse(Set.of());
    }

    /**
     * Alle ferdigstilte avklaringer på saken, akkumulert på tvers av behandlinger.
     */
    public Set<BostedsPeriodeAvklaring> getFerdigstilteAvklaringer() {
        return Optional.ofNullable(avklaringer)
            .map(BostedsAvklaringHolder::hentFerdigstilteAvklaringer)
            .orElse(Set.of());
    }

    public LocalDateTimeline<BostedsPeriodeAvklaring> getForeslåtteAvklaringerSomTidslinje() {
        return BostedsAvklaringHolder.byggAvklaringTidslinje(getForeslåtteAvklaringer());
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
     * Bygger en tidslinje for vurdert periode der oppgitt fakta fra søknad flettes sammen med avklaringene som er
     * foreslått i denne behandlingen. Foreslått avklaring er kilde til sannhet der de overlapper.
     * Baserer seg på {@link #hentSøknadsfaktaSomTidslinje()} og mapper til {@link BostedsfaktaOgAvklaring}.
     */
    public LocalDateTimeline<BostedsfaktaOgAvklaring> hentOppgittOgForeslåttFaktaSomTidslinje() {
        return flettMedSøknadsfakta(getForeslåtteAvklaringerSomTidslinje());
    }

    /**
     * Som {@link #hentOppgittOgForeslåttFaktaSomTidslinje()}, men inkluderer også tidligere ferdigstilte avklaringer.
     * Avklaringer foreslått i denne behandlingen overstyrer de ferdigstilte der de overlapper.
     */
    public LocalDateTimeline<BostedsfaktaOgAvklaring> hentOppgittOgAlleAvklaringerSomTidslinje() {
        var alleAvklaringer = getForeslåtteAvklaringerSomTidslinje()
            .crossJoin(BostedsAvklaringHolder.byggAvklaringTidslinje(getFerdigstilteAvklaringer()));
        return flettMedSøknadsfakta(alleAvklaringer);
    }

    private LocalDateTimeline<BostedsfaktaOgAvklaring> flettMedSøknadsfakta(LocalDateTimeline<BostedsPeriodeAvklaring> avklaringTidslinje) {
        return hentSøknadsfaktaSomTidslinje().combine(avklaringTidslinje,
            (di, søknad, avklaring) -> new LocalDateSegment<>(di, new BostedsfaktaOgAvklaring(
                søknad == null ? null : søknad.getValue(),
                avklaring == null ? null : avklaring.getValue(),
                avklaring != null && kanRedigeres(avklaring.getValue())
            )),
            LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    private boolean kanRedigeres(BostedsPeriodeAvklaring avklaring) {
        return avklaringer != null && avklaringer.erForeslåttOgIkkeFerdigstilt(avklaring);
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
            Objects.equals(avklaringer, that.avklaringer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgittFraSøknad, avklaringer);
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
            grunnlag.avklaringer
        );
    }

    /**
     * Kopierer grunnlaget til en ny behandling. Avklaringer som ble foreslått i den forrige behandlingen gjelder ikke
     * for den nye behandlingen, og kopieres derfor ikke med — kun de ferdigstilte avklaringene følger med videre.
     */
    public static BostedsGrunnlag nyttGrunnlagForBehandlingMedReferanserFra(Long behandlingId, BostedsGrunnlag grunnlag) {
        var forrigeHolder = grunnlag.avklaringer;
        var nyHolder = forrigeHolder != null && forrigeHolder.harForeslåtteAvklaringer()
            ? BostedsAvklaringHolder.lagKopiUtenForeslåtte(forrigeHolder)
            : forrigeHolder;
        return new BostedsGrunnlag(
            behandlingId,
            grunnlag.getOppgittFraSøknad(),
            nyHolder
        );
    }
}
