package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.opptjening;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.HarEndretInntektsmeldingVurderer;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingRelevantForVilkårsrevurdering;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.EndringPåForlengelseInput;
import no.nav.k9.sak.perioder.EndringPåForlengelsePeriodeVurderer;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.PSBEndringPåForlengelseInput;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@VilkårTypeRef(VilkårType.OPPTJENINGSVILKÅRET)
@ApplicationScoped
public class PleiepengerOpptjeningEndringPåForlengelsePeriodeVurderer implements EndringPåForlengelsePeriodeVurderer {

    private static final Set<BehandlingÅrsakType> RELEVANTE_ÅRSAKER = Set.of(
        BehandlingÅrsakType.RE_OPPLYSNINGER_OM_OPPTJENING);

    private ProsessTriggereRepository prosessTriggereRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private HarEndretInntektsmeldingVurderer harEndretInntektsmeldingVurderer;
    private boolean skalVurdereIM;


    PleiepengerOpptjeningEndringPåForlengelsePeriodeVurderer() {
    }

    @Inject
    public PleiepengerOpptjeningEndringPåForlengelsePeriodeVurderer(ProsessTriggereRepository prosessTriggereRepository,
                                                                    BehandlingRepository behandlingRepository,
                                                                    MottatteDokumentRepository mottatteDokumentRepository,
                                                                    HarRelvantInntektsmeldingendringForForlengelseIOpptjening harRelevantEndringIInntektsmelding,
                                                                    @Any Instance<InntektsmeldingRelevantForVilkårsrevurdering> inntektsmeldingRelevantForVilkårsrevurdering,
                                                                    @KonfigVerdi(value = "FORLENGELSE_IM_OPPTJENING_FILTER", defaultVerdi = "false") boolean skalVurdereIM) {
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.skalVurdereIM = skalVurdereIM;
        this.harEndretInntektsmeldingVurderer = new HarEndretInntektsmeldingVurderer(behandlingRepository, getInntektsmeldingFilter(inntektsmeldingRelevantForVilkårsrevurdering), harRelevantEndringIInntektsmelding);
    }

    @Override
    public boolean harPeriodeEndring(EndringPåForlengelseInput input, DatoIntervallEntitet periode) {
        if (skalVurdereIM) {
            var inntektsmeldinger = ((PSBEndringPåForlengelseInput) input).getSakInntektsmeldinger();
            var mottatteInntektsmeldinger = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(input.getBehandlingReferanse().getFagsakId())
                .stream()
                .filter(it -> Objects.equals(Brevkode.INNTEKTSMELDING, it.getType()))
                .toList();
            if (harEndretInntektsmeldingVurderer.harEndringPåInntektsmeldingerTilBrukForPerioden(
                input.getBehandlingReferanse(),
                periode, inntektsmeldinger,
                mottatteInntektsmeldinger
            )) {
                return true;
            }

        }
        var prosessTriggereOpt = prosessTriggereRepository.hentGrunnlag(input.getBehandlingReferanse().getBehandlingId());

        if (prosessTriggereOpt.isPresent()) {
            var aktuelleTriggere = prosessTriggereOpt.get()
                .getTriggere()
                .stream()
                .filter(it -> it.getPeriode().overlapper(periode))
                .filter(it -> RELEVANTE_ÅRSAKER.contains(it.getÅrsak()))
                .toList();

            return !aktuelleTriggere.isEmpty();
        }
        return false;
    }

    private static HarEndretInntektsmeldingVurderer.InntektsmeldingFilter getInntektsmeldingFilter(Instance<InntektsmeldingRelevantForVilkårsrevurdering> inntektsmeldingRelevantForBeregningVilkårsvurdering) {
        return (BehandlingReferanse referanse, Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode) ->
            InntektsmeldingRelevantForVilkårsrevurdering.finnTjeneste(inntektsmeldingRelevantForBeregningVilkårsvurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, referanse.getFagsakYtelseType()).begrensInntektsmeldinger(referanse, sakInntektsmeldinger, vilkårsPeriode);
    }

}
