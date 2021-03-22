package no.nav.k9.sak.domene.arbeidsforhold.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyrtePerioder;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.Permisjon;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsforholdAksjonspunktÅrsak;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsforholdIdDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseArbeidsforholdV2Dto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.MottattInntektsmeldingDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.PeriodeDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.PermisjonDto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Stillingsprosent;

class ArbeidsforholdMapper {

    private final Set<InntektArbeidYtelseArbeidsforholdV2Dto> result = new LinkedHashSet<>();
    private Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon;

    ArbeidsforholdMapper(Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        this.arbeidsforholdInformasjon = arbeidsforholdInformasjon;
    }

    void utledArbeidsforholdFraInntektsmeldinger(LinkedHashSet<Inntektsmelding> inntektsmeldinger) {
        class Mapper {
            void mapInntektsmeldingTilArbeidsforhold(Inntektsmelding im) {
                var dto = finnEllerOpprett(im);
                dto.leggTilInntektsmelding(new MottattInntektsmeldingDto(im.getJournalpostId(), im.getInnsendingstidspunkt(), DokumentStatus.GYLDIG, null));
                dto.leggTilKilde(ArbeidsforholdKilde.INNTEKTSMELDING);
            }

        }
        var m = new Mapper();
        inntektsmeldinger.forEach(im -> m.mapInntektsmeldingTilArbeidsforhold(im));
    }

    void utledArbeidsforholdFraYrkesaktivteter(Collection<Yrkesaktivitet> yrkesaktiviteter) {

        class Mapper {

            void mapYrkesaktivitet(Yrkesaktivitet yr) {
                var dto = finnEllerOpprett(yr.getArbeidsgiver(), yr.getArbeidsforholdRef());

                dto.leggTilKilde(ArbeidsforholdKilde.AAREGISTERET);
                dto.setAnsettelsesPerioder(mapAnsettelsesPerioder(yr.getAnsettelsesPeriode()));
                dto.setPermisjoner(mapPermisjoner(yr.getPermisjon()));
                dto.setStillingsprosent(yr.getStillingsprosentFor(LocalDate.now()).map(Stillingsprosent::getVerdi).orElse(BigDecimal.ZERO));
            }

            private Set<PeriodeDto> mapAnsettelsesPerioder(Collection<AktivitetsAvtale> ansettelsesPeriode) {
                return ansettelsesPeriode.stream()
                    .map(AktivitetsAvtale::getPeriode)
                    .map(it -> new PeriodeDto(it.getFomDato(), it.getTomDato()))
                    .collect(Collectors.toSet());
            }

            private List<PermisjonDto> mapPermisjoner(Collection<Permisjon> permisjon) {
                return permisjon.stream()
                    .map(it -> new PermisjonDto(it.getFraOgMed(), it.getTilOgMed(), it.getProsentsats().getVerdi(), it.getPermisjonsbeskrivelseType()))
                    .collect(Collectors.toList());
            }

        }
        var m = new Mapper();

        yrkesaktiviteter.forEach(yr -> m.mapYrkesaktivitet(yr));

    }

