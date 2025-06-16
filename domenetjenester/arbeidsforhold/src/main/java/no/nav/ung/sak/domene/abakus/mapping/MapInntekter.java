package no.nav.ung.sak.domene.abakus.mapping;

import no.nav.abakus.iaygrunnlag.Aktør;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.PersonIdent;
import no.nav.abakus.iaygrunnlag.inntekt.v1.InntekterDto;
import no.nav.abakus.iaygrunnlag.inntekt.v1.UtbetalingDto;
import no.nav.abakus.iaygrunnlag.inntekt.v1.UtbetalingsPostDto;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder.InntekterBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.OrgNummer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class MapInntekter {

    private MapInntekter() {
        // Skjul konstruktør
    }

    static class MapFraDto {

        @SuppressWarnings("unused")
        private final InntektArbeidYtelseAggregatBuilder aggregatBuilder;

        MapFraDto(InntektArbeidYtelseAggregatBuilder aggregatBuilder) {
            this.aggregatBuilder = aggregatBuilder;
        }

        List<InntekterBuilder> map(Collection<InntekterDto> dtos) {
            if (dtos == null || dtos.isEmpty()) {
                return Collections.emptyList();
            }

            var builders = dtos.stream().map(idto -> {
                var builder = aggregatBuilder.getInntekterBuilder();
                idto.getUtbetalinger().forEach(utbetalingDto -> builder.leggTilInntekt(mapUtbetaling(utbetalingDto)));
                return builder;
            }).toList();

            return builders;
        }

        private InntektBuilder mapUtbetaling(UtbetalingDto dto) {
            InntektBuilder inntektBuilder = InntektBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(mapArbeidsgiver(dto.getUtbetaler()))
                .medInntektsKilde(KodeverkMapper.mapInntektsKildeFraDto(dto.getKilde()));
            dto.getPoster()
                .forEach(post -> inntektBuilder.leggTilInntektspost(mapInntektspost(post)));
            return inntektBuilder;
        }

        private InntektspostBuilder mapInntektspost(UtbetalingsPostDto post) {
            return InntektspostBuilder.ny()
                .medBeløp(post.getBeløp())
                .medInntektspostType(KodeverkMapper.mapInntektspostTypeFraDto(post.getInntektspostType()))
                .medPeriode(post.getPeriode().getFom(), post.getPeriode().getTom())
                .medSkatteOgAvgiftsregelType(KodeverkMapper.mapSkatteOgAvgiftsregelFraDto(post.getSkattAvgiftType()))
                .medLønnsinntektBeskrivelse(KodeverkMapper.mapLønnsinntektBeskrivelseFraDto(post.getLønnsinntektBeskrivelse()))
                .medInntektYtelse(KodeverkMapper.mapUtbetaltYtelseTypeTilGrunnlag(post.getInntektYtelseType()));
        }

        private Arbeidsgiver mapArbeidsgiver(Aktør arbeidsgiver) {
            if (arbeidsgiver == null)
                return null;
            if (arbeidsgiver.getErOrganisasjon()) {
                return Arbeidsgiver.virksomhet(new OrgNummer(arbeidsgiver.getIdent()));
            }
            return Arbeidsgiver.person(new AktørId(arbeidsgiver.getIdent()));
        }

    }

}
