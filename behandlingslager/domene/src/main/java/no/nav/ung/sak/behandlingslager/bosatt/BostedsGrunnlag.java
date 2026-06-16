package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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

    @JoinColumn(name = "bosatt_soeknad_grunnlag_id", nullable = false)
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private BostedsinformasjonFraSøknadHolder oppgittFraSøknad;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "foreslatt_holder_id", updatable = false)
    private BostedsAvklaringHolder foreslått;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "resultat_holder_id", updatable = false)
    private BostedsAvklaringHolder resultat;

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

    BostedsGrunnlag(Long behandlingId, BostedsinformasjonFraSøknadHolder oppgittFraSøknad, BostedsAvklaringHolder foreslått, BostedsAvklaringHolder resultat) {
        this.behandlingId = behandlingId;
        this.oppgittFraSøknad = oppgittFraSøknad;
        this.foreslått = foreslått;
        this.resultat = resultat;
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
    void setForeslåttAvklaring(List<BostedsPeriodeAvklaring> avklaringer) {
        var nyHolder = new BostedsAvklaringHolder(this.foreslått);
        nyHolder.leggTilEllerErstattPeriodeAvklaringer(avklaringer);

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

    public BostedsAvklaringHolder getForeslått() {
        return foreslått;
    }

    public BostedsAvklaringHolder getResultat() {
        return resultat;
    }

    public BostedsinformasjonFraSøknadHolder getOppgittFraSøknad() {
        return oppgittFraSøknad;
    }

    // Bygger en tidslinje for vurdert periode, som inneholder oppgitt fakta, og foreslått fakta der de overlapper
    public LocalDateTimeline<BostedsPeriodeAvklaring> hentOppgittOgForeslåttFaktaSomTidslinje(LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        Map<LocalDate, BostedsinformasjonFraSøknad> oppgittFraSøknadPerFom = (oppgittFraSøknad == null) ? Map.of() : oppgittFraSøknad.hentSomMap();

        var oppgittFraSøknadTidslinje = tidslinjeTilVurdering.map(periode -> {
            var oppgittForPeriode = oppgittFraSøknadPerFom.get(periode.getFom());
            if (oppgittForPeriode == null) {
                return Collections.emptyList();
            }

            return List.of(new LocalDateSegment<>(periode.getFom(), periode.getTom(),
                new BostedsPeriodeAvklaring(
                    DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()),
                    oppgittForPeriode.isErBosattITrondheim(),
                    null,
                    Kilde.SØKNAD
                )
            ));
        });

        if (foreslått == null) {
            return oppgittFraSøknadTidslinje;
        }
        return foreslått.hentSomTidslinje().crossJoin(oppgittFraSøknadTidslinje);
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
            Objects.equals(foreslått, that.foreslått) &&
            Objects.equals(resultat, that.resultat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(foreslått);
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
            grunnlag.getForeslått(),
            grunnlag.getResultat()
        );
    }

    public static BostedsGrunnlag nyttGrunnlagForBehandlingMedReferanserFra(Long behandlingId, BostedsGrunnlag grunnlag) {
        return new BostedsGrunnlag(
            behandlingId,
            grunnlag.getOppgittFraSøknad(),
            grunnlag.getForeslått(),
            grunnlag.getResultat()
        );
    }
}
