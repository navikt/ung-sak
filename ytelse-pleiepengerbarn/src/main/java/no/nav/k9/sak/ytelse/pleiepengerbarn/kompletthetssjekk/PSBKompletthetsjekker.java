package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.KompletthetForBeregningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;

@ApplicationScoped
@BehandlingTypeRef
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
public class PSBKompletthetsjekker implements Kompletthetsjekker {

    private KompletthetssjekkerSøknad kompletthetssjekkerSøknad;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;

    PSBKompletthetsjekker() {
        // CDI
    }

    @Inject
    public PSBKompletthetsjekker(KompletthetssjekkerSøknad kompletthetssjekkerSøknad,
                                 InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                 KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste) {
        this.kompletthetssjekkerSøknad = kompletthetssjekkerSøknad;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref) {
        Optional<LocalDateTime> forTidligFrist = kompletthetssjekkerSøknad.erSøknadMottattForTidlig(ref);
        return forTidligFrist.map(localDateTime -> KompletthetResultat.ikkeOppfylt(localDateTime, Venteårsak.FOR_TIDLIG_SOKNAD)).orElseGet(KompletthetResultat::oppfylt);
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref) {
        return KompletthetResultat.oppfylt();
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        return kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraGrunnlag(ref)
            .values()
            .stream()
            .allMatch(List::isEmpty);
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        return kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraGrunnlag(ref)
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    @Override
    public Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledAlleManglendeVedleggForPerioder(BehandlingReferanse ref) {
        return kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraGrunnlag(ref);
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
        return inntektsmeldingTjeneste
            .hentAlleInntektsmeldingerSomIkkeKommer(ref.getBehandlingId())
            .stream()
            .map(e -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, e.getArbeidsgiver(), true))
            .collect(Collectors.toList());
    }

    @Override
    public KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref) {
        return KompletthetResultat.oppfylt();
    }
}
