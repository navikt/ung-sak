package no.nav.k9.sak.domene.abakus.mapping;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.abakus.iaygrunnlag.Organisasjon;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.kodeverk.Landkode;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittAnnenAktivitetDto;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittArbeidsforholdDto;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittEgenNæringDto;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittFrilansDto;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittFrilansoppdragDto;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittOpptjeningDto;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.sak.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.k9.sak.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.k9.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittUtenlandskVirksomhet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.OrgNummer;

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

    OppgittOpptjeningBuilder mapFraDto(OppgittOpptjeningDto oppgittOpptjening) {
        return MapFraDto.map(oppgittOpptjening);
    }

    private static class MapTilDto {

        private MapTilDto() {
            // Skjul
        }

        private static OppgittOpptjeningDto map(OppgittOpptjening oppgittOpptjening) {
            if (oppgittOpptjening == null)
                return null;
            if (!oppgittOpptjening.harOpptjening())
                return null;

            var dto = new OppgittOpptjeningDto(oppgittOpptjening.getEksternReferanse(), oppgittOpptjening.getOpprettetTidspunkt());

            dto.medArbeidsforhold(oppgittOpptjening.getOppgittArbeidsforhold().stream().map(MapTilDto::mapArbeidsforhold).sorted(COMP_OPPGITT_ARBEIDSFORHOLD)
                .collect(Collectors.toList()));
            dto.medEgenNæring(
                oppgittOpptjening.getEgenNæring().stream().map(MapTilDto::mapEgenNæring).sorted(COMP_OPPGITT_EGEN_NÆRING).collect(Collectors.toList()));
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
            Optional.ofNullable(frilans.getErNyoppstartet()).ifPresent(frilansDto::medErNyoppstartet);
            Optional.ofNullable(frilans.getHarInntektFraFosterhjem()).ifPresent(frilansDto::medHarInntektFraFosterhjem);
            Optional.ofNullable(frilans.getHarNærRelasjon()).ifPresent(frilansDto::medHarNærRelasjon);
            return frilansDto;
        }

        private static OppgittArbeidsforholdDto mapArbeidsforhold(OppgittArbeidsforhold arbeidsforhold) {
            if (arbeidsforhold == null)
                return null;

            DatoIntervallEntitet periode1 = arbeidsforhold.getPeriode();
            var periode = new Periode(periode1.getFomDato(), periode1.getTomDato());
            var arbeidType = KodeverkMapper.mapArbeidTypeTilDto(arbeidsforhold.getArbeidType());

            var dto = new OppgittArbeidsforholdDto(periode, arbeidType)
                .medErUtenlandskInntekt(arbeidsforhold.erUtenlandskInntekt());

            var virksomhet = arbeidsforhold.getUtenlandskVirksomhet();
            if (virksomhet != null) {
                var landKode = virksomhet.getLandkode() == null ? Landkode.NOR : Landkode.fraKode(virksomhet.getLandkode().getKode());
                if (virksomhet.getNavn() != null) {
                    dto.medOppgittVirksomhetNavn(fjernUnicodeControlOgAlternativeWhitespaceCharacters(virksomhet.getNavn()), landKode);
                } else {
                    dto.setLandkode(landKode);
                }
            } else {
                dto.setLandkode(Landkode.NOR);
            }

            return dto;
        }

        private static OppgittEgenNæringDto mapEgenNæring(OppgittEgenNæring egenNæring) {
            if (egenNæring == null)
                return null;

            var periode = new Periode(egenNæring.getPeriode().getFomDato(), egenNæring.getPeriode().getTomDato());

            var org = egenNæring.getOrgnr() == null ? null : new Organisasjon(egenNæring.getOrgnr());
            var virksomhetType = egenNæring.getVirksomhetType();

            var dto = new OppgittEgenNæringDto(periode)
                .medBegrunnelse(egenNæring.getBegrunnelse())
                .medBruttoInntekt(minMax(egenNæring.getBruttoInntekt(), BigDecimal.ZERO, null))
                .medEndringDato(egenNæring.getEndringDato())
                .medRegnskapsførerNavn(fjernUnicodeControlOgAlternativeWhitespaceCharacters(egenNæring.getRegnskapsførerNavn()))
                .medRegnskapsførerTlf(fjernUnicodeControlOgAlternativeWhitespaceCharacters(egenNæring.getRegnskapsførerTlf()))
                .medVirksomhet(org)
                .medVirksomhetType(virksomhetType);

            Optional.ofNullable(egenNæring.getNyIArbeidslivet()).ifPresent(dto::medNyIArbeidslivet);
            Optional.ofNullable(egenNæring.getNyoppstartet()).ifPresent(dto::medNyoppstartet);
            Optional.ofNullable(egenNæring.getNærRelasjon()).ifPresent(dto::medNærRelasjon);
            Optional.ofNullable(egenNæring.getVarigEndring()).ifPresent(dto::medVarigEndring);

            var virksomhet = egenNæring.getVirksomhet();
            if (virksomhet != null) {
                var landkode = virksomhet.getLandkode() == null ? Landkode.NOR : Landkode.fraKode(virksomhet.getLandkode().getKode());
                var navn = virksomhet.getNavn();
                if (navn != null) {
                    dto.medOppgittVirksomhetNavn(fjernUnicodeControlOgAlternativeWhitespaceCharacters(navn), landkode);
                } else {
                    dto.setLandkode(landkode);
                }
            } else {
                dto.setLandkode(Landkode.NOR);
            }

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

            var annenAktivitet = mapEach(dto.getAnnenAktivitet(), MapFraDto::mapAnnenAktivitet);
            annenAktivitet.forEach(builder::leggTilAnnenAktivitet);

            var arbeidsforhold = mapEach(dto.getArbeidsforhold(), MapFraDto::mapOppgittArbeidsforhold);
            arbeidsforhold.forEach(builder::leggTilOppgittArbeidsforhold);

            var egenNæring = mapEach(dto.getEgenNæring(), MapFraDto::mapEgenNæring);
            builder.leggTilEgneNæringer(egenNæring);

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

            frilansBuilder.leggTilOppgittOppdrag(frilansoppdrag);
            return frilansBuilder.build();
        }

        private static EgenNæringBuilder mapEgenNæring(OppgittEgenNæringDto dto) {
            if (dto == null)
                return null;

            var builder = EgenNæringBuilder.ny();

            var org = dto.getVirksomhet() == null ? null : new OrgNummer(dto.getVirksomhet().getIdent());
            var periode = dto.getPeriode();

            var virksomhet = tilUtenlandskVirksomhet(dto);
            builder
                .medBegrunnelse(dto.getBegrunnelse())
                .medBruttoInntekt(dto.getBruttoInntekt())
                .medEndringDato(dto.getEndringDato())
                .medUtenlandskVirksomhet(virksomhet)
                .medVirksomhet(org)
                .medVirksomhetType(dto.getVirksomhetTypeDto())
                .medRegnskapsførerNavn(dto.getRegnskapsførerNavn())
                .medRegnskapsførerTlf(dto.getRegnskapsførerTlf())
                .medNyIArbeidslivet(dto.isNyIArbeidslivet())
                .medNyoppstartet(dto.isNyoppstartet())
                .medNærRelasjon(dto.isNærRelasjon())
                .medVarigEndring(dto.isVarigEndring())
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()));

            return builder;
        }

        private static OppgittArbeidsforholdBuilder mapOppgittArbeidsforhold(OppgittArbeidsforholdDto dto) {
            if (dto == null)
                return null;

            Periode dto1 = dto.getPeriode();
            var virksomhet = tilUtenlandskVirksomhet(dto);
            var builder = OppgittArbeidsforholdBuilder.ny()
                .medArbeidType(KodeverkMapper.mapArbeidType(dto.getArbeidTypeDto()))
                .medErUtenlandskInntekt(dto.isErUtenlandskInntekt())
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(dto1.getFom(), dto1.getTom()))
                .medUtenlandskVirksomhet(virksomhet);

            return builder;
        }

        private static OppgittUtenlandskVirksomhet tilUtenlandskVirksomhet(OppgittArbeidsforholdDto dto) {
            if (dto == null)
                return null;

            var landkode = dto.getLandkode() == null ? null : Landkoder.fraKode(dto.getLandkode().getKode());
            return new OppgittUtenlandskVirksomhet(landkode, fjernUnicodeControlOgAlternativeWhitespaceCharacters(dto.getVirksomhetNavn()));
        }

        private static OppgittUtenlandskVirksomhet tilUtenlandskVirksomhet(OppgittEgenNæringDto dto) {
            if (dto == null)
                return null;

            var landkode = dto.getLandkode() == null ? null : Landkoder.fraKode(dto.getLandkode().getKode());
            return new OppgittUtenlandskVirksomhet(landkode, fjernUnicodeControlOgAlternativeWhitespaceCharacters(dto.getVirksomhetNavn()));
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
