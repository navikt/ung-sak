package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

public class ArbeidstidMappingInput {

    private Saksnummer saksnummer;
    private AktørId bruker;
    private Set<KravDokument> kravDokumenter;
    private Set<PerioderFraSøknad> perioderFraSøknader;
    private LocalDateTimeline<Boolean> tidslinjeTilVurdering;
    private Vilkår vilkår;
    private OpptjeningResultat opptjeningResultat;
    private Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> inaktivTidslinje;
    private Map<Saksnummer, Set<LocalDate>> sakerSomMåSpesialhåndteres = new HashMap<>();
    private InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag;
    private DatoIntervallEntitet utvidetPeriodeSomFølgeAvDødsfall;

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

    public ArbeidstidMappingInput medSaksnummer(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
        return this;
    }

    public ArbeidstidMappingInput medSakerSomMåSpesialHåndteres(Map<Saksnummer, Set<LocalDate>> sakerSomMåSpesialhåndteres) {
        this.sakerSomMåSpesialhåndteres = sakerSomMåSpesialhåndteres;
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

    public ArbeidstidMappingInput medInaktivTidslinje(Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> inaktivTidslinje) {
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

    public Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> getInaktivTidslinje() {
        return inaktivTidslinje != null ? inaktivTidslinje : Map.of();
    }

    public DatoIntervallEntitet getUtvidetPeriodeSomFølgeAvDødsfall() {
        return utvidetPeriodeSomFølgeAvDødsfall;
    }

    public ArbeidstidMappingInput medAutomatiskUtvidelseVedDødsfall(DatoIntervallEntitet utvidetPeriodeSomFølgeAvDødsfall) {
        this.utvidetPeriodeSomFølgeAvDødsfall = utvidetPeriodeSomFølgeAvDødsfall;
        return this;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public Map<Saksnummer, Set<LocalDate>> getSakerSomMåSpesialhåndteres() {
        return sakerSomMåSpesialhåndteres;
    }

    public ArbeidstidMappingInput medInntektArbeidYtelseGrunnlag(InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        this.inntektArbeidYtelseGrunnlag = inntektArbeidYtelseGrunnlag;
        return this;
    }

    public ArbeidstidMappingInput medBruker(AktørId bruker) {
        this.bruker = bruker;
        return this;
    }

    public InntektArbeidYtelseGrunnlag getInntektArbeidYtelseGrunnlag() {
        return inntektArbeidYtelseGrunnlag;
    }

    public AktørId getBruker() {
        return bruker;
    }
}
