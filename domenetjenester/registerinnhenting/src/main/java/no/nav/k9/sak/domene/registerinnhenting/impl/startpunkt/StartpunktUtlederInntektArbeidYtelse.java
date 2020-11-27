package no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.arbeidsforhold.AktørYtelseEndring;
import no.nav.k9.sak.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.registerinnhenting.StartpunktUtleder;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
@GrunnlagRef("InntektArbeidYtelseGrunnlag")
class StartpunktUtlederInntektArbeidYtelse implements StartpunktUtleder {

    private String klassenavn = this.getClass().getSimpleName();
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private StartpunktUtlederInntektsmeldinger startpunktUtlederInntektsmeldinger;
    private VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste;
    private BehandlingRepository behandlingRepository;

    public StartpunktUtlederInntektArbeidYtelse() {
        // For CDI
    }

    @Inject
    StartpunktUtlederInntektArbeidYtelse(InntektArbeidYtelseTjeneste iayTjeneste, // NOSONAR - ingen enkel måte å unngå mange parametere her
                                         BehandlingRepositoryProvider repositoryProvider,
                                         StartpunktUtlederInntektsmeldinger startpunktUtlederInntektsmeldinger,
                                         VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.startpunktUtlederInntektsmeldinger = startpunktUtlederInntektsmeldinger;
        this.vurderArbeidsforholdTjeneste = vurderArbeidsforholdTjeneste;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return hentAlleStartpunktForInntektArbeidYtelse(ref, (UUID) grunnlagId1, (UUID) grunnlagId2).stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private List<StartpunktType> hentAlleStartpunktForInntektArbeidYtelse(BehandlingReferanse ref, UUID grunnlagId1, UUID grunnlagId2) { // NOSONAR
        List<StartpunktType> startpunkter = new ArrayList<>();
        var grunnlag1 = iayTjeneste.hentGrunnlagForGrunnlagId(ref.getBehandlingId(), grunnlagId1);
        var grunnlag2 = iayTjeneste.hentGrunnlagForGrunnlagId(ref.getBehandlingId(), grunnlagId2);
        var diff = new IAYGrunnlagDiff(grunnlag1, grunnlag2);

        if (startpunktUtlederInntektsmeldinger.inntektsmeldingErSøknad(ref)) {
            var startpunktType = startpunktUtlederInntektsmeldinger.utledStartpunkt(ref, grunnlag2);
            boolean erInntektsmeldingEndret = !StartpunktType.UDEFINERT.equals(startpunktType);
            if (erInntektsmeldingEndret) {
                leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, startpunktType, "inntektsmelding");
            }
        } else {
            boolean erInntektsmeldingEndret = diff.erEndringPåInntektsmelding();
            if (erInntektsmeldingEndret) {
                leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.INIT_PERIODER, "inntektsmelding");
            }
        }

        if (!startpunkter.isEmpty()) {
            return startpunkter; // quick exit siden vi har allerede testet to viktigste startpunkt typer
        }

        Saksnummer saksnummer = ref.getSaksnummer();
        LocalDate skjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();
        AktørYtelseEndring aktørYtelseEndringForSøker = diff.endringPåAktørYtelseForAktør(saksnummer, skjæringstidspunkt, ref.getAktørId());
        if (harAksjonspunkt5080(ref)) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.KONTROLLER_ARBEIDSFORHOLD, "manuell vurdering av arbeidsforhold");
        } else if (aktørYtelseEndringForSøker.erEksklusiveYtelserEndret()) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.KONTROLLER_ARBEIDSFORHOLD, "aktør ytelse");
        } else if (erPåkrevdManuelleAvklaringer(ref)) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.KONTROLLER_ARBEIDSFORHOLD, "manuell vurdering av arbeidsforhold");
        }

        if (!startpunkter.isEmpty()) {
            return startpunkter; // quick exit siden vi har allerede testet to viktigste startpunkt typer
        }

        boolean erAktørArbeidEndretForSøker = diff.erEndringPåAktørArbeidForAktør(skjæringstidspunkt, ref.getAktørId());
        if (erAktørArbeidEndretForSøker) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.OPPTJENING, "aktørarbeid");
        } else if (aktørYtelseEndringForSøker.erAndreYtelserEndret()) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.OPPTJENING, "aktør ytelse andre tema");
        } else {
            boolean erAktørInntektEndretForSøker = diff.erEndringPåAktørInntektForAktør(skjæringstidspunkt, ref.getAktørId());
            if (erAktørInntektEndretForSøker) {
                leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.OPPTJENING, "aktør inntekt");
            }
        }

        return startpunkter;
    }

    private boolean erPåkrevdManuelleAvklaringer(BehandlingReferanse ref) {
        return !vurderArbeidsforholdTjeneste.vurder(ref).isEmpty();
    }

    private boolean harAksjonspunkt5080(BehandlingReferanse ref) {
        Behandling behandling = behandlingRepository.hentBehandling(ref.getId());

        return behandling.getAksjonspunkter()
            .stream()
            .filter(Aksjonspunkt::erÅpentAksjonspunkt)
            .anyMatch(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD));
    }

    private void leggTilStartpunkt(List<StartpunktType> startpunkter, UUID grunnlagId1, UUID grunnlagId2, StartpunktType startpunkt, String endringLoggtekst) {
        startpunkter.add(startpunkt);
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(klassenavn, startpunkt, endringLoggtekst, grunnlagId1, grunnlagId2);
    }

}