    void utledArbeidsforholdFraArbeidsforholdInformasjon(Collection<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {

        class Mapper {

            void mapOverstyring(ArbeidsforholdOverstyring overstyring) {
                var dto = finnEllerOpprett(overstyring.getArbeidsgiver(), overstyring.getArbeidsforholdRef());
                if (Set.of(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING, ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER).contains(overstyring.getHandling())) {
                    dto.leggTilKilde(ArbeidsforholdKilde.SAKSBEHANDLER);
                    dto.setStillingsprosent(overstyring.getStillingsprosent().getVerdi());
                    dto.setAnsettelsesPerioder(mapAnsettelsesPerioder(overstyring.getArbeidsforholdOverstyrtePerioder()));
                }
                dto.setHandlingType(overstyring.getHandling());
                dto.setBegrunnelse(overstyring.getBegrunnelse());
            }

            private Set<PeriodeDto> mapAnsettelsesPerioder(List<ArbeidsforholdOverstyrtePerioder> arbeidsforholdOverstyrtePerioder) {
                return arbeidsforholdOverstyrtePerioder.stream()
                    .map(it -> new PeriodeDto(it.getOverstyrtePeriode().getFomDato(), it.getOverstyrtePeriode().getTomDato()))
                    .collect(Collectors.toSet());
            }

        }

        var m = new Mapper();
        arbeidsforholdOverstyringer.forEach(overstyring -> m.mapOverstyring(overstyring));
    }

    void mapVurdering(Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> arbeidsgiverÅrsaker) {

        var vurderinger = arbeidsgiverÅrsaker
            .entrySet()
            .stream()
            .filter(it -> it.getValue()
                .stream()
                .anyMatch(at -> !at.getÅrsaker().isEmpty()))
            .distinct()
            .collect(Collectors.toList());

        vurderinger.stream().forEach(vurd -> {
            vurd.getValue().forEach(af -> {
                var dto = finnEllerOpprett(vurd.getKey(), af.getRef());
                dto.setAksjonspunktÅrsaker(af.getÅrsaker().stream().map(it -> ArbeidsforholdAksjonspunktÅrsak.fraKode(it.name())).collect(Collectors.toSet()));
            });
        });
    }

    private ArbeidsforholdIdDto mapArbeidsforholdsId(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        return new ArbeidsforholdIdDto(arbeidsforholdRef.getUUIDReferanse(),
            arbeidsforholdInformasjon.map(it -> it.finnEkstern(arbeidsgiver, arbeidsforholdRef)).map(EksternArbeidsforholdRef::getReferanse).orElse(null));
    }

    private InntektArbeidYtelseArbeidsforholdV2Dto finnEllerOpprett(Arbeidsgiver arbeidsgiver,
                                                                    InternArbeidsforholdRef arbeidsforholdRef) {
        Objects.requireNonNull(arbeidsgiver, "arbeidsgiver");

        if (!arbeidsforholdRef.gjelderForSpesifiktArbeidsforhold()) {
            // ignorer alle spesifikke hvis finnes?
            var fjernSpesifikke = result.stream()
                .filter(it -> gjelderSammeArbeidsforhold(it, arbeidsgiver, arbeidsforholdRef)
                    && InternArbeidsforholdRef.ref(it.getArbeidsforhold().getInternArbeidsforholdId()).gjelderForSpesifiktArbeidsforhold())
                .collect(Collectors.toCollection(LinkedHashSet::new));

            result.removeAll(fjernSpesifikke);
        }

        var collect = result.stream()
            .filter(it -> gjelderSammeArbeidsforhold(it, arbeidsgiver, arbeidsforholdRef))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (collect.isEmpty()) {
            var dto = new InntektArbeidYtelseArbeidsforholdV2Dto(arbeidsgiver, mapArbeidsforholdsId(arbeidsgiver, arbeidsforholdRef, arbeidsforholdInformasjon));
            result.add(dto);
            return dto;
        }
        if (collect.size() > 1) {
            throw new IllegalStateException("Flere arbeidsforhold med samme nøkkel for [" + arbeidsforholdRef + "], kan ikke forekomme, men fant " + collect + ", blant:" + result);
        }
        return collect.iterator().next();
    }

    private InntektArbeidYtelseArbeidsforholdV2Dto finnEllerOpprett(Inntektsmelding im) {
        InternArbeidsforholdRef arbeidsforholdRef = im.getArbeidsforholdRef();

        var collect = result.stream()
            .filter(it -> gjelderSammeArbeidsforhold(it, im.getArbeidsgiver(), arbeidsforholdRef))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (collect.isEmpty()) {
            var dto = new InntektArbeidYtelseArbeidsforholdV2Dto(im.getArbeidsgiver(),
                new ArbeidsforholdIdDto(arbeidsforholdRef.getUUIDReferanse(), im.getEksternArbeidsforholdRef().map(EksternArbeidsforholdRef::getReferanse).orElse(null)));
            result.add(dto);
            return dto;
        }
        if (collect.size() > 1) {
            throw new IllegalStateException("Flere arbeidsforhold med samme nøkkel for [" + arbeidsforholdRef + "], kan ikke forekomme, men fant " + collect + ", blant:" + result);
        }
        return collect.iterator().next();
    }

    private boolean gjelderSammeArbeidsforhold(InntektArbeidYtelseArbeidsforholdV2Dto it, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        return it.getArbeidsgiver().getIdentifikator().equals(arbeidsgiver.getIdentifikator()) &&
            arbeidsforholdRef.gjelderFor(InternArbeidsforholdRef.ref(it.getArbeidsforhold().getInternArbeidsforholdId()));
    }

    public Set<InntektArbeidYtelseArbeidsforholdV2Dto> getArbeidsforhold() {
        return result;
    }

    public boolean harArbeidsforhold() {
        return !getArbeidsforhold().isEmpty();
    }

}
