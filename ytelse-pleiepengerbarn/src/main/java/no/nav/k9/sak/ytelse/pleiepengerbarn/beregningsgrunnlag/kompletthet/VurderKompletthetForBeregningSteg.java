package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.formidling.kontrakt.kodeverk.IdType;
import no.nav.k9.formidling.kontrakt.kodeverk.Mottaker;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.etterlysning.BestiltEtterlysning;
import no.nav.k9.sak.behandlingslager.behandling.etterlysning.BestiltEtterlysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagSteg;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.kontrakt.dokument.MottakerDto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.internal.PSBKompletthetSjekkerTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk.PSBKompletthetsjekker;

@FagsakYtelseTypeRef("PSB")
@BehandlingStegRef(kode = "KOMPLETT_FOR_BEREGNING")
@BehandlingTypeRef
@ApplicationScoped
public class VurderKompletthetForBeregningSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private PSBKompletthetSjekkerTjeneste kompletthetSjekkerTjeneste;
    private BestiltEtterlysningRepository bestiltEtterlysningRepository;
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private PSBKompletthetsjekker kompletthetsjekker;
    private boolean benyttNyFlyt = false;

    protected VurderKompletthetForBeregningSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderKompletthetForBeregningSteg(BehandlingRepository behandlingRepository,
                                             BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                             PSBKompletthetSjekkerTjeneste kompletthetSjekkerTjeneste,
                                             BestiltEtterlysningRepository bestiltEtterlysningRepository,
                                             DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
                                             @BehandlingTypeRef @FagsakYtelseTypeRef("PSB") PSBKompletthetsjekker kompletthetsjekker,
                                             @KonfigVerdi(value = "KOMPLETTHET_NY_FLYT", defaultVerdi = "false") Boolean benyttNyFlyt) {

        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.kompletthetSjekkerTjeneste = kompletthetSjekkerTjeneste;
        this.bestiltEtterlysningRepository = bestiltEtterlysningRepository;
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.kompletthetsjekker = kompletthetsjekker;
        this.benyttNyFlyt = benyttNyFlyt;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        if (benyttNyFlyt) {
            return nyKompletthetFlyt(ref, kontekst);
        } else {
            return legacykompletthetHåndtering(ref);
        }
    }

    private BehandleStegResultat nyKompletthetFlyt(BehandlingReferanse ref, BehandlingskontrollKontekst kontekst) {
        var kompletthetsAksjon = kompletthetSjekkerTjeneste.utledTilstand(ref, kontekst);

        if (kompletthetsAksjon.kanFortsette()) {
            avbrytAksjonspunktHvisTilstede(kontekst);

            return BehandleStegResultat.utførtUtenAksjonspunkter();
        } else if (kompletthetsAksjon.harFrist()) {
            var perioderMedManglendeVedlegg = kompletthetsAksjon.getPerioderMedMangler();
            if (!perioderMedManglendeVedlegg.isEmpty()) {
                var manglendeBestillinger = utledManglendeBestillinger(ref, perioderMedManglendeVedlegg, kompletthetsAksjon);

                bestiltEtterlysningRepository.lagre(manglendeBestillinger);
                var aktørerDetSkalEtterlysesFra = manglendeBestillinger.stream()
                    .map(BestiltEtterlysning::getArbeidsgiver)
                    .collect(Collectors.toSet());
                for (Arbeidsgiver arbeidsgiver : aktørerDetSkalEtterlysesFra) {
                    var mottaker = arbeidsgiver != null ? new Mottaker(arbeidsgiver.getIdentifikator(), arbeidsgiver.getErVirksomhet() ? IdType.ORGNR : IdType.AKTØRID) : new Mottaker(ref.getAktørId().getAktørId(), IdType.AKTØRID);
                    sendBrev(ref.getBehandlingId(), DokumentMalType.fraKode(kompletthetsAksjon.getDokumentMalType().getKode()), mottaker);
                }
            }

            var aksjonspunktResultat = AksjonspunktResultat.opprettForAksjonspunktMedFrist(kompletthetsAksjon.getAksjonspunktDefinisjon(),
                utledVenteÅrsak(kompletthetsAksjon),
                kompletthetsAksjon.getFrist());
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(aksjonspunktResultat));
        } else if (!kompletthetsAksjon.erUavklart()) {
            var aksjonspunktResultat = AksjonspunktResultat.opprettForAksjonspunkt(kompletthetsAksjon.getAksjonspunktDefinisjon());

            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(aksjonspunktResultat));
        } else {
            throw new IllegalStateException("Ugyldig kompletthetstilstand :: " + kompletthetsAksjon);
        }
    }

    private Set<BestiltEtterlysning> utledManglendeBestillinger(BehandlingReferanse ref, List<PeriodeMedMangler> arbeidsgiverDetSkalEtterlysesFra, KompletthetsAksjon aksjon) {
        Objects.requireNonNull(aksjon);
        var aksjonspunktDefinisjon = aksjon.getAksjonspunktDefinisjon();

        var bestilteEtterlysninger = bestiltEtterlysningRepository.hentFor(ref.getFagsakId());

        if (AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_FOR_BEREGNING.equals(aksjonspunktDefinisjon) ||
            AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_VARSLE_AVSLAG_FOR_BEREGNING.equals(aksjonspunktDefinisjon)) {
            var bestilt = new HashSet<BestiltEtterlysning>();
            var brukerBrev = arbeidsgiverDetSkalEtterlysesFra.stream()
                .map(PeriodeMedMangler::getPeriode)
                .map(it -> new BestiltEtterlysning(ref.getFagsakId(), ref.getBehandlingId(), it, null, aksjon.getDokumentMalType().getKode()))
                .filter(it -> bestilteEtterlysninger.stream().noneMatch(at -> at.erTilsvarendeFraSammeBehandling(it)))
                .collect(Collectors.toSet());

            var arbeidsgiverBrev = arbeidsgiverDetSkalEtterlysesFra.stream()
                .map(it -> it.getMangler().stream().map(mapTilBestilling(ref, aksjon, it)).collect(Collectors.toSet()))
                .flatMap(Collection::stream)
                .filter(it -> bestilteEtterlysninger.stream().noneMatch(at -> at.erTilsvarendeFraSammeBehandling(it)))
                .collect(Collectors.toSet());

            bestilt.addAll(brukerBrev);
            bestilt.addAll(arbeidsgiverBrev);

            return bestilt;
        } else {
            throw new IllegalArgumentException("Ukjent aksjonspunkt definisjon " + aksjonspunktDefinisjon);
        }

    }

    private Function<ManglendeVedlegg, BestiltEtterlysning> mapTilBestilling(BehandlingReferanse ref, KompletthetsAksjon aksjon, PeriodeMedMangler it) {
        return at -> new BestiltEtterlysning(ref.getFagsakId(), ref.getBehandlingId(), it.getPeriode(), at.getArbeidsgiver(), aksjon.getDokumentMalType().getKode());
    }

    private void avbrytAksjonspunktHvisTilstede(BehandlingskontrollKontekst kontekst) {
        var haddeAksjonspunktSomSkulleAvbrytes = false;
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.ENDELIG_AVKLAR_KOMPLETT_NOK_FOR_BEREGNING)) {
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.ENDELIG_AVKLAR_KOMPLETT_NOK_FOR_BEREGNING)
                .avbryt();
            haddeAksjonspunktSomSkulleAvbrytes = true;
        }
        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_KOMPLETT_NOK_FOR_BEREGNING)) {
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.AVKLAR_KOMPLETT_NOK_FOR_BEREGNING)
                .avbryt();
            haddeAksjonspunktSomSkulleAvbrytes = true;
        }
        if (haddeAksjonspunktSomSkulleAvbrytes) {
            behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        }
    }

    private Venteårsak utledVenteÅrsak(KompletthetsAksjon kompletthetsAksjon) {
        var ap = kompletthetsAksjon.getAksjonspunktDefinisjon();
        if (AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_FOR_BEREGNING.equals(ap)) {
            return Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKTSMELDINGER;
        }
        if (AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_VARSLE_AVSLAG_FOR_BEREGNING.equals(ap)) {
            return Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKTSMELDINGER;
        }
        throw new IllegalArgumentException("Ukjent autopunkt '" + ap + "'");
    }

    private BehandleStegResultat legacykompletthetHåndtering(BehandlingReferanse ref) {
        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true);

        if (perioderTilVurdering.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var kompletthetsVurderinger = kompletthetsjekker.utledAlleManglendeVedleggForPerioder(ref);
        var relevanteKompletthetsvurderinger = kompletthetsVurderinger.entrySet()
            .stream()
            .filter(it -> perioderTilVurdering.contains(it.getKey()))
            .collect(Collectors.toList());

        if (relevanteKompletthetsvurderinger.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var erKomplett = relevanteKompletthetsvurderinger.stream()
            .allMatch(it -> it.getValue().isEmpty());

        if (erKomplett) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        } else {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_KOMPLETT_NOK_FOR_BEREGNING));
        }
    }

    private void sendBrev(Long behandlingId, DokumentMalType dokumentMalType, Mottaker mottaker) {
        BestillBrevDto bestillBrevDto = new BestillBrevDto(behandlingId, dokumentMalType, new MottakerDto(mottaker.id, mottaker.type.toString()));
        dokumentBestillerApplikasjonTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.VEDTAKSLØSNINGEN);
    }

}
