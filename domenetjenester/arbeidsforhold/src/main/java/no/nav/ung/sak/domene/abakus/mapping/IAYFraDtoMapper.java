package no.nav.ung.sak.domene.abakus.mapping;

import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittOpptjeningDto;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgitteOpptjeningerDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.ung.sak.domene.iay.modell.*;
import no.nav.ung.sak.typer.AktørId;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Merk denne mapper alltid hele aggregat tilbake til nye instanser av IAY Aggregat. (i motsetning til tilsvarende implementasjon i ABakus som mapper til eksisterende instans).
 */
public class IAYFraDtoMapper {

    private final AktørId aktørId;

    public IAYFraDtoMapper(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    /**
     * Til bruk for migrering (sender inn registerdata, istdf. å hente fra registerne.).  Merk tar ikke hensyn til eksisterende grunnlag lagret (mapper kun input).
     */
    public InntektArbeidYtelseGrunnlag mapTilGrunnlagInklusivRegisterdata(InntektArbeidYtelseGrunnlagDto dto, boolean erAktivtGrunnlag) {
        var builder = InntektArbeidYtelseGrunnlagBuilder.ny(UUID.fromString(dto.getGrunnlagReferanse()), dto.getGrunnlagTidspunkt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
        builder.medErAktivtGrunnlag(erAktivtGrunnlag);
        return mapTilGrunnlagInklusivRegisterdata(dto, builder);
    }

    /**
     * @see #mapTilGrunnlagInklusivRegisterdata(InntektArbeidYtelseGrunnlagDto, boolean)
     */
    public InntektArbeidYtelseGrunnlag mapTilGrunnlagInklusivRegisterdata(InntektArbeidYtelseGrunnlagDto dto, InntektArbeidYtelseGrunnlagBuilder builder) {
        mapSaksbehandlerDataTilBuilder(dto, builder);
        mapTilGrunnlagBuilder(dto, builder);

        // ta med registerdata til grunnlaget
        mapRegisterDataTilMigrering(dto, builder);

        return builder.build();
    }

    // brukes kun til migrering av data (dytter inn IAYG)
    private void mapRegisterDataTilMigrering(InntektArbeidYtelseGrunnlagDto dto, InntektArbeidYtelseGrunnlagBuilder builder) {
        var register = dto.getRegister();
        if (register == null) return;

        var tidspunkt = register.getOpprettetTidspunkt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

        var registerBuilder = InntektArbeidYtelseAggregatBuilder.builderFor(Optional.empty(), register.getEksternReferanse(), tidspunkt, VersjonType.REGISTER);

        var aktørArbeid = new MapAktørArbeid.MapFraDto(aktørId, registerBuilder).map(register.getArbeid());
        var aktørInntekt = new MapAktørInntekt.MapFraDto(aktørId, registerBuilder).map(register.getInntekt());
        var aktørYtelse = new MapAktørYtelse.MapFraDto(aktørId, registerBuilder).map(register.getYtelse());

        aktørArbeid.forEach(registerBuilder::leggTilAktørArbeid);
        aktørInntekt.forEach(registerBuilder::leggTilAktørInntekt);
        aktørYtelse.forEach(registerBuilder::leggTilAktørYtelse);

        builder.medData(registerBuilder);
    }

    private void mapTilGrunnlagBuilder(InntektArbeidYtelseGrunnlagDto dto, InntektArbeidYtelseGrunnlagBuilder builder) {
        var arbeidsforholdInformasjonBuilder = new MapArbeidsforholdInformasjon.MapFraDto(builder).map(dto.getArbeidsforholdInformasjon());

        var oppgittOpptjening = mapOppgttOpptjening(dto.getOppgittOpptjening());
        var overstyrtOppgittOpptjening = mapOppgttOpptjening(dto.getOverstyrtOppgittOpptjening());
        var arbeidsforholdInformasjon = arbeidsforholdInformasjonBuilder.build();

        builder.medOverstyrtOppgittOpptjening(overstyrtOppgittOpptjening);
        builder.medOppgittOpptjening(oppgittOpptjening);
        builder.medOppgittOpptjeningAggregat(mapOppgitteOpptjeninger(dto.getOppgitteOpptjeninger()));
        builder.medInformasjon(arbeidsforholdInformasjon);
    }

    private Collection<OppgittOpptjeningBuilder> mapOppgitteOpptjeninger(OppgitteOpptjeningerDto oppgitteOpptjeninger) {
        if (oppgitteOpptjeninger == null) {
            return null;
        }
        return oppgitteOpptjeninger.getOppgitteOpptjeninger().stream()
            .map(this::mapOppgttOpptjening)
            .collect(Collectors.toList());
    }

    public OppgittOpptjeningBuilder mapOppgttOpptjening(OppgittOpptjeningDto oppgittOpptjening) {
        return new MapOppgittOpptjening().mapFraDto(oppgittOpptjening);
    }

    private void mapSaksbehandlerDataTilBuilder(InntektArbeidYtelseGrunnlagDto dto, InntektArbeidYtelseGrunnlagBuilder builder) {
        var overstyrt = dto.getOverstyrt();
        if (overstyrt != null) {
            var tidspunkt = overstyrt.getOpprettetTidspunkt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            var saksbehandlerOverstyringer = InntektArbeidYtelseAggregatBuilder.builderFor(Optional.empty(), overstyrt.getEksternReferanse(), tidspunkt, VersjonType.SAKSBEHANDLET);
            var overstyrtAktørArbeid = new MapAktørArbeid.MapFraDto(aktørId, saksbehandlerOverstyringer).map(overstyrt.getArbeid());
            overstyrtAktørArbeid.forEach(saksbehandlerOverstyringer::leggTilAktørArbeid);
            builder.medData(saksbehandlerOverstyringer);
        }
    }
}
