package no.nav.ung.sak.domene.registerinnhenting.impl.behandlingårsak;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
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
@FagsakYtelseTypeRef
class BehandlingÅrsakUtlederInntektArbeidYtelse implements BehandlingÅrsakUtleder {
    private static final Logger log = LoggerFactory.getLogger(BehandlingÅrsakUtlederInntektArbeidYtelse.class);

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BehandlingÅrsakUtlederAktørInntekt behandlingÅrsakUtlederAktørInntekt;
    private BehandlingÅrsakUtlederAktørYtelse behandlingÅrsakUtlederAktørYtelse;


    @Inject
    public BehandlingÅrsakUtlederInntektArbeidYtelse(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                     BehandlingÅrsakUtlederAktørInntekt behandlingÅrsakUtlederAktørInntekt,
                                                     BehandlingÅrsakUtlederAktørYtelse behandlingÅrsakUtlederAktørYtelse) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
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
        boolean erAktørInntektEndret = iayGrunnlagDiff.erEndringPåAktørInntektForAktør(skjæringstidspunkt, ref.getAktørId());
        AktørYtelseEndring aktørYtelseEndring = iayGrunnlagDiff.endringPåAktørYtelseForAktør(saksnummer, skjæringstidspunkt, ref.getAktørId());


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

        return behandlingÅrsakTyper;
    }
}
