package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk.PSBKompletthetsjekker;

@Dependent
public class PSBKompletthetSjekkerTjeneste {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private PSBKompletthetsjekker kompletthetsjekker;

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


    public KompletthetsAksjon utledHandlinger(BehandlingReferanse ref) {
        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true);

        if (perioderTilVurdering.isEmpty()) {
            return KompletthetsAksjon.fortsett();
        }

        var kompletthetsVurderinger = kompletthetsjekker.utledAlleManglendeVedleggForPerioder(ref);
        var relevanteKompletthetsvurderinger = kompletthetsVurderinger.entrySet()
            .stream()
            .filter(it -> perioderTilVurdering.contains(it.getKey()))
            .collect(Collectors.toList());

        if (relevanteKompletthetsvurderinger.isEmpty()) {
            return KompletthetsAksjon.fortsett();
        }

        var erKomplett = relevanteKompletthetsvurderinger.stream()
            .allMatch(it -> it.getValue().isEmpty());

        if (erKomplett) {
            return KompletthetsAksjon.fortsett();
        }

        var grunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(ref.getBehandlingId());

        if (grunnlag.isPresent() && !grunnlag.get().getKompletthetPerioder().isEmpty()) {
            var kompletthetPerioder = grunnlag.get().getKompletthetPerioder();
            // Sjekk mot saksbehandler avklaringer
            var kanFortsette = relevanteKompletthetsvurderinger.stream()
                .filter(it -> !it.getValue().isEmpty())
                .filter(it -> kompletthetPerioder.stream().noneMatch(at -> Objects.equals(at.getSkjæringstidspunkt(), it.getKey().getFomDato()))
                    || kompletthetPerioder.stream().anyMatch(at -> Objects.equals(at.getSkjæringstidspunkt(), it.getKey().getFomDato())
                    && Set.of(Vurdering.UDEFINERT, Vurdering.MANGLENDE_GRUNNLAG).contains(at.getVurdering())))
                .allMatch(it -> it.getValue().isEmpty());

            if (kanFortsette) {
                return KompletthetsAksjon.fortsett();
            }
        }

        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        // Automatisk etterlys hvis ikke sendt ut før
        var etterlysImAutopunkt = behandling.getAksjonspunktFor(AksjonspunktKodeDefinisjon.ETTERLYS_IM_FOR_BEREGNING_KODE);
        var eksisterendeFrist = etterlysImAutopunkt.map(Aksjonspunkt::getFristTid).orElse(null);

        if (etterlysImAutopunkt.isEmpty() || LocalDateTime.now().isBefore(eksisterendeFrist)) {
            var fristTid = regnUtFrist(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_FOR_BEREGNING, eksisterendeFrist);
            return KompletthetsAksjon.automatiskEtterlysning(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_FOR_BEREGNING, fristTid, etterlysImAutopunkt.isEmpty());
        }

        // Be Saksbehandler avklar fortsettelse eller varsle avslag
        var avklarKompletthetAvklaring1 = behandling.getAksjonspunktFor(AksjonspunktKodeDefinisjon.AVKLAR_KOMPLETT_NOK_FOR_BEREGNING_KODE);
        if (avklarKompletthetAvklaring1.isEmpty() || avklarKompletthetAvklaring1.get().erÅpentAksjonspunkt()) {
            return KompletthetsAksjon.manuellAvklaring(AksjonspunktDefinisjon.AVKLAR_KOMPLETT_NOK_FOR_BEREGNING);
        }

        // Varsle avslag
        var etterlysImMedVarselAutopunkt = behandling.getAksjonspunktFor(AksjonspunktKodeDefinisjon.ETTERLYS_IM_VARSLE_AVSLAG_FOR_BEREGNING_KODE);
        eksisterendeFrist = etterlysImAutopunkt.map(Aksjonspunkt::getFristTid).orElse(null);
        if (etterlysImMedVarselAutopunkt.isEmpty() || LocalDateTime.now().isBefore(eksisterendeFrist)) {
            var fristTid = regnUtFrist(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_VARSLE_AVSLAG_FOR_BEREGNING, eksisterendeFrist);
            return KompletthetsAksjon.automatiskEtterlysning(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_VARSLE_AVSLAG_FOR_BEREGNING, fristTid, etterlysImMedVarselAutopunkt.isEmpty());
        }

        // Manuell avklaring
        return KompletthetsAksjon.manuellAvklaring(AksjonspunktDefinisjon.ENDELIG_AVKLAR_KOMPLETT_NOK_FOR_BEREGNING);
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
