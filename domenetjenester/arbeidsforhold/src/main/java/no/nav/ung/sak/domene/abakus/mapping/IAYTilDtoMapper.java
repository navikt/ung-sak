package no.nav.ung.sak.domene.abakus.mapping;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittOpptjeningDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseAggregatRegisterDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.typer.AktørId;

public class IAYTilDtoMapper {

    private AktørId aktørId;
    private UUID grunnlagReferanse;
    private UUID behandlingReferanse;

    public IAYTilDtoMapper(AktørId aktørId,
                           UUID grunnlagReferanse,
                           UUID behandlingReferanse) {
        this.aktørId = aktørId;
        this.grunnlagReferanse = grunnlagReferanse;
        this.behandlingReferanse = behandlingReferanse;
    }

    public InntektArbeidYtelseGrunnlagDto mapTilDto(YtelseType ytelseType, InntektArbeidYtelseGrunnlag grunnlag, boolean validerArbeidsforholdId) {
        if (grunnlag == null) {
            return null;
        }

        var grunnlagTidspunkt = grunnlag.getOpprettetTidspunkt().atZone(ZoneId.systemDefault()).toOffsetDateTime();

        var dto = new InntektArbeidYtelseGrunnlagDto(
            new AktørIdPersonident(aktørId.getId()),
            grunnlagTidspunkt,
            grunnlagReferanse,
            behandlingReferanse,
            ytelseType);

        // REGISTEROPPLYSNINGER
        grunnlag.getRegisterVersjon().ifPresent(a -> mapRegisterOpplysninger(a, dto));

        // OPPGITT OPPTJENING
        grunnlag.getOppgittOpptjening().ifPresent(oo -> dto.medOppgittOpptjening(new MapOppgittOpptjening().mapTilDto(oo)));
        grunnlag.getOppgittOpptjeningAggregat().ifPresent(oa -> dto.medOppgittOpptjeninger(new MapOppgittOpptjening().mapTilDto(oa)));
        return dto;
    }

    public OppgittOpptjeningDto mapTilDto(OppgittOpptjeningBuilder builder) {
        return new MapOppgittOpptjening().mapTilDto(builder.build());
    }


    private void mapRegisterOpplysninger(InntektArbeidYtelseAggregat aggregat,
                                         InntektArbeidYtelseGrunnlagDto dto) {
        var tidspunkt = Optional.ofNullable(aggregat.getOpprettetTidspunkt())
            .map(it -> it.atZone(ZoneId.systemDefault()).toOffsetDateTime())
            .orElse(OffsetDateTime.now());

        var inntekter = new MapAktørInntekt.MapTilDto().map(aggregat.getAktørInntekt());
        var ytelser = new MapAktørYtelse.MapTilDto().map(aggregat.getAktørYtelse());
        var register = new InntektArbeidYtelseAggregatRegisterDto(tidspunkt, aggregat.getEksternReferanse())
            .medInntekt(inntekter)
            .medYtelse(ytelser);
        dto.medRegister(register);
    }

}
