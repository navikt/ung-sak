package no.nav.k9.sak.domene.behandling.steg.kompletthet;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.INNHENT_INNTEKTSMELDING;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.ArbeidsgiverPortalenTjeneste;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.etterlysning.BestiltEtterlysning;
import no.nav.k9.sak.behandlingslager.behandling.etterlysning.BestiltEtterlysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.KompletthetForBeregningTjeneste;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@BehandlingStegRef(value = INNHENT_INNTEKTSMELDING)
@BehandlingTypeRef
@ApplicationScoped
public class InnhentInntektsmeldingSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;
    private ArbeidsgiverPortalenTjeneste arbeidsgiverPortalenTjeneste;
    private BestiltEtterlysningRepository bestiltEtterlysningRepository;
    private boolean enableSteg;

    private static final Logger log = LoggerFactory.getLogger(InnhentInntektsmeldingSteg.class);

    InnhentInntektsmeldingSteg() {
        // for CDI proxy
    }

    @Inject
    public InnhentInntektsmeldingSteg(BehandlingRepository behandlingRepository,
                                      KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste,
                                      ArbeidsgiverPortalenTjeneste arbeidsgiverPortalenTjeneste,
                                      BestiltEtterlysningRepository bestiltEtterlysningRepository,
                                      @KonfigVerdi(value = "ENABLE_INNHENT_INNTEKTSMELDING_STEG", defaultVerdi = "false") boolean enableSteg) {
        this.behandlingRepository = behandlingRepository;
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
        this.arbeidsgiverPortalenTjeneste = arbeidsgiverPortalenTjeneste;
        this.bestiltEtterlysningRepository = bestiltEtterlysningRepository;
        this.enableSteg = enableSteg;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (!enableSteg) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        var manglendeVedleggPerPeriode = kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraGrunnlag(ref);
        var etterlysninger = lagEtterlysninger(manglendeVedleggPerPeriode, behandling);

        arbeidsgiverPortalenTjeneste.sendInntektsmeldingForespørsel(etterlysninger);
        log.info("Sendte forespørsel om inntektsmelding til arbeidsgiverportalen for følgende perioder: {}", etterlysninger.stream().map(BestiltEtterlysning::getPeriode).collect(Collectors.toList()));

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private Set<BestiltEtterlysning> lagEtterlysninger(Map<DatoIntervallEntitet, List<ManglendeVedlegg>> manglendeVedleggPerPeriode, Behandling behandling) {
        Set<BestiltEtterlysning> etterlysninger = new HashSet<>();

        var bestilteEtterlysninger = bestiltEtterlysningRepository.hentFor(behandling.getFagsakId());

        manglendeVedleggPerPeriode.forEach((periode, mangler) ->
            mangler.forEach(magel -> {
                var nyEtterLysning = new BestiltEtterlysning(behandling.getFagsakId(), behandling.getId(), periode, magel.getArbeidsgiver(), DokumentMalType.ETTERLYS_INNTEKTSMELDING_DOK.getKode());

                if (bestilteEtterlysninger.stream().noneMatch(tidligereEtterlysning -> tidligereEtterlysning.erTilsvarendeBestiltTidligere(nyEtterLysning))) {
                    etterlysninger.add(nyEtterLysning);
                }
            })
        );

        //TODO fix..
        //bestiltEtterlysningRepository.lagre(etterlysninger);

        return etterlysninger;
    }
}
