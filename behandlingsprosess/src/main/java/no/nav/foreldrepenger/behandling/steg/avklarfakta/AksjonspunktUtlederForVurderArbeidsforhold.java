package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class AksjonspunktUtlederForVurderArbeidsforhold implements AksjonspunktUtleder {
    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();

    private VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste;
    private ArbeidsforholdUtenRelevantOppgittOpptjening arbeidsforholdUtenRelevantOppgittOpptjeningTjeneste;

    private BehandlingRepository behandlingRepository;

    private InntektArbeidYtelseTjeneste iayTjeneste;

    AksjonspunktUtlederForVurderArbeidsforhold() {
    }

    @Inject
    public AksjonspunktUtlederForVurderArbeidsforhold(BehandlingRepository behandlingRepository,
                                                      InntektArbeidYtelseTjeneste iayTjeneste,
                                                      VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
        this.vurderArbeidsforholdTjeneste = vurderArbeidsforholdTjeneste;
        this.arbeidsforholdUtenRelevantOppgittOpptjeningTjeneste = new ArbeidsforholdUtenRelevantOppgittOpptjening();
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        if (skalVentePåInntektsmelding(param)) {
            var skjæringstidspunkt = param.getSkjæringstidspunkt();
            boolean ventPåGyldigInntektsmeldingForArbeidsforhold = vurderArbeidsforholdTjeneste
                .inntektsmeldingMedArbeidsforholdIdSomIkkeMatcherArbeidsforholdIAAReg(param.getBehandlingId(), param.getAktørId(),
                    skjæringstidspunkt.getUtledetSkjæringstidspunkt());
            if (ventPåGyldigInntektsmeldingForArbeidsforhold) {
                return Collections.singletonList(opprettSettPåVentAutopunktUgyldigInntektsmelding());
            }
        }

        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)) {
            return INGEN_AKSJONSPUNKTER;
        }

        var iayGrunnlag = iayTjeneste.finnGrunnlag(param.getBehandlingId());
        if (iayGrunnlag.isPresent()) {
            var vurder = hentArbeidsforholdTilVurdering(param, iayGrunnlag.get());
            if (!vurder.isEmpty()) {
                return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);
            }
        }

        if (arbeidsforholdUtenRelevantOppgittOpptjeningTjeneste.erUtenRelevantOppgittOpptjening(param, iayGrunnlag)) {
            return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);
        }

        return INGEN_AKSJONSPUNKTER;
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> hentArbeidsforholdTilVurdering(AksjonspunktUtlederInput param,
                                                                                           InntektArbeidYtelseGrunnlag iayGrunnlag) {
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder;
        boolean taStillingTilEndringerIArbeidsforhold = skalTaStillingTilEndringerIArbeidsforhold(param.getRef());
        var sakInntektsmeldinger = taStillingTilEndringerIArbeidsforhold ? iayTjeneste.hentInntektsmeldinger(param.getSaksnummer()) : null;
        vurder = vurderArbeidsforholdTjeneste.vurder(param.getRef(), iayGrunnlag, sakInntektsmeldinger, taStillingTilEndringerIArbeidsforhold);
        return vurder;
    }

    private boolean skalVentePåInntektsmelding(AksjonspunktUtlederInput param) {
        return !(FagsakYtelseType.ENGANGSTØNAD.equals(param.getYtelseType()));
    }

    private AksjonspunktResultat opprettSettPåVentAutopunktUgyldigInntektsmelding() {
        LocalDateTime frist = LocalDateTime.of(LocalDate.now().plusDays(14), LocalTime.MIDNIGHT);
        AksjonspunktDefinisjon apDef = AksjonspunktDefinisjon.AUTO_VENT_INNTEKTSMELDING_MED_UGYLDIG_ARBEIDSFORHOLDID;
        return AksjonspunktResultat.opprettForAksjonspunktMedFrist(apDef, Venteårsak.VENT_PÅ_NY_INNTEKTSMELDING_MED_GYLDIG_ARB_ID, frist);
    }

    private boolean skalTaStillingTilEndringerIArbeidsforhold(BehandlingReferanse behandlingReferanse) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId());
        return !Objects.equals(behandlingReferanse.getBehandlingType(), BehandlingType.FØRSTEGANGSSØKNAD) || behandling.harSattStartpunkt();
    }
}
