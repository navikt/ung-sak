package no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.arbeidsforhold.AktørYtelseEndring;
import no.nav.k9.sak.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.registerinnhenting.StartpunktUtleder;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
@FagsakYtelseTypeRef
@GrunnlagRef("InntektArbeidYtelseGrunnlag")
class StartpunktUtlederInntektArbeidYtelse implements StartpunktUtleder {

    private String klassenavn = this.getClass().getSimpleName();
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private StartpunktUtlederInntektsmelding startpunktUtlederInntektsmelding;
    private StartpunktUtlederInntektsmeldinger startpunktUtlederInntektsmeldinger;
    private VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    public StartpunktUtlederInntektArbeidYtelse() {
        // For CDI
    }

    @Inject
    StartpunktUtlederInntektArbeidYtelse(InntektArbeidYtelseTjeneste iayTjeneste, // NOSONAR - ingen enkel måte å unngå mange parametere her
                                         BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                         BehandlingRepositoryProvider repositoryProvider,
                                         StartpunktUtlederInntektsmelding startpunktUtlederInntektsmelding,
                                         StartpunktUtlederInntektsmeldinger startpunktUtlederInntektsmeldinger,
                                         VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.startpunktUtlederInntektsmelding = startpunktUtlederInntektsmelding;
        this.startpunktUtlederInntektsmeldinger = startpunktUtlederInntektsmeldinger;
        this.vurderArbeidsforholdTjeneste = vurderArbeidsforholdTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return hentAlleStartpunktForInntektArbeidYtelse(ref, (UUID) grunnlagId1, (UUID) grunnlagId2).stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private List<StartpunktType> hentAlleStartpunktForInntektArbeidYtelse(BehandlingReferanse ref,
                                                                          UUID grunnlagId1, UUID grunnlagId2) {
        List<StartpunktType> startpunkter = new ArrayList<>();
        InntektArbeidYtelseGrunnlag grunnlag1 = iayTjeneste.hentGrunnlagForGrunnlagId(ref.getBehandlingId(), grunnlagId1);
        InntektArbeidYtelseGrunnlag grunnlag2 = iayTjeneste.hentGrunnlagForGrunnlagId(ref.getBehandlingId(), grunnlagId2);

        LocalDate skjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();
        boolean skalTaStillingTilEndringerIArbeidsforhold = skalTaStillingTilEndringerIArbeidsforhold(ref);

        InntektArbeidYtelseGrunnlag iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId()); // TODO burde ikke være nødvendig (bør velge grunnlagId1, grunnlagId2)
        SakInntektsmeldinger sakInntektsmeldinger = skalTaStillingTilEndringerIArbeidsforhold ? iayTjeneste.hentInntektsmeldinger(ref.getSaksnummer()) : null /* ikke hent opp */;

        boolean erPåkrevdManuelleAvklaringer = !vurderArbeidsforholdTjeneste.vurder(ref, iayGrunnlag, sakInntektsmeldinger, skalTaStillingTilEndringerIArbeidsforhold).isEmpty();

        IAYGrunnlagDiff iayGrunnlagDiff = new IAYGrunnlagDiff(grunnlag1, grunnlag2);
        boolean erAktørArbeidEndretForSøker = iayGrunnlagDiff.erEndringPåAktørArbeidForAktør(skjæringstidspunkt, ref.getAktørId());
        boolean erAktørInntektEndretForSøker = iayGrunnlagDiff.erEndringPåAktørInntektForAktør(skjæringstidspunkt, ref.getAktørId());
        var startpunktType = startpunktUtlederInntektsmeldinger.utledStartpunkt(ref, grunnlag1);
        boolean erInntektsmeldingEndret = erInntektsmeldingEndret(ref, iayGrunnlagDiff, startpunktType);

        Saksnummer saksnummer = ref.getSaksnummer();
        AktørYtelseEndring aktørYtelseEndringForSøker = iayGrunnlagDiff.endringPåAktørYtelseForAktør(saksnummer, skjæringstidspunkt, ref.getAktørId());

        if (erPåkrevdManuelleAvklaringer) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.KONTROLLER_ARBEIDSFORHOLD, "manuell vurdering av arbeidsforhold");
        } else {
            ryddOppAksjonspunktHvisEksisterer(ref);
        }
        if (erAktørArbeidEndretForSøker) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.OPPTJENING, "aktørarbeid");
        }
        if (aktørYtelseEndringForSøker.erEksklusiveYtelserEndret()) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.KONTROLLER_ARBEIDSFORHOLD, "aktør ytelse");
        }
        if (aktørYtelseEndringForSøker.erAndreYtelserEndret()) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.OPPTJENING, "aktør ytelse andre tema");
        }
        if (erAktørInntektEndretForSøker) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.OPPTJENING, "aktør inntekt");
        }
        if (erInntektsmeldingEndret) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, startpunktType, "inntektsmelding");
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, startpunktUtlederInntektsmelding.utledStartpunkt(ref, grunnlag1, grunnlag2), "inntektsmelding");
        }

        return startpunkter;
    }

    private boolean erInntektsmeldingEndret(BehandlingReferanse ref, IAYGrunnlagDiff iayGrunnlagDiff, StartpunktType startpunktType) {
        return startpunktUtlederInntektsmeldinger.inntektsmeldingErSøknad(ref) ? !StartpunktType.UDEFINERT.equals(startpunktType) : iayGrunnlagDiff.erEndringPåInntektsmelding();
    }

    private boolean skalTaStillingTilEndringerIArbeidsforhold(BehandlingReferanse behandlingReferanse) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId());
        return !Objects.equals(behandlingReferanse.getBehandlingType(), BehandlingType.FØRSTEGANGSSØKNAD)
            || behandling.harSattStartpunkt();
    }

    /*
    Kontroller arbeidsforhold skal ikke lenger være aktiv hvis tilstanden i saken ikke tilsier det
    Setter dermed aksjonspunktet til utført hvis det står til opprettet.
     */
    private void ryddOppAksjonspunktHvisEksisterer(BehandlingReferanse behandlingReferanse) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingReferanse.getId());
        List<Aksjonspunkt> aksjonspunkter = behandling.getAksjonspunkter().stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD)
                || ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.AUTO_VENT_INNTEKTSMELDING_MED_UGYLDIG_ARBEIDSFORHOLDID))
            .filter(Aksjonspunkt::erÅpentAksjonspunkt)
            .collect(Collectors.toList());

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingskontrollTjeneste.lagreAksjonspunkterUtført(kontekst, behandling.getAktivtBehandlingSteg(), aksjonspunkter);
    }

    private void leggTilStartpunkt(List<StartpunktType> startpunkter, UUID grunnlagId1, UUID grunnlagId2, StartpunktType startpunkt, String endringLoggtekst) {
        startpunkter.add(startpunkt);
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(klassenavn, startpunkt, endringLoggtekst, grunnlagId1, grunnlagId2);
    }

}
