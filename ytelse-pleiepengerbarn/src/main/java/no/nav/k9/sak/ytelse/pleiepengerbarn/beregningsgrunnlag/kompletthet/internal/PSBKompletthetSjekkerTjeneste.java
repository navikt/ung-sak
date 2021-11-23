package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.internal;

import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.etterlysning.BestiltEtterlysning;
import no.nav.k9.sak.behandlingslager.behandling.etterlysning.BestiltEtterlysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.KompletthetsAksjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.TidligereEtterlysning;
import no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk.PSBKompletthetsjekker;

@Dependent
public class PSBKompletthetSjekkerTjeneste {

    private static final Logger log = LoggerFactory.getLogger(PSBKompletthetSjekkerTjeneste.class);

    private final BehandlingRepository behandlingRepository;
    private final BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private final BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private final BestiltEtterlysningRepository etterlysningRepository;
    private final PSBKompletthetsjekker kompletthetsjekker;
    private final KompletthetUtleder kompletthetUtleder = new KompletthetUtleder();
    private final EtterlysInntektsmeldingUtleder etterlysInntektsmeldingUtleder = new EtterlysInntektsmeldingUtleder();
    private final EtterlysInntektsmeldingOgVarsleOmAvslagUtleder etterlysInntektsmeldingOgVarsleOmAvslagUtleder = new EtterlysInntektsmeldingOgVarsleOmAvslagUtleder();

