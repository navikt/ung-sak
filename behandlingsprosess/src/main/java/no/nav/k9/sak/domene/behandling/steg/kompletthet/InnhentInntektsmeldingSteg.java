package no.nav.k9.sak.domene.behandling.steg.kompletthet;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.INNHENT_INNTEKTSMELDING;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.ArbeidsgiverPortalenTjeneste;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.KompletthetForBeregningTjeneste;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.typer.Arbeidsgiver;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@BehandlingStegRef(value = INNHENT_INNTEKTSMELDING)
@BehandlingTypeRef
@ApplicationScoped
public class InnhentInntektsmeldingSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;
    private ArbeidsgiverPortalenTjeneste arbeidsgiverPortalenTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private boolean enableSteg;

    private static final Logger log = LoggerFactory.getLogger(InnhentInntektsmeldingSteg.class);

    InnhentInntektsmeldingSteg() {
        // for CDI proxy
    }

    @Inject
    public InnhentInntektsmeldingSteg(BehandlingRepository behandlingRepository,
                                      KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste,
                                      ArbeidsgiverPortalenTjeneste arbeidsgiverPortalenTjeneste,
                                      InntektArbeidYtelseTjeneste iayTjeneste,
                                      BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                      @KonfigVerdi(value = "ENABLE_INNHENT_INNTEKTSMELDING_STEG", defaultVerdi = "false") boolean enableSteg) {
        this.behandlingRepository = behandlingRepository;
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
        this.arbeidsgiverPortalenTjeneste = arbeidsgiverPortalenTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
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


        //TODO: Skal dette ligge i kompletthetsjekken i stedet?
        // Utled vilkårsperioder
        var vilkårsPerioder = beregningsgrunnlagVilkårTjeneste.utledPerioderForKompletthet(ref, false, false, true)
            .stream()
            .sorted(Comparator.comparing(DatoIntervallEntitet::getFomDato))
            .collect(Collectors.toCollection(TreeSet::new));

        var stp = vilkårsPerioder.stream().map(DatoIntervallEntitet::getFomDato).toList();

        var inntektsmeldinger = iayTjeneste.hentInntektsmeldingerKommetTomBehandling(ref.getSaksnummer(), ref.getBehandlingId());

        var ignorerteInntektsmeldinger = inntektsmeldinger.stream()
            .filter(im -> im.getStartDatoPermisjon().isPresent() && !stp.contains(im.getStartDatoPermisjon().get()))
            .map(Inntektsmelding::getJournalpostId).collect(Collectors.toSet());

        var manglendeVedleggPerPeriode = kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraGrunnlag(ref, ignorerteInntektsmeldinger);
        var forespørsler = mapTilForespørsler(manglendeVedleggPerPeriode);

        arbeidsgiverPortalenTjeneste.oppdaterInntektsmeldingforespørslerISak(forespørsler, behandling);
        log.info("Sendte forespørsel om inntektsmelding til arbeidsgiverportalen for følgende perioder: {}", forespørsler.keySet());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private static Map<DatoIntervallEntitet, List<Arbeidsgiver>> mapTilForespørsler(Map<DatoIntervallEntitet, List<ManglendeVedlegg>> manglendeVedleggPerPeriode) {
        return manglendeVedleggPerPeriode.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().stream().map(ManglendeVedlegg::getArbeidsgiver).collect(Collectors.toList())));
    }
}
