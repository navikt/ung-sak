package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.opptjening;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.HarEndretInntektsmeldingVurderer;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
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
    private HarRelvantInntektsmeldingendringForForlengelseIOpptjening harRelevantEndringIInntektsmelding;
    private HarEndretInntektsmeldingVurderer harEndretInntektsmeldingVurderer;
    private boolean skalVurdereIM;


    PleiepengerOpptjeningEndringPåForlengelsePeriodeVurderer() {
    }

    @Inject
    public PleiepengerOpptjeningEndringPåForlengelsePeriodeVurderer(ProsessTriggereRepository prosessTriggereRepository,
                                                                    MottatteDokumentRepository mottatteDokumentRepository,
                                                                    HarRelvantInntektsmeldingendringForForlengelseIOpptjening harRelevantEndringIInntektsmelding,
                                                                    HarEndretInntektsmeldingVurderer harEndretInntektsmeldingVurderer,
                                                                    @KonfigVerdi(value = "FORLENGELSE_IM_OPPTJENING_FILTER", defaultVerdi = "false") boolean skalVurdereIM) {
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.harRelevantEndringIInntektsmelding = harRelevantEndringIInntektsmelding;
        this.harEndretInntektsmeldingVurderer = harEndretInntektsmeldingVurderer;
        this.skalVurdereIM = skalVurdereIM;
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
                VilkårType.OPPTJENINGSVILKÅRET,
                periode, inntektsmeldinger,
                mottatteInntektsmeldinger,
                harRelevantEndringIInntektsmelding
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
}
