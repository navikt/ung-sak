package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.EndringPåForlengelseInput;
import no.nav.k9.sak.perioder.EndringPåForlengelsePeriodeVurderer;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.PSBEndringPåForlengelseInput;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@VilkårTypeRef(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
@ApplicationScoped
public class PleiepengerBeregningEndringPåForlengelsePeriodeVurderer implements EndringPåForlengelsePeriodeVurderer {

    private static final Set<BehandlingÅrsakType> RELEVANTE_ÅRSAKER = Set.of(
        BehandlingÅrsakType.RE_SATS_REGULERING,
        BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG,
        BehandlingÅrsakType.RE_KLAGE_MED_END_INNTEKT,
        BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG);

    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning;
    private ProsessTriggereRepository prosessTriggereRepository;
    private Instance<EndringPåForlengelsePeriodeVurderer> endringsVurderere;

    PleiepengerBeregningEndringPåForlengelsePeriodeVurderer() {
    }

    @Inject
    public PleiepengerBeregningEndringPåForlengelsePeriodeVurderer(BehandlingRepository behandlingRepository,
                                                                   MottatteDokumentRepository mottatteDokumentRepository,
                                                                   ProsessTriggereRepository prosessTriggereRepository,
                                                                   @Any Instance<EndringPåForlengelsePeriodeVurderer> endringsVurderere,
                                                                   @Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning) {
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.endringsVurderere = endringsVurderere;
    }

    @Override
    public boolean harPeriodeEndring(EndringPåForlengelseInput input, DatoIntervallEntitet periode) {
        if (harMarkertPeriodeForReberegning(input, periode)) {
            return true;
        }

        if (harEndringPåInntektsmeldingerTilBrukForPerioden(input, periode)) {
            return true;
        }

        var vurderer = EndringPåForlengelsePeriodeVurderer.finnVurderer(endringsVurderere, VilkårType.OPPTJENINGSVILKÅRET, FagsakYtelseType.PSB);

        return vurderer.harPeriodeEndring(input, periode);
    }

    private boolean harEndringPåInntektsmeldingerTilBrukForPerioden(EndringPåForlengelseInput input, DatoIntervallEntitet periode) {
        var referanse = input.getBehandlingReferanse();
        if (referanse.getOriginalBehandlingId().isEmpty()) {
            return false;
        }
        var originalBehandling = behandlingRepository.hentBehandling(referanse.getOriginalBehandlingId().orElseThrow());
        var inntektsmeldinger = ((PSBEndringPåForlengelseInput) input).getSakInntektsmeldinger();
        var mottatteInntektsmeldinger = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(referanse.getFagsakId())
            .stream()
            .filter(it -> Objects.equals(Brevkode.INNTEKTSMELDING, it.getType()))
            .toList();

        var inntektsmeldingerForrigeVedtak = inntektsmeldinger.stream()
            .filter(it -> finnEksaktMottattTidspunkt(it, mottatteInntektsmeldinger).isBefore(originalBehandling.getAvsluttetDato()))
            .toList();

        var relevanteInntektsmeldingerForrigeVedtak = utledRelevanteForPeriode(BehandlingReferanse.fra(originalBehandling), inntektsmeldingerForrigeVedtak, periode);
        var relevanteInntektsmeldinger = utledRelevanteForPeriode(referanse, inntektsmeldinger, periode);

        return harEndretSeg(relevanteInntektsmeldingerForrigeVedtak.stream()
            .map(Inntektsmelding::getJournalpostId)
            .collect(Collectors.toSet()), relevanteInntektsmeldinger.stream()
            .map(Inntektsmelding::getJournalpostId)
            .collect(Collectors.toSet()));
    }

    boolean harEndretSeg(Set<JournalpostId> forrigeVedtakJournalposter, Set<JournalpostId> denneBehandlingJournalposter) {

        var erLikeStore = forrigeVedtakJournalposter.size() == denneBehandlingJournalposter.size();

        var inneholderDeSamme = denneBehandlingJournalposter.containsAll(forrigeVedtakJournalposter);

        return !(erLikeStore && inneholderDeSamme);
    }

    private List<Inntektsmelding> utledRelevanteForPeriode(BehandlingReferanse referanse, Collection<Inntektsmelding> inntektsmeldinger, DatoIntervallEntitet periode) {
        var relevanteImTjeneste = InntektsmeldingerRelevantForBeregning.finnTjeneste(inntektsmeldingerRelevantForBeregning, referanse.getFagsakYtelseType());
        var inntektsmeldingBegrenset = relevanteImTjeneste.begrensSakInntektsmeldinger(referanse, inntektsmeldinger, periode);
        return relevanteImTjeneste.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldingBegrenset, periode);
    }

    private LocalDateTime finnEksaktMottattTidspunkt(Inntektsmelding inntektsmelding, List<MottattDokument> mottatteInntektsmeldinger) {
        return mottatteInntektsmeldinger.stream()
            .filter(it -> Objects.equals(it.getJournalpostId(), inntektsmelding.getJournalpostId()))
            .findAny()
            .map(MottattDokument::getMottattTidspunkt)
            .orElse(LocalDateTime.now());
    }

    private boolean harMarkertPeriodeForReberegning(EndringPåForlengelseInput input, DatoIntervallEntitet periode) {
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
