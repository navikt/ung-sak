package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.internal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.KompletthetsAksjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk.PSBKompletthetsjekker;

@Dependent
public class PSBKompletthetSjekkerTjeneste {

    private static final Logger log = LoggerFactory.getLogger(PSBKompletthetSjekkerTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private PSBKompletthetsjekker kompletthetsjekker;
    private KompletthetUtleder kompletthetUtleder = new KompletthetUtleder();
    private EtterlysInntektsmeldingUtleder etterlysInntektsmeldingUtleder = new EtterlysInntektsmeldingUtleder();
    private EtterlysInntektsmeldingOgVarsleOmAvslagUtleder etterlysInntektsmeldingOgVarsleOmAvslagUtleder = new EtterlysInntektsmeldingOgVarsleOmAvslagUtleder();

    @Inject
    public PSBKompletthetSjekkerTjeneste(BehandlingRepository behandlingRepository,
                                         BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                         BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                         @BehandlingTypeRef @FagsakYtelseTypeRef("PSB") PSBKompletthetsjekker kompletthetsjekker) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.kompletthetsjekker = kompletthetsjekker;
    }


    public KompletthetsAksjon utledHandlinger(BehandlingReferanse ref, BehandlingskontrollKontekst kontekst) {
        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true);
        var kompletthetsVurderinger = kompletthetsjekker.utledAlleManglendeVedleggForPerioder(ref);

        var inputUtenVurderinger = new VurdererInput(perioderTilVurdering, kompletthetsVurderinger);
        var aksjon = kompletthetUtleder.utled(inputUtenVurderinger);

        if (aksjon.kanFortsette()) {
            return aksjon;
        }

        var grunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(ref.getBehandlingId());
        avslåOgAvkortRelevanteKompletthetsvurderinger(kontekst, perioderTilVurdering, grunnlag);

