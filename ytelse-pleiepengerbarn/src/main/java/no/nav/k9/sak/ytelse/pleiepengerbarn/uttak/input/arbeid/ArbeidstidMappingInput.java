package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

public class ArbeidstidMappingInput {

    private Set<KravDokument> kravDokumenter;
    private Set<PerioderFraSøknad> perioderFraSøknader;
    private LocalDateTimeline<Boolean> tidslinjeTilVurdering;
    private Vilkår vilkår;
    private OpptjeningResultat opptjeningResultat;
    private LocalDateTimeline<Boolean> inaktivTidslinje;

    public ArbeidstidMappingInput() {
    }

    public ArbeidstidMappingInput(Set<KravDokument> kravDokumenter,
                                  Set<PerioderFraSøknad> perioderFraSøknader,
                                  LocalDateTimeline<Boolean> tidslinjeTilVurdering,
                                  Vilkår vilkår,
                                  OpptjeningResultat opptjeningResultat) {
        this.kravDokumenter = kravDokumenter;
        this.perioderFraSøknader = perioderFraSøknader;
        this.tidslinjeTilVurdering = tidslinjeTilVurdering;
        this.vilkår = vilkår;
        this.opptjeningResultat = opptjeningResultat;
    }

    public ArbeidstidMappingInput medKravDokumenter(Set<KravDokument> kravDokumenter) {
        this.kravDokumenter = kravDokumenter;
        return this;
    }

    public ArbeidstidMappingInput medPerioderFraSøknader(Set<PerioderFraSøknad> perioderFraSøknader) {
        this.perioderFraSøknader = perioderFraSøknader;
        return this;
    }

    public ArbeidstidMappingInput medTidslinjeTilVurdering(LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        this.tidslinjeTilVurdering = tidslinjeTilVurdering;
        return this;
    }

    public ArbeidstidMappingInput medInaktivTidslinje(LocalDateTimeline<Boolean> inaktivTidslinje) {
        this.inaktivTidslinje = inaktivTidslinje;
        return this;
    }

    public ArbeidstidMappingInput medOpptjeningsResultat(OpptjeningResultat opptjeningResultat) {
        this.opptjeningResultat = opptjeningResultat;
        return this;
    }

    public ArbeidstidMappingInput medVilkår(Vilkår vilkår) {
        this.vilkår = vilkår;
        return this;
    }

    public Set<KravDokument> getKravDokumenter() {
        return kravDokumenter;
    }

    public Set<PerioderFraSøknad> getPerioderFraSøknader() {
        return perioderFraSøknader;
    }

    public LocalDateTimeline<Boolean> getTidslinjeTilVurdering() {
        return tidslinjeTilVurdering;
    }

    public Vilkår getVilkår() {
        return vilkår;
    }

    public OpptjeningResultat getOpptjeningResultat() {
        return opptjeningResultat;
    }

    public LocalDateTimeline<Boolean> getInaktivTidslinje() {
        return inaktivTidslinje != null ? inaktivTidslinje : LocalDateTimeline.EMPTY_TIMELINE;
    }
}
