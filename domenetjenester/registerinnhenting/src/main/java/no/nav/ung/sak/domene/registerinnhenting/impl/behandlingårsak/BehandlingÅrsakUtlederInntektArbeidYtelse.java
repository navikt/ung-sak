package no.nav.ung.sak.domene.registerinnhenting.impl.behandlingårsak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.arbeidsforhold.AktørYtelseEndring;
import no.nav.ung.sak.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.ung.sak.typer.Saksnummer;

@ApplicationScoped
@GrunnlagRef(InntektArbeidYtelseGrunnlag.class)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(FRISINN)
class BehandlingÅrsakUtlederInntektArbeidYtelse implements BehandlingÅrsakUtleder {
    private static final Logger log = LoggerFactory.getLogger(BehandlingÅrsakUtlederInntektArbeidYtelse.class);

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    private BehandlingÅrsakUtlederInntektsmelding behandlingÅrsakUtlederInntektsmelding;
    private BehandlingÅrsakUtlederAktørArbeid behandlingÅrsakUtlederAktørArbeid;
    private BehandlingÅrsakUtlederAktørInntekt behandlingÅrsakUtlederAktørInntekt;
    private BehandlingÅrsakUtlederAktørYtelse behandlingÅrsakUtlederAktørYtelse;


    @Inject
    public BehandlingÅrsakUtlederInntektArbeidYtelse(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                     BehandlingÅrsakUtlederInntektsmelding behandlingÅrsakUtlederInntektsmelding,
                                                     BehandlingÅrsakUtlederAktørArbeid behandlingÅrsakUtlederAktørArbeid,
                                                     BehandlingÅrsakUtlederAktørInntekt behandlingÅrsakUtlederAktørInntekt,
                                                     BehandlingÅrsakUtlederAktørYtelse behandlingÅrsakUtlederAktørYtelse) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.behandlingÅrsakUtlederInntektsmelding = behandlingÅrsakUtlederInntektsmelding;
        this.behandlingÅrsakUtlederAktørArbeid = behandlingÅrsakUtlederAktørArbeid;
        this.behandlingÅrsakUtlederAktørInntekt = behandlingÅrsakUtlederAktørInntekt;
        this.behandlingÅrsakUtlederAktørYtelse = behandlingÅrsakUtlederAktørYtelse;
    }

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {

        LocalDate skjæringstidspunkt = ref.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        return hentAlleBehandlingÅrsakTyperForInntektArbeidYtelse(ref, skjæringstidspunkt, (UUID) grunnlagId1, (UUID) grunnlagId2);
    }

    private Set<BehandlingÅrsakType> hentAlleBehandlingÅrsakTyperForInntektArbeidYtelse(BehandlingReferanse ref, LocalDate skjæringstidspunkt, UUID grunnlagUuid1, UUID grunnlagUuid2) {
        Saksnummer saksnummer = ref.getSaksnummer();

        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag1 = grunnlagUuid1 != null ? inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(ref.getBehandlingId(), grunnlagUuid1) : null;
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag2 = grunnlagUuid2 != null ? inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(ref.getBehandlingId(), grunnlagUuid2) : null;

        Set<BehandlingÅrsakType> behandlingÅrsakTyper = new HashSet<>();

        IAYGrunnlagDiff iayGrunnlagDiff = new IAYGrunnlagDiff(inntektArbeidYtelseGrunnlag1, inntektArbeidYtelseGrunnlag2);
        boolean erAktørArbeidEndret = iayGrunnlagDiff.erEndringPåAktørArbeidForAktør(skjæringstidspunkt, ref.getAktørId());
        boolean erAktørInntektEndret = iayGrunnlagDiff.erEndringPåAktørInntektForAktør(skjæringstidspunkt, ref.getAktørId());
        boolean erInntektsmeldingEndret = iayGrunnlagDiff.erEndringPåInntektsmelding();
        AktørYtelseEndring aktørYtelseEndring = iayGrunnlagDiff.endringPåAktørYtelseForAktør(saksnummer, skjæringstidspunkt, ref.getAktørId());

        if (erAktørArbeidEndret) {
            BehandlingÅrsakType behandlingÅrsakTypeAktørArbeid = behandlingÅrsakUtlederAktørArbeid.utledBehandlingÅrsak();
            log.info("Setter behandlingårsak til {}, har endring i aktør arbeid, grunnlagId1: {}, grunnlagId2: {}", behandlingÅrsakTypeAktørArbeid, grunnlagUuid1, grunnlagUuid2);
            behandlingÅrsakTyper.add(behandlingÅrsakTypeAktørArbeid);
        }
        if (aktørYtelseEndring.erEndret()) {
            BehandlingÅrsakType behandlingÅrsakTypeAktørYtelse = behandlingÅrsakUtlederAktørYtelse.utledBehandlingÅrsak();
            log.info("Setter behandlingårsak til {}, har endring i aktør ytelse, grunnlagId1: {}, grunnlagId2: {}", behandlingÅrsakTypeAktørYtelse, grunnlagUuid1, grunnlagUuid2);
            behandlingÅrsakTyper.add(behandlingÅrsakTypeAktørYtelse);
        }
        if (erAktørInntektEndret) {
            BehandlingÅrsakType behandlingÅrsakTypeAktørInntekt = behandlingÅrsakUtlederAktørInntekt.utledBehandlingÅrsak();
            log.info("Setter behandlingårsak til {}, har endring i aktør inntekt, grunnlagId1: {}, grunnlagId2: {}", behandlingÅrsakTypeAktørInntekt, grunnlagUuid1, grunnlagUuid2);
            behandlingÅrsakTyper.add(behandlingÅrsakTypeAktørInntekt);
        }
        if (erInntektsmeldingEndret) {
            BehandlingÅrsakType behandlingÅrsakTypeInntektsmelding = behandlingÅrsakUtlederInntektsmelding.utledBehandlingÅrsak();
            log.info("Setter behandlingårsak til {}, har endring i inntektsmelding, grunnlagId1: {}, grunnlagId2: {}", behandlingÅrsakTypeInntektsmelding, grunnlagUuid1, grunnlagUuid2);
            behandlingÅrsakTyper.add(behandlingÅrsakTypeInntektsmelding);
        }

        return behandlingÅrsakTyper;
    }
}
