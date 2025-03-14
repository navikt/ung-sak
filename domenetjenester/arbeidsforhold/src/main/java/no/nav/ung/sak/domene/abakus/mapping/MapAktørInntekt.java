package no.nav.ung.sak.domene.abakus.mapping;

import no.nav.abakus.iaygrunnlag.Aktør;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.Organisasjon;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.PersonIdent;
import no.nav.abakus.iaygrunnlag.inntekt.v1.InntekterDto;
import no.nav.abakus.iaygrunnlag.inntekt.v1.UtbetalingDto;
import no.nav.abakus.iaygrunnlag.inntekt.v1.UtbetalingsPostDto;
import no.nav.abakus.iaygrunnlag.kodeverk.InntektYtelseType;
import no.nav.ung.sak.domene.iay.modell.AktørInntekt;
import no.nav.ung.sak.domene.iay.modell.Inntekt;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektBuilder;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.OrgNummer;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class MapAktørInntekt {

    private MapAktørInntekt() {
        // Skjul konstruktør
    }

    private static final Comparator<UtbetalingDto> COMP_UTBETALING = Comparator
        .comparing((UtbetalingDto dto) -> dto.getKilde() == null ? null : dto.getKilde().getKode(), Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getUtbetaler() == null ? null : dto.getUtbetaler().getIdent(), Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<UtbetalingsPostDto> COMP_UTBETALINGSPOST = Comparator
        .comparing((UtbetalingsPostDto dto) -> dto.getInntektspostType().getKode(), Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getPeriode().getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getPeriode().getTom(), Comparator.nullsLast(Comparator.naturalOrder()));

    static class MapFraDto {

        @SuppressWarnings("unused")
        private final AktørId søkerAktørId;

        private final InntektArbeidYtelseAggregatBuilder aggregatBuilder;

        MapFraDto(AktørId søkerAktørId, InntektArbeidYtelseAggregatBuilder aggregatBuilder) {
            this.søkerAktørId = søkerAktørId;
            this.aggregatBuilder = aggregatBuilder;
        }

        List<AktørInntektBuilder> map(Collection<InntekterDto> dtos) {
            if (dtos == null || dtos.isEmpty()) {
                return Collections.emptyList();
            }

            var builders = dtos.stream().map(idto -> {
                var builder = aggregatBuilder.getAktørInntektBuilder(tilAktørId(idto.getPerson()));
                idto.getUtbetalinger().forEach(utbetalingDto -> builder.leggTilInntekt(mapUtbetaling(utbetalingDto)));
                return builder;
            }).toList();

            return builders;
        }

        /** Returnerer person sin aktørId. Denne trenger ikke være samme som søkers aktørid men kan f.eks. være annen part i en sak. */
        private AktørId tilAktørId(PersonIdent person) {
            if (!(person instanceof AktørIdPersonident)) {
                throw new IllegalArgumentException("Støtter kun " + AktørIdPersonident.class.getSimpleName() + " her");
            }
            return new AktørId(person.getIdent());
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

    static class MapTilDto {
        List<InntekterDto> map(Collection<AktørInntekt> aktørInntekt) {
            if (aktørInntekt == null || aktørInntekt.isEmpty()) {
                return Collections.emptyList();
            }
            return aktørInntekt.stream().map(this::mapTilInntekt).collect(Collectors.toList());
        }

        private InntekterDto mapTilInntekt(AktørInntekt ai) {
            InntekterDto dto = new InntekterDto(new AktørIdPersonident(ai.getAktørId().getId()));
            List<UtbetalingDto> utbetalinger = tilUtbetalinger(ai.getInntekt());
            dto.setUtbetalinger(utbetalinger);
            return dto;
        }

        private List<UtbetalingDto> tilUtbetalinger(Collection<Inntekt> inntekter) {
            return inntekter.stream().map(in -> tilUtbetaling(in)).sorted(COMP_UTBETALING).collect(Collectors.toList());
        }

        private UtbetalingDto tilUtbetaling(Inntekt inntekt) {
            Arbeidsgiver arbeidsgiver = inntekt.getArbeidsgiver();
            UtbetalingDto dto = new UtbetalingDto(KodeverkMapper.mapInntektsKildeTilDto(inntekt.getInntektsKilde()));
            dto.medArbeidsgiver(mapArbeidsgiver(arbeidsgiver));
            dto.setPoster(tilPoster(inntekt.getAlleInntektsposter()));
            return dto;
        }

        private Aktør mapArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            if (arbeidsgiver == null) {
                return null;
            }
            if (arbeidsgiver.getErVirksomhet()) {
                return new Organisasjon(arbeidsgiver.getOrgnr());
            } else {
                return new AktørIdPersonident(arbeidsgiver.getAktørId().getId());
            }
        }

        private List<UtbetalingsPostDto> tilPoster(Collection<Inntektspost> inntektspost) {
            return inntektspost.stream().map(this::tilPost).sorted(COMP_UTBETALINGSPOST).collect(Collectors.toList());
        }

        private UtbetalingsPostDto tilPost(Inntektspost inntektspost) {
            var periode = new Periode(inntektspost.getPeriode().getFomDato(), inntektspost.getPeriode().getTomDato());
            var inntektspostType = KodeverkMapper.mapInntektspostTypeTilDto(inntektspost.getInntektspostType());
            var ytelseType = inntektspost.getInntektYtelseType() != null ? InntektYtelseType.valueOf(inntektspost.getInntektYtelseType().name()) : null;
            var skattOgAvgiftType = KodeverkMapper.mapSkatteOgAvgiftsregelTilDto(inntektspost.getSkatteOgAvgiftsregelType());

            UtbetalingsPostDto dto = new UtbetalingsPostDto(periode, inntektspostType)
                .medInntektYtelseType(ytelseType)
                .medSkattAvgiftType(skattOgAvgiftType)
                .medBeløp(inntektspost.getBeløp().getVerdi());

            return dto;
        }

    }

}
