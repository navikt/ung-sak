package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Grunnlag som kobler en behandling til bostedsopplysningene oppgitt i søknaden.
 */
@Entity(name = "BostedSøknadsfaktaGrunnlag")
@Table(name = "GR_BOSTED_SOKNADSFAKTA")
public class BostedSøknadsfaktaGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_BOSTED_SOKNADSFAKTA")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "bostedsinformasjon_soeknad_holder_id", nullable = false)
    private BostedsinformasjonFraSøknadHolder oppgittFraSøknad;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public BostedSøknadsfaktaGrunnlag() {
    }

    BostedSøknadsfaktaGrunnlag(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        this.behandlingId = behandlingId;
    }

    private BostedSøknadsfaktaGrunnlag(Long behandlingId, BostedsinformasjonFraSøknadHolder oppgittFraSøknad) {
        this(behandlingId);
        this.oppgittFraSøknad = oppgittFraSøknad;
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

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
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

    public boolean isAktiv() {
        return aktiv;
    }

    void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedSøknadsfaktaGrunnlag that)) return false;
        return Objects.equals(oppgittFraSøknad, that.oppgittFraSøknad);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgittFraSøknad);
    }

    @Override
    public String toString() {
        return "BostedSøknadsfaktaGrunnlag{behandlingId=" + behandlingId
            + ", aktiv=" + aktiv + '}';
    }

    static BostedSøknadsfaktaGrunnlag nyttGrunnlagMedReferanserFra(BostedSøknadsfaktaGrunnlag grunnlag) {
        return new BostedSøknadsfaktaGrunnlag(grunnlag.getBehandlingId(), grunnlag.getOppgittFraSøknad());
    }

    static BostedSøknadsfaktaGrunnlag nyttGrunnlagForBehandlingMedReferanserFra(Long behandlingId, BostedSøknadsfaktaGrunnlag grunnlag) {
        return new BostedSøknadsfaktaGrunnlag(behandlingId, grunnlag.getOppgittFraSøknad());
    }
}
