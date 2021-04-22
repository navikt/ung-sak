package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagSteg;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk.PSBKompletthetsjekker;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.MapArbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

@FagsakYtelseTypeRef
@BehandlingStegRef(kode = "KOMPLETT_FOR_BEREGNING")
@BehandlingTypeRef
@ApplicationScoped
public class VurderKompletthetForBeregningSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private PSBKompletthetsjekker kompletthetsjekker;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;

    protected VurderKompletthetForBeregningSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderKompletthetForBeregningSteg(BehandlingRepository behandlingRepository,
                                             BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                             UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                                             @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste,
                                             @BehandlingTypeRef @FagsakYtelseTypeRef("PSB") PSBKompletthetsjekker kompletthetsjekker) {

        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.kompletthetsjekker = kompletthetsjekker;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true);
        var uttakGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(ref.getBehandlingId()).orElseThrow();
        var vurderteSøknadsperioder = søknadsfristTjeneste.vurderSøknadsfrist(ref);

        if (perioderTilVurdering.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var kompletthetsVurderinger = kompletthetsjekker.utledAlleManglendeVedleggForPerioder(ref);

        var erKomplett = kompletthetsVurderinger.entrySet()
            .stream()
            .filter(it -> perioderTilVurdering.contains(it.getKey()))
            .filter(it -> !it.getValue().isEmpty())
            .allMatch(it -> erPeriodeKomplettBasertPåArbeid(uttakGrunnlag, vurderteSøknadsperioder, it));

        if (erKomplett) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        } else {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_KOMPLETT_NOK_FOR_BEREGNING));
        }
    }

    private boolean erPeriodeKomplettBasertPåArbeid(UttaksPerioderGrunnlag uttakGrunnlag,
                                                    Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> vurderteSøknadsperioder,
                                                    Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> manglendeVedleggForPeriode) {


        var perioderFraSøknadene = uttakGrunnlag.getOppgitteSøknadsperioder().getPerioderFraSøknadene();
        var kravDokumenter = vurderteSøknadsperioder.keySet();
        var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(manglendeVedleggForPeriode.getKey().getFomDato(), manglendeVedleggForPeriode.getKey().getTomDato(), true)));

        var arbeidIPeriode = new MapArbeid().map(kravDokumenter, perioderFraSøknadene, timeline, Set.of());
        var manglendeVedlegg = manglendeVedleggForPeriode.getValue();

        return manglendeVedlegg.stream()
            .noneMatch(at -> harFraværFraArbeidsgiverIPerioden(arbeidIPeriode, at));
    }

    private boolean harFraværFraArbeidsgiverIPerioden(List<Arbeid> arbeidIPeriode, ManglendeVedlegg at) {
        return arbeidIPeriode.stream()
            .anyMatch(it -> harFravær(it.getPerioder()) && utledIdentifikator(it).equals(at.getArbeidsgiver()));
    }

    private String utledIdentifikator(Arbeid it) {
        if (it.getArbeidsforhold().getOrganisasjonsnummer() != null) {
            return it.getArbeidsforhold().getOrganisasjonsnummer();
        } else if (it.getArbeidsforhold().getAktørId() != null) {
            return it.getArbeidsforhold().getAktørId();
        }
        throw new IllegalStateException("Fravær for arbeidsforhold mangler identifikator");
    }

    private boolean harFravær(Map<LukketPeriode, ArbeidsforholdPeriodeInfo> perioder) {
        return perioder.values().stream().anyMatch(it -> !it.getJobberNormalt().equals(it.getJobberNå()));
    }
}