        var redusertPerioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true);
        var inputMedVurderinger = new VurdererInput(redusertPerioderTilVurdering, kompletthetsVurderinger, grunnlag.map(BeregningsgrunnlagPerioderGrunnlag::getKompletthetPerioder).orElse(List.of()), Set.of(Vurdering.KAN_FORTSETTE));
        aksjon = kompletthetUtleder.utled(inputMedVurderinger);

        if (aksjon.kanFortsette()) {
            return aksjon;
        }

        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        // Automatisk etterlys hvis ikke sendt ut før
        var aksjonspunkter = behandling.getAksjonspunkter().stream().collect(Collectors.toMap(a -> a.getAksjonspunktDefinisjon().getKode(), Aksjonspunkt::getFristTid));
        Map<DatoIntervallEntitet, List<BestiltEtterlysning>> bestilteBrev = Map.of(); // TODO
        var etterlysInntektsmeldingInput = new EtterlysningInput(aksjonspunkter, kompletthetUtleder.utledRelevanteVurderinger(inputMedVurderinger), bestilteBrev);
        var etterlysAksjon = etterlysInntektsmeldingUtleder.utled(etterlysInntektsmeldingInput);

        if (!etterlysAksjon.erUavklart() || etterlysAksjon.kanFortsette()) {
            return etterlysAksjon;
        }

        // Be Saksbehandler avklar fortsettelse eller varsle avslag
        var avklarKompletthetAvklaring1 = behandling.getAksjonspunktFor(AksjonspunktKodeDefinisjon.AVKLAR_KOMPLETT_NOK_FOR_BEREGNING_KODE);
        if ((avklarKompletthetAvklaring1.isEmpty() || avklarKompletthetAvklaring1.get().erÅpentAksjonspunkt()) && etterlysAksjon.erUavklart()) {
            return KompletthetsAksjon.manuellAvklaring(AksjonspunktDefinisjon.AVKLAR_KOMPLETT_NOK_FOR_BEREGNING);
        }

        var etterlysMedVarselaksjon = etterlysInntektsmeldingOgVarsleOmAvslagUtleder.utled(etterlysInntektsmeldingInput);
        // Varsle avslag
        if (!etterlysMedVarselaksjon.erUavklart() || etterlysMedVarselaksjon.kanFortsette()) {
            return etterlysAksjon;
        }

        // Manuell avklaring
        return KompletthetsAksjon.manuellAvklaring(AksjonspunktDefinisjon.ENDELIG_AVKLAR_KOMPLETT_NOK_FOR_BEREGNING);
    }

    private void avslåOgAvkortRelevanteKompletthetsvurderinger(BehandlingskontrollKontekst kontekst, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Optional<BeregningsgrunnlagPerioderGrunnlag> grunnlag) {
        avslåPerioderSaksbehandlerHarMarkertMedManglendeGrunnlag(kontekst, perioderTilVurdering, grunnlag);
    }

    private void avslåPerioderSaksbehandlerHarMarkertMedManglendeGrunnlag(BehandlingskontrollKontekst kontekst, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Optional<BeregningsgrunnlagPerioderGrunnlag> grunnlag) {
        if (grunnlag.isPresent() && !grunnlag.get().getKompletthetPerioder().isEmpty()) {
            grunnlag.get().getKompletthetPerioder()
                .stream()
                .filter(it -> Vurdering.MANGLENDE_GRUNNLAG.equals(it.getVurdering()))
                .filter(it -> perioderTilVurdering.stream().anyMatch(at -> at.inkluderer(it.getSkjæringstidspunkt())))
                .forEach(periode -> avslå(periode, perioderTilVurdering, kontekst));
        }
    }

    private List<Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>>> utledRelevanteVurdering(List<Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>>> relevanteKompletthetsvurderinger, Optional<BeregningsgrunnlagPerioderGrunnlag> grunnlag, Set<Vurdering> vurderingstyperDetSkalTasHensynTil) {
        var kompletthetPerioder = grunnlag.map(BeregningsgrunnlagPerioderGrunnlag::getKompletthetPerioder);

        return relevanteKompletthetsvurderinger.stream()
            .filter(it -> skalMedEtterVurdering(kompletthetPerioder, it, vurderingstyperDetSkalTasHensynTil))
            .collect(Collectors.toList());
    }

    private boolean skalMedEtterVurdering(Optional<List<KompletthetPeriode>> kompletthetPerioderOpt, Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> it, Set<Vurdering> vurderingstyperDetSkalTasHensynTil) {
        if (kompletthetPerioderOpt.isEmpty()) {
            return true;
        }
        var kompletthetPerioder = kompletthetPerioderOpt.get();

        if (kompletthetPerioder.isEmpty()) {
            return true;
        }

        return kompletthetPerioder.stream().noneMatch(at -> Objects.equals(at.getSkjæringstidspunkt(), it.getKey().getFomDato()))
            || kompletthetPerioder.stream().anyMatch(at -> Objects.equals(at.getSkjæringstidspunkt(), it.getKey().getFomDato())
            && !vurderingstyperDetSkalTasHensynTil.contains(at.getVurdering()));
    }

    private void avslå(KompletthetPeriode periode, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, BehandlingskontrollKontekst kontekst) {
        var relevantPeriode = perioderTilVurdering.stream()
            .filter(it -> it.inkluderer(periode.getSkjæringstidspunkt()))
            .collect(Collectors.toList());

        if (relevantPeriode.size() > 1) {
            throw new IllegalStateException("Fant flere vilkårsperioder(" + relevantPeriode.size() + ") relevant for " + periode);
        } else if (!relevantPeriode.isEmpty()) {
            beregningsgrunnlagVilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, relevantPeriode.get(0), Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG);
        }
    }

    private LocalDateTime regnUtFrist(AksjonspunktDefinisjon definisjon, LocalDateTime eksisterendeFrist) {
        if (eksisterendeFrist != null) {
            return eksisterendeFrist;
        }
        if (definisjon.getFristPeriod() == null) {
            throw new IllegalArgumentException("[Utvikler feil] Prøver å utlede frist basert på et aksjonspunkt uten fristperiode definert");
        }

        return LocalDateTime.now().plus(definisjon.getFristPeriod());
    }
}
