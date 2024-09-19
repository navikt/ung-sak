package no.nav.k9.sak.domene.behandling.steg.kompletthet;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.INNHENT_INNTEKTSMELDING;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.ArbeidsgiverPortalenTjeneste;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
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

    InnhentInntektsmeldingSteg() {
        // for CDI proxy
    }

    @Inject
    public InnhentInntektsmeldingSteg(BehandlingRepository behandlingRepository,
                                      KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste,
                                      ArbeidsgiverPortalenTjeneste arbeidsgiverPortalenTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
        this.arbeidsgiverPortalenTjeneste = arbeidsgiverPortalenTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        Map<DatoIntervallEntitet, List<ManglendeVedlegg>> manglendeVedleggPerPeriode = kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraGrunnlag(ref);
        var stp = manglendeVedleggPerPeriode.keySet().stream().map(periode -> periode.getFomDato()).collect(Collectors.toList());

        arbeidsgiverPortalenTjeneste.sendInntektsmeldingForespørsel();
    }
}
