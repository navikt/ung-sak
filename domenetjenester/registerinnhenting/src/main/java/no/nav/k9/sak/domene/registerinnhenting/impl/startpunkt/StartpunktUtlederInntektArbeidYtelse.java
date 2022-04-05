package no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
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
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
@GrunnlagRef(InntektArbeidYtelseGrunnlag.class)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(FRISINN)
class StartpunktUtlederInntektArbeidYtelse implements EndringStartpunktUtleder {

    private String klassenavn = this.getClass().getSimpleName();
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private StartpunktUtlederInntektsmeldinger startpunktUtlederInntektsmeldinger;
    private VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private BehandlingRepository behandlingRepository;

    public StartpunktUtlederInntektArbeidYtelse() {
        // For CDI
    }

    @Inject
    StartpunktUtlederInntektArbeidYtelse(InntektArbeidYtelseTjeneste iayTjeneste, // NOSONAR - ingen enkel måte å unngå mange parametere her
                                         BehandlingRepositoryProvider repositoryProvider,
                                         StartpunktUtlederInntektsmeldinger startpunktUtlederInntektsmeldinger,
                                         VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste,
                                         @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.startpunktUtlederInntektsmeldinger = startpunktUtlederInntektsmeldinger;
        this.vurderArbeidsforholdTjeneste = vurderArbeidsforholdTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return hentAlleStartpunktForInntektArbeidYtelse(ref, (UUID) grunnlagId1, (UUID) grunnlagId2).stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private List<StartpunktType> hentAlleStartpunktForInntektArbeidYtelse(BehandlingReferanse ref, UUID grunnlagId1, UUID grunnlagId2) { // NOSONAR
        List<StartpunktType> startpunkter = new ArrayList<>();
        var grunnlag1 = grunnlagId1 != null ? iayTjeneste.hentGrunnlagForGrunnlagId(ref.getBehandlingId(), grunnlagId1) : null;
        var grunnlag2 = grunnlagId2 != null ? iayTjeneste.hentGrunnlagForGrunnlagId(ref.getBehandlingId(), grunnlagId2) : null;
        var diff = new IAYGrunnlagDiff(grunnlag1, grunnlag2);

        if (startpunktUtlederInntektsmeldinger.inntektsmeldingErSøknad(ref)) {
            var startpunktType = startpunktUtlederInntektsmeldinger.utledStartpunkt(ref, grunnlag2);
            boolean erInntektsmeldingEndret = !StartpunktType.UDEFINERT.equals(startpunktType);
            if (erInntektsmeldingEndret) {
                leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, startpunktType, "tilkommet inntektsmeldinger");
            }
        } else {
            boolean erInntektsmeldingEndret = diff.erEndringPåInntektsmelding();
            if (erInntektsmeldingEndret) {
                leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.BEREGNING, "inntektsmelding");
            }
        }

        if (!startpunkter.isEmpty()) {
            return startpunkter; // quick exit siden vi har allerede testet to viktigste startpunkt typer
        }
        Saksnummer saksnummer = ref.getSaksnummer();

        if (harAksjonspunkt5080(ref)) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.KONTROLLER_ARBEIDSFORHOLD, "manuell vurdering av arbeidsforhold");
        } else if (erPåkrevdManuelleAvklaringer(ref)) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.KONTROLLER_ARBEIDSFORHOLD, "manuell vurdering av arbeidsforhold");
        }

        if (!startpunkter.isEmpty()) {
            return startpunkter; // quick exit siden vi har allerede testet to viktigste startpunkt typer
        }

        if (FagsakYtelseType.FRISINN.equals(ref.getFagsakYtelseType())) {
            diffForFrisinn(ref, grunnlagId1, grunnlagId2, startpunkter, diff, saksnummer);

        } else {
            var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType());
            var perioderTilVurdering = perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.OPPTJENINGSVILKÅRET);

            for (DatoIntervallEntitet periode : perioderTilVurdering) {
                var opptjeningsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato().minusDays(30), periode.getFomDato());

                boolean erAktørArbeidEndretForSøker = diff.erEndringPåAktørArbeidForAktør(opptjeningsperiode, ref.getAktørId());
                boolean aktørYtelseEndring = diff.endringPåAktørYtelseForAktør(saksnummer, opptjeningsperiode, ref.getAktørId());
                if (erAktørArbeidEndretForSøker) {
                    leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.OPPTJENING, "aktørarbeid for periode " + opptjeningsperiode);
                } else if (aktørYtelseEndring) {
                    leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.OPPTJENING, "aktør ytelse andre tema for periode " + opptjeningsperiode);
                } else {
                    var relevantInntektsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato().minusMonths(3), periode.getFomDato());
                    boolean erAktørInntektEndretForSøker = diff.erEndringPåAktørInntektForAktør(relevantInntektsperiode, ref.getAktørId());
                    if (erAktørInntektEndretForSøker) {
                        leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.OPPTJENING, "aktør inntekt for periode " + relevantInntektsperiode);
                    }
                }
            }
        }

        return startpunkter;
    }

    private void diffForFrisinn(BehandlingReferanse ref, UUID grunnlagId1, UUID grunnlagId2, List<StartpunktType> startpunkter, IAYGrunnlagDiff diff, Saksnummer saksnummer) {
        LocalDate skjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();
        AktørYtelseEndring aktørYtelseEndringForSøker = diff.endringPåAktørYtelseForAktør(saksnummer, skjæringstidspunkt, ref.getAktørId());
        boolean erAktørArbeidEndretForSøker = diff.erEndringPåAktørArbeidForAktør(skjæringstidspunkt, ref.getAktørId());
        if (erAktørArbeidEndretForSøker) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.OPPTJENING, "aktørarbeid");
        } else if (aktørYtelseEndringForSøker.erAndreYtelserEndret()) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.OPPTJENING, "aktør ytelse andre tema");
        } else if (aktørYtelseEndringForSøker.erEksklusiveYtelserEndret()) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.KONTROLLER_ARBEIDSFORHOLD, "aktør ytelse");
        } else {
            boolean erAktørInntektEndretForSøker = diff.erEndringPåAktørInntektForAktør(skjæringstidspunkt, ref.getAktørId());
            if (erAktørInntektEndretForSøker) {
                leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.OPPTJENING, "aktør inntekt");
            }
        }
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
