package no.nav.ung.sak.domene.registerinnhenting.impl.behandlingårsak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@GrunnlagRef(InntektArbeidYtelseGrunnlag.class)
@FagsakYtelseTypeRef
class BehandlingÅrsakUtlederInntektArbeidYtelse implements BehandlingÅrsakUtleder {
    private static final Logger log = LoggerFactory.getLogger(BehandlingÅrsakUtlederInntektArbeidYtelse.class);

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BehandlingÅrsakUtlederInntekter behandlingÅrsakUtlederInntekter;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;


    @Inject
    public BehandlingÅrsakUtlederInntektArbeidYtelse(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                     BehandlingÅrsakUtlederInntekter behandlingÅrsakUtlederInntekter,
                                                     ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.behandlingÅrsakUtlederInntekter = behandlingÅrsakUtlederInntekter;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
    }

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        final var kontrollTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(ref.getBehandlingId()).filterValue(it -> it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
        return hentAlleBehandlingÅrsakTyperForInntektArbeidYtelse(ref, kontrollTidslinje, (UUID) grunnlagId1, (UUID) grunnlagId2);
    }

    private Set<BehandlingÅrsakType> hentAlleBehandlingÅrsakTyperForInntektArbeidYtelse(BehandlingReferanse ref, LocalDateTimeline<?> perioder, UUID grunnlagUuid1, UUID grunnlagUuid2) {
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag1 = grunnlagUuid1 != null ? inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(ref.getBehandlingId(), grunnlagUuid1) : null;
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag2 = grunnlagUuid2 != null ? inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(ref.getBehandlingId(), grunnlagUuid2) : null;

        Set<BehandlingÅrsakType> behandlingÅrsakTyper = new HashSet<>();

        IAYGrunnlagDiff iayGrunnlagDiff = new IAYGrunnlagDiff(inntektArbeidYtelseGrunnlag1, inntektArbeidYtelseGrunnlag2);
        boolean erInntektEndret = iayGrunnlagDiff.erEndringPåInntekter(perioder);

        if (erInntektEndret) {
            BehandlingÅrsakType behandlingÅrsakTypeInntekt = behandlingÅrsakUtlederInntekter.utledBehandlingÅrsak();
            log.info("Setter behandlingårsak til {}, har endring i aktør inntekt, grunnlagId1: {}, grunnlagId2: {}", behandlingÅrsakTypeInntekt, grunnlagUuid1, grunnlagUuid2);
            behandlingÅrsakTyper.add(behandlingÅrsakTypeInntekt);
        }

        return behandlingÅrsakTyper;
    }
}
