package no.nav.ung.sak.domene.abakus.mapping;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittOpptjeningDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseAggregatOverstyrtDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseAggregatRegisterDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.abakus.iaygrunnlag.v1.OverstyrtInntektArbeidYtelseDto;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.typer.AktørId;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

public class IAYTilDtoMapper {

    private final AktørId aktørId;
    private final UUID grunnlagReferanse;
    private final UUID behandlingReferanse;

    public IAYTilDtoMapper(AktørId aktørId,
                           UUID grunnlagReferanse,
                           UUID behandlingReferanse) {
        this.aktørId = aktørId;
        this.grunnlagReferanse = grunnlagReferanse;
        this.behandlingReferanse = behandlingReferanse;
    }

    public OverstyrtInntektArbeidYtelseDto mapTilDto(FagsakYtelseType fagsakYtelseType, InntektArbeidYtelseAggregat overstyrt) {
        var ytelseType = YtelseType.fraKode(fagsakYtelseType.getKode());
        var person = new AktørIdPersonident(aktørId.getId());
        var overstyrtDto = mapSaksbehandlerOverstyrteOpplysninger(overstyrt);
        return new OverstyrtInntektArbeidYtelseDto(person, grunnlagReferanse, behandlingReferanse, ytelseType, null, overstyrtDto);
    }

    public InntektArbeidYtelseGrunnlagDto mapTilDto(YtelseType ytelseType, InntektArbeidYtelseGrunnlag grunnlag) {
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

        grunnlag.getSaksbehandletVersjon().ifPresent(a -> dto.medOverstyrt(mapSaksbehandlerOverstyrteOpplysninger(a)));

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

    private InntektArbeidYtelseAggregatOverstyrtDto mapSaksbehandlerOverstyrteOpplysninger(InntektArbeidYtelseAggregat aggregat) {
        if (aggregat == null) {
            return null;
        }

        var tidspunkt = Optional.ofNullable(aggregat.getOpprettetTidspunkt())
            .map(it -> it.atZone(ZoneId.systemDefault()).toOffsetDateTime())
            .orElse(OffsetDateTime.now());

        var overstyrt = new InntektArbeidYtelseAggregatOverstyrtDto(tidspunkt, aggregat.getEksternReferanse());
        return overstyrt;
    }

}
