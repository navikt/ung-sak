package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet.Kravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleieperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDødGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynForPleietrengende;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

public class InputParametere {

    private Behandling behandling;
    private PersonopplysningerAggregat personopplysningerAggregat;
    private Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> vurderteSøknadsperioder;
    private NavigableSet<DatoIntervallEntitet> perioderTilVurdering;
    private Vilkårene vilkårene;
    private Set<VilkårType> definerendeVilkårtyper;
    private UttaksPerioderGrunnlag uttaksGrunnlag;
    private List<EtablertPleieperiode> pleiebehov;
    private Set<Saksnummer> relaterteSaker;
    private NavigableSet<DatoIntervallEntitet> utvidetRevurderingPerioder;
    private NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles;
    private List<EtablertTilsynPeriode> etablertTilsynPerioder;
    private LocalDateTimeline<List<Kravprioritet>> kravprioritet;
    private OpptjeningResultat opptjeningResultat;
    private RettPleiepengerVedDødGrunnlag rettPleiepengerVedDødGrunnlag;
    private InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag;
    private UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende;
    private Set<PerioderFraSøknad> perioderFraSøknad;

    public InputParametere() {
    }

    public InputParametere medBehandling(Behandling behandling) {
        this.behandling = Objects.requireNonNull(behandling);
        return this;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public InputParametere medPersonopplysninger(PersonopplysningerAggregat personopplysningerAggregat) {
        this.personopplysningerAggregat = Objects.requireNonNull(personopplysningerAggregat);
        return this;
    }

    public PersonopplysningerAggregat getPersonopplysningerAggregat() {
        return personopplysningerAggregat;
    }

    public InputParametere medVurderteSøknadsperioder(Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> vurderteSøknadsperioder) {
        this.vurderteSøknadsperioder = Objects.requireNonNull(vurderteSøknadsperioder);
        return this;
    }

    public Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> getVurderteSøknadsperioder() {
        return vurderteSøknadsperioder;
    }

    public InputParametere medPerioderTilVurdering(NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        this.perioderTilVurdering = Objects.requireNonNull(perioderTilVurdering);
        return this;
    }

    public NavigableSet<DatoIntervallEntitet> getPerioderTilVurdering() {
        return perioderTilVurdering;
    }

    public InputParametere medVilkårene(Vilkårene vilkårene) {
        this.vilkårene = Objects.requireNonNull(vilkårene);
        return this;
    }

    public Vilkårene getVilkårene() {
        return vilkårene;
    }

    public InputParametere medDefinerendeVilkår(Set<VilkårType> definerendeVilkårtyper) {
        this.definerendeVilkårtyper = Objects.requireNonNull(definerendeVilkårtyper);
        return this;
    }

    public Set<VilkårType> getDefinerendeVilkårtyper() {
        return definerendeVilkårtyper;
    }

    public InputParametere medUttaksGrunnlag(UttaksPerioderGrunnlag uttaksPerioderGrunnlag) {
        this.uttaksGrunnlag = Objects.requireNonNull(uttaksPerioderGrunnlag);
        return this;
    }

    public UttaksPerioderGrunnlag getUttaksGrunnlag() {
        return uttaksGrunnlag;
    }

    public InputParametere medPerioderFraSøknad(Set<PerioderFraSøknad> perioderFraSøknad) {
        this.perioderFraSøknad = Objects.requireNonNull(perioderFraSøknad);
        return this;
    }

    public Set<PerioderFraSøknad> getPerioderFraSøknad() {
        return perioderFraSøknad;
    }

    public InputParametere medPleiebehov(List<EtablertPleieperiode> pleiebehov) {
        this.pleiebehov = Objects.requireNonNull(pleiebehov);
        return this;
    }

    public InputParametere medRettPleiepengerVedDødGrunnlag(RettPleiepengerVedDødGrunnlag rettPleiepengerVedDødGrunnlag) {
        this.rettPleiepengerVedDødGrunnlag = rettPleiepengerVedDødGrunnlag;
        return this;
    }

    public List<EtablertPleieperiode> getPleiebehov() {
        return pleiebehov;
    }

    public InputParametere medRelaterteSaker(Set<Saksnummer> relaterteSaker) {
        this.relaterteSaker = Objects.requireNonNull(relaterteSaker);
        return this;
    }

    public Set<Saksnummer> getRelaterteSaker() {
        return relaterteSaker;
    }

    public InputParametere medUtvidetPerioderRevurdering(NavigableSet<DatoIntervallEntitet> utvidetRevurderingPerioder) {
        this.utvidetRevurderingPerioder = utvidetRevurderingPerioder;
        return this;
    }

    public InputParametere medPerioderSomSkalTilbakestilles(NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles) {
        this.perioderSomSkalTilbakestilles = perioderSomSkalTilbakestilles;
        return this;
    }

    public NavigableSet<DatoIntervallEntitet> getPerioderSomSkalTilbakestilles() {
        return perioderSomSkalTilbakestilles;
    }

    public InputParametere medEtablertTilsynPerioder(List<EtablertTilsynPeriode> utledetEtablertTilsyn) {
        this.etablertTilsynPerioder = utledetEtablertTilsyn;
        return this;
    }

    public List<EtablertTilsynPeriode> getEtablertTilsynPerioder() {
        return etablertTilsynPerioder;
    }

    public NavigableSet<DatoIntervallEntitet> getUtvidetRevurderingPerioder() {
        return utvidetRevurderingPerioder;
    }

    public InputParametere medKravprioritet(LocalDateTimeline<List<Kravprioritet>> kravprioritet) {
        this.kravprioritet = kravprioritet;
        return this;
    }

    public LocalDateTimeline<List<Kravprioritet>> getKravprioritet() {
        return kravprioritet;
    }

    public Optional<RettPleiepengerVedDødGrunnlag> getRettPleiepengerVedDødGrunnlag() {
        return Optional.ofNullable(rettPleiepengerVedDødGrunnlag);
    }

    public InputParametere medOpptjeningsresultat(OpptjeningResultat opptjeningResultat) {
        this.opptjeningResultat = opptjeningResultat;
        return this;
    }

    public Optional<OpptjeningResultat> getOpptjeningResultat() {
        return Optional.ofNullable(opptjeningResultat);
    }

    public InputParametere medIAYGrunnlag(InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        this.inntektArbeidYtelseGrunnlag = inntektArbeidYtelseGrunnlag;
        return this;
    }

    public InntektArbeidYtelseGrunnlag getInntektArbeidYtelseGrunnlag() {
        return inntektArbeidYtelseGrunnlag;
    }

    public InputParametere medUnntakEtablertTilsynForPleietrengende(UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende) {
        this.unntakEtablertTilsynForPleietrengende = unntakEtablertTilsynForPleietrengende;
        return this;
    }

    public Optional<UnntakEtablertTilsynForPleietrengende> getUnntakEtablertTilsynForPleietrengende() {
        return Optional.ofNullable(unntakEtablertTilsynForPleietrengende);
    }
}
