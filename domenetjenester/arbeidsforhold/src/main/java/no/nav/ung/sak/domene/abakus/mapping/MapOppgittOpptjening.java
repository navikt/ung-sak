package no.nav.ung.sak.domene.abakus.mapping;

import no.nav.abakus.iaygrunnlag.JournalpostId;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.*;
import no.nav.ung.sak.domene.iay.modell.*;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansBuilder;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class MapOppgittOpptjening {

    private static final Comparator<OppgittFrilansoppdragDto> COMP_FRILANSOPPDRAG = Comparator
        .comparing((OppgittFrilansoppdragDto dto) -> dto.getOppdragsgiver(), Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getPeriode().getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getPeriode().getTom(), Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<OppgittAnnenAktivitetDto> COMP_ANNEN_AKTIVITET = Comparator
        .comparing((OppgittAnnenAktivitetDto dto) -> dto.getArbeidTypeDto() == null ? null : dto.getArbeidTypeDto().getKode(),
            Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getPeriode().getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getPeriode().getTom(), Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<OppgittArbeidsforholdDto> COMP_OPPGITT_ARBEIDSFORHOLD = Comparator
        .comparing((OppgittArbeidsforholdDto dto) -> dto.getArbeidTypeDto() == null ? null : dto.getArbeidTypeDto().getKode(),
            Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getPeriode().getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getPeriode().getTom(), Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getLandkode() == null ? null : dto.getLandkode().getKode(), Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getVirksomhetNavn(), Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<OppgittEgenNæringDto> COMP_OPPGITT_EGEN_NÆRING = Comparator
        .comparing((OppgittEgenNæringDto dto) -> dto.getVirksomhetTypeDto() == null ? null : dto.getVirksomhetTypeDto().getKode(),
            Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getPeriode().getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getPeriode().getTom(), Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getVirksomhet() == null ? null : dto.getVirksomhet().getIdent(), Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getLandkode() == null ? null : dto.getLandkode().getKode(), Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getVirksomhetNavn(), Comparator.nullsLast(Comparator.naturalOrder()));

    OppgittOpptjeningDto mapTilDto(OppgittOpptjening oppgittOpptjening) {
        return MapTilDto.map(oppgittOpptjening);
    }

    OppgitteOpptjeningerDto mapTilDto(OppgittOpptjeningAggregat oppgitteOpptjeninger) {
        return MapTilDto.map(oppgitteOpptjeninger);
    }

    OppgittOpptjeningBuilder mapFraDto(OppgittOpptjeningDto oppgittOpptjening) {
        return MapFraDto.map(oppgittOpptjening);
    }

    private static class MapTilDto {

        private MapTilDto() {
            // Skjul
        }

        private static OppgitteOpptjeningerDto map(OppgittOpptjeningAggregat oppgitteOpptjeninger) {
            return new OppgitteOpptjeningerDto().medOppgitteOpptjeninger(
                oppgitteOpptjeninger.getOppgitteOpptjeninger().stream()
                    .map(MapTilDto::map)
                    .collect(Collectors.toList())
            );
        }

        private static OppgittOpptjeningDto map(OppgittOpptjening oppgittOpptjening) {
            if (oppgittOpptjening == null)
                return null;

            JournalpostId journalpostId = oppgittOpptjening.getJournalpostId() != null
                ? new JournalpostId(oppgittOpptjening.getJournalpostId().getVerdi())
                : null;
            OffsetDateTime innsendtTid = oppgittOpptjening.getInnsendingstidspunkt() != null
                ? oppgittOpptjening.getInnsendingstidspunkt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                : null;
            OffsetDateTime opprettetTid = oppgittOpptjening.getOpprettetTidspunkt().atZone(ZoneId.systemDefault()).toOffsetDateTime();
            var dto = new OppgittOpptjeningDto(journalpostId, innsendtTid, oppgittOpptjening.getEksternReferanse(), opprettetTid);

            dto.medArbeidsforhold(oppgittOpptjening.getOppgittArbeidsforhold().stream().map(MapTilDto::mapArbeidsforhold).sorted(COMP_OPPGITT_ARBEIDSFORHOLD)
                .collect(Collectors.toList()));
            dto.medAnnenAktivitet(
                oppgittOpptjening.getAnnenAktivitet().stream().map(MapTilDto::mapAnnenAktivitet).sorted(COMP_ANNEN_AKTIVITET).collect(Collectors.toList()));

            oppgittOpptjening.getFrilans().ifPresent(f -> dto.medFrilans(mapFrilans(f)));
            return dto;
        }

        private static OppgittFrilansDto mapFrilans(OppgittFrilans frilans) {
            if (frilans == null)
                return null;

            var frilansoppdrag = frilans.getFrilansoppdrag().stream().map(MapTilDto::mapFrilansoppdrag).sorted(COMP_FRILANSOPPDRAG)
                .collect(Collectors.toList());
            var frilansDto = new OppgittFrilansDto(frilansoppdrag);
            frilansDto.medErNyoppstartet(booleanOrFalse(frilans.getErNyoppstartet()));
            frilansDto.medHarInntektFraFosterhjem(booleanOrFalse(frilans.getHarInntektFraFosterhjem()));
            frilansDto.medHarNærRelasjon(booleanOrFalse(frilans.getHarNærRelasjon()));
            return frilansDto;
        }

        private static boolean booleanOrFalse(Boolean bool) {
            return bool != null && bool;
        }

        private static OppgittArbeidsforholdDto mapArbeidsforhold(OppgittArbeidsforhold arbeidsforhold) {
            if (arbeidsforhold == null)
                return null;

            DatoIntervallEntitet periode1 = arbeidsforhold.getPeriode();
            var periode = new Periode(periode1.getFomDato(), periode1.getTomDato());
            var arbeidType = KodeverkMapper.mapArbeidTypeTilDto(arbeidsforhold.getArbeidType());

            var dto = new OppgittArbeidsforholdDto(periode, arbeidType);

            dto.setInntekt(arbeidsforhold.getInntekt());
            return dto;
        }

        private static BigDecimal minMax(BigDecimal val, BigDecimal min, BigDecimal max) {
            if (val == null) {
                return null;
            }
            if (min != null && val.compareTo(min) < 0) {
                return min;
            }
            if (max != null && val.compareTo(max) > 0) {
                return max;
            }
            return val;
        }

        private static OppgittFrilansoppdragDto mapFrilansoppdrag(OppgittFrilansoppdrag frilansoppdrag) {
            var periode = new Periode(frilansoppdrag.getPeriode().getFomDato(), frilansoppdrag.getPeriode().getTomDato());
            var oppdragsgiver = frilansoppdrag.getOppdragsgiver();
            BigDecimal inntekt = frilansoppdrag.getInntekt();
            OppgittFrilansoppdragDto oppgittFrilansoppdragDto = new OppgittFrilansoppdragDto(periode, fjernUnicodeControlOgAlternativeWhitespaceCharacters(oppdragsgiver));
            if (inntekt != null) {
                return oppgittFrilansoppdragDto.medInntekt(inntekt);
            }
            return oppgittFrilansoppdragDto;
        }

        private static OppgittAnnenAktivitetDto mapAnnenAktivitet(OppgittAnnenAktivitet annenAktivitet) {
            var periode = new Periode(annenAktivitet.getPeriode().getFomDato(), annenAktivitet.getPeriode().getTomDato());
            var arbeidType = KodeverkMapper.mapArbeidTypeTilDto(annenAktivitet.getArbeidType());
            return new OppgittAnnenAktivitetDto(periode, arbeidType);
        }
    }

    private static class MapFraDto {

        private MapFraDto() {
            // Skjul konstruktør
        }

        public static OppgittOpptjeningBuilder map(OppgittOpptjeningDto dto) {
            if (dto == null)
                return null;

            var oppgittOpptjeningEksternReferanse = UUID.fromString(dto.getEksternReferanse().getReferanse());
            var builder = OppgittOpptjeningBuilder.ny(oppgittOpptjeningEksternReferanse, dto.getOpprettetTidspunkt());
            Optional.ofNullable(dto.getJournalpostId()).ifPresent(jp -> builder.medJournalpostId(new no.nav.ung.sak.typer.JournalpostId(jp.getId())));
            Optional.ofNullable(dto.getInnsendingstidspunkt()).ifPresent(tidspunkt -> builder.medInnsendingstidspunkt(tidspunkt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()));

            var annenAktivitet = mapEach(dto.getAnnenAktivitet(), MapFraDto::mapAnnenAktivitet);
            annenAktivitet.forEach(builder::leggTilAnnenAktivitet);

            var arbeidsforhold = mapEach(dto.getArbeidsforhold(), MapFraDto::mapOppgittArbeidsforhold);
            arbeidsforhold.forEach(builder::leggTilOppgittArbeidsforhold);

            var frilans = mapFrilans(dto.getFrilans());
            builder.leggTilFrilansOpplysninger(frilans);

            return builder;
        }

        private static <V, R> List<R> mapEach(List<V> data, Function<V, R> transform) {
            if (data == null) {
                return Collections.emptyList();
            }
            return data.stream().map(transform).collect(Collectors.toList());
        }

        private static OppgittFrilans mapFrilans(OppgittFrilansDto dto) {
            if (dto == null)
                return null;

            OppgittFrilansBuilder frilansBuilder = OppgittFrilansBuilder.ny();

            frilansBuilder.medErNyoppstartet(dto.isErNyoppstartet());
            frilansBuilder.medHarNærRelasjon(dto.isHarNærRelasjon());
            frilansBuilder.medHarInntektFraFosterhjem(dto.isHarInntektFraFosterhjem());

            var frilansoppdrag = mapEach(dto.getFrilansoppdrag(),
                f -> OppgittFrilansOppdragBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(f.getPeriode().getFom(), f.getPeriode().getTom()))
                    .medInntekt(f.getInntekt())
                    .medOppdragsgiver(fjernUnicodeControlOgAlternativeWhitespaceCharacters(f.getOppdragsgiver()))
                    .build());

            frilansBuilder.medFrilansOppdrag(frilansoppdrag);
            return frilansBuilder.build();
        }

        private static OppgittArbeidsforholdBuilder mapOppgittArbeidsforhold(OppgittArbeidsforholdDto dto) {
            if (dto == null)
                return null;

            Periode dto1 = dto.getPeriode();
            var builder = OppgittArbeidsforholdBuilder.ny()
                .medArbeidType(KodeverkMapper.mapArbeidType(dto.getArbeidTypeDto()))
                .medInntekt(dto.getInntekt())
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(dto1.getFom(), dto1.getTom()));

            return builder;
        }

        private static OppgittAnnenAktivitet mapAnnenAktivitet(OppgittAnnenAktivitetDto dto) {
            if (dto == null)
                return null;

            Periode dto1 = dto.getPeriode();
            var periode = DatoIntervallEntitet.fraOgMedTilOgMed(dto1.getFom(), dto1.getTom());
            var arbeidType = KodeverkMapper.mapArbeidType(dto.getArbeidTypeDto());
            return new OppgittAnnenAktivitet(periode, arbeidType);
        }

    }

    private static String fjernUnicodeControlOgAlternativeWhitespaceCharacters(String subject) {
        return subject != null ? subject.replaceAll("[\\p{C}\\h\\v&&[^ ]]", "X") : null;
    }

}