    @Inject
    public PSBKompletthetSjekkerTjeneste(BehandlingRepository behandlingRepository,
                                         BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                         BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                         BestiltEtterlysningRepository etterlysningRepository,
                                         @BehandlingTypeRef @FagsakYtelseTypeRef("PSB") PSBKompletthetsjekker kompletthetsjekker) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.etterlysningRepository = etterlysningRepository;
        this.kompletthetsjekker = kompletthetsjekker;
    }


    public KompletthetsAksjon utledTilstand(BehandlingReferanse ref, BehandlingskontrollKontekst kontekst) {
        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true);
        var kompletthetsVurderinger = kompletthetsjekker.utledAlleManglendeVedleggForPerioder(ref);

        var inputUtenVurderinger = new VurdererInput(perioderTilVurdering, kompletthetsVurderinger);
        var aksjon = kompletthetUtleder.utled(inputUtenVurderinger);

        var grunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(ref.getBehandlingId());
        if (aksjon.kanFortsette()) {
            deaktiverVurderingerSomIkkeErRelevantePgaNåKomplett(ref.getBehandlingId(), perioderTilVurdering, kompletthetsVurderinger, grunnlag);

            log.info("Behandlingen er komplett, kan fortsette.");
            return aksjon;
        }

        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());

        avslåOgAvkortRelevanteKompletthetsvurderinger(kontekst, perioderTilVurdering, grunnlag);

        var redusertPerioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true);
        var inputMedVurderinger = new VurdererInput(redusertPerioderTilVurdering, kompletthetsVurderinger, grunnlag.map(BeregningsgrunnlagPerioderGrunnlag::getKompletthetPerioder).orElse(List.of()), Set.of(Vurdering.KAN_FORTSETTE));
        aksjon = kompletthetUtleder.utled(inputMedVurderinger);

        if (aksjon.kanFortsette()) {
            log.info("Behandlingen er komplett etter vurdering fra saksbehandler, kan fortsette.");
            return aksjon;
        }

        log.info("Aksjonspunkter :: {}", behandling.getAksjonspunkter());
        // Automatisk etterlys hvis ikke sendt ut før
        var aksjonspunkter = behandling.getAksjonspunkter().stream()
            .filter(it -> it.getFristTid() != null)
            .collect(Collectors.toMap(Aksjonspunkt::getAksjonspunktDefinisjon, Aksjonspunkt::getFristTid));
        Map<DatoIntervallEntitet, List<TidligereEtterlysning>> bestilteBrev = hentTidligereEtterlysninger(ref);
        var etterlysInntektsmeldingInput = new EtterlysningInput(aksjonspunkter, kompletthetUtleder.utledRelevanteVurderinger(inputMedVurderinger), bestilteBrev);
        var etterlysAksjon = etterlysInntektsmeldingUtleder.utled(etterlysInntektsmeldingInput);

        if (etterlysAksjon.harAksjonspunktMedFrist() || etterlysAksjon.kanFortsette()) {
            log.info("Behandlingen er IKKE komplett etter vurdering fra saksbehandler, skal sende etterlysninger.");
            return etterlysAksjon;
        }

        // Be Saksbehandler avklar fortsettelse eller varsle avslag
        var avklarKompletthetAvklaring1 = behandling.getAksjonspunktFor(AksjonspunktKodeDefinisjon.AVKLAR_KOMPLETT_NOK_FOR_BEREGNING_KODE);
        if ((avklarKompletthetAvklaring1.isEmpty() || avklarKompletthetAvklaring1.get().erÅpentAksjonspunkt()) && etterlysAksjon.erUavklart()) {
            log.info("Behandlingen er IKKE komplett, ber om manuell avklaring.");
            return KompletthetsAksjon.manuellAvklaring(AksjonspunktDefinisjon.AVKLAR_KOMPLETT_NOK_FOR_BEREGNING);
        }

        var etterlysMedVarselaksjon = etterlysInntektsmeldingOgVarsleOmAvslagUtleder.utled(etterlysInntektsmeldingInput);
        // Varsle avslag
        if (etterlysMedVarselaksjon.harAksjonspunktMedFrist() || etterlysMedVarselaksjon.kanFortsette()) {
            log.info("Behandlingen er IKKE komplett, etterlyser med varsel om avslag.");
            return etterlysAksjon;
        }

        // Manuell avklaring
        log.info("Behandlingen er IKKE komplett, ber om manuell avklaring og avklaring om mulige avslag.");
        return KompletthetsAksjon.manuellAvklaring(AksjonspunktDefinisjon.ENDELIG_AVKLAR_KOMPLETT_NOK_FOR_BEREGNING);
    }

    private void deaktiverVurderingerSomIkkeErRelevantePgaNåKomplett(Long behandlingId, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Map<DatoIntervallEntitet, List<ManglendeVedlegg>> kompletthetsVurderinger, Optional<BeregningsgrunnlagPerioderGrunnlag> grunnlag) {
        if (grunnlag.isEmpty()) {
            return;
        }
        if (perioderTilVurdering.isEmpty()) {
            return;
        }
        if (kompletthetsVurderinger.keySet().isEmpty()) {
            return;
        }
        var grlag = grunnlag.get();

        var kompletthetPerioder = grlag.getKompletthetPerioder()
            .stream()
            .filter(it -> perioderTilVurdering.stream().anyMatch(at -> Objects.equals(at.getFomDato(), it.getSkjæringstidspunkt())))
            .filter(it -> !Objects.equals(it.getVurdering(), Vurdering.UDEFINERT))
            .filter(it -> kompletthetsVurderinger.entrySet().stream().anyMatch(at -> Objects.equals(at.getKey().getFomDato(), it.getSkjæringstidspunkt()) && at.getValue().isEmpty()))
            .collect(Collectors.toList());

        if (!kompletthetPerioder.isEmpty()) {
            beregningPerioderGrunnlagRepository.deaktiverKompletthetsPerioder(behandlingId, kompletthetPerioder);
        }
    }

    private Map<DatoIntervallEntitet, List<TidligereEtterlysning>> hentTidligereEtterlysninger(BehandlingReferanse ref) {
        Map<DatoIntervallEntitet, List<TidligereEtterlysning>> bestilteBrev = etterlysningRepository.hentFor(ref.getFagsakId())
            .stream()
            .filter(BestiltEtterlysning::getErArbeidsgiverMottaker)
            .collect(Collectors.groupingBy(BestiltEtterlysning::getPeriode,
                Collectors.flatMapping(it -> Stream.of(new TidligereEtterlysning(mapDokumentMalType(it.getDokumentMal()), it.getArbeidsgiver())), Collectors.toList())));
        return bestilteBrev;
    }

    private DokumentMalType mapDokumentMalType(String dokumentMal) {
        return DokumentMalType.fraKode(dokumentMal);
    }

    private void avslåOgAvkortRelevanteKompletthetsvurderinger(BehandlingskontrollKontekst kontekst, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Optional<BeregningsgrunnlagPerioderGrunnlag> grunnlag) {
        avslåPerioderSaksbehandlerHarMarkertMedManglendeGrunnlag(kontekst, perioderTilVurdering, grunnlag);
    }

    private void avslåPerioderSaksbehandlerHarMarkertMedManglendeGrunnlag(BehandlingskontrollKontekst kontekst, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Optional<BeregningsgrunnlagPerioderGrunnlag> grunnlag) {
        if (grunnlag.isPresent() && !grunnlag.get().getKompletthetPerioder().isEmpty()) {
            grunnlag.get().getKompletthetPerioder()
                .stream()
                .filter(it -> Vurdering.MANGLENDE_GRUNNLAG.equals(it.getVurdering()))
                .filter(it -> perioderTilVurdering.stream().anyMatch(at -> Objects.equals(at.getFomDato(), it.getSkjæringstidspunkt())))
                .forEach(periode -> avslå(periode, perioderTilVurdering, kontekst));
        }
    }

    private void avslå(KompletthetPeriode periode, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, BehandlingskontrollKontekst kontekst) {
        var relevantPeriode = perioderTilVurdering.stream()
            .filter(it -> it.inkluderer(periode.getSkjæringstidspunkt()))
            .collect(Collectors.toList());

        if (relevantPeriode.size() > 1) {
            throw new IllegalStateException("Fant flere vilkårsperioder(" + relevantPeriode.size() + ") relevant for " + periode);
        } else if (!relevantPeriode.isEmpty()) {
            beregningsgrunnlagVilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, relevantPeriode.get(0), periode.getBegrunnelse(), Avslagsårsak.MANGLENDE_INNTEKTSGRUNNLAG);
        }
    }
}
