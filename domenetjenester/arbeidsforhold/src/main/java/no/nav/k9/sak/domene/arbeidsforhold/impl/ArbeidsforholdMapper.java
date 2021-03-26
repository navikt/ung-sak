package no.nav.k9.sak.domene.arbeidsforhold.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
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
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
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

    ArbeidsforholdMapper(ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        this.arbeidsforholdInformasjon = Optional.ofNullable(arbeidsforholdInformasjon);
    }

    void utledArbeidsforholdFraInntektsmeldinger(NavigableSet<Inntektsmelding> inntektsmeldinger) {

        FinnMatchArbeidsforhold matchArbeidsforhold = new FinnMatchArbeidsforhold(this.result);
        class Mapper {

            void mapInntektsmeldingTilArbeidsforhold(Inntektsmelding im) {
                var dtos = matchArbeidsforhold.finnEllerOpprett(im);
                for (var dto : dtos) {
                    dto.leggTilInntektsmelding(new MottattInntektsmeldingDto(im.getJournalpostId(), im.getInnsendingstidspunkt(), DokumentStatus.GYLDIG, null));
                    dto.leggTilKilde(ArbeidsforholdKilde.INNTEKTSMELDING);
                    result.add(dto);
                }
            }

        }
        var m = new Mapper();

        // ta alle spesifikke først
        inntektsmeldinger.stream().filter(im -> im.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold()).forEach(im -> m.mapInntektsmeldingTilArbeidsforhold(im));

        // suppler med overstyrende arbeidsforhold
        inntektsmeldinger.stream().filter(im -> !(im.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold())).forEach(im -> m.mapInntektsmeldingTilArbeidsforhold(im));

    }

    void utledArbeidsforholdFraYrkesaktiviteter(Collection<Yrkesaktivitet> yrkesaktiviteter) {

        FinnEksaktArbeidsforhold finnEksaktArbeidsforhold = new FinnEksaktArbeidsforhold(this.result, this.arbeidsforholdInformasjon);

        class Mapper {

            void mapYrkesaktivitet(Yrkesaktivitet yr) {
                var dto = finnEksaktArbeidsforhold.finnEllerOpprett(yr.getArbeidsgiver(), yr.getArbeidsforholdRef());
                result.add(dto);
                dto.leggTilKilde(ArbeidsforholdKilde.AAREGISTERET);
                dto.setAnsettelsesPerioder(mapAnsettelsesPerioder(yr.getAnsettelsesPeriode()));
                dto.setPermisjoner(mapPermisjoner(yr.getPermisjon()));
                dto.setStillingsprosent(yr.getStillingsprosentFor(LocalDate.now()).map(Stillingsprosent::getVerdi).orElse(BigDecimal.ZERO));
            }

            private Set<PeriodeDto> mapAnsettelsesPerioder(Collection<AktivitetsAvtale> ansettelsesPeriode) {
                return ansettelsesPeriode.stream()
                    .map(AktivitetsAvtale::getPeriode)
                    .map(avtalePeriode -> DatoIntervallEntitet.fraOgMedTilOgMed(avtalePeriode.getFomDato(), avtalePeriode.getTomDato()))
                    .sorted()
                    .map(it -> new PeriodeDto(it.getFomDato(), it.getTomDato()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
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

        FinnEksaktArbeidsforhold finnEksaktArbeidsforhold = new FinnEksaktArbeidsforhold(this.result, this.arbeidsforholdInformasjon);

        class Mapper {

            void mapOverstyring(ArbeidsforholdOverstyring overstyring) {
                var dto = finnEksaktArbeidsforhold.finnEllerOpprett(overstyring.getArbeidsgiver(), overstyring.getArbeidsforholdRef());
                result.add(dto);
                var handling = overstyring.getHandling();
                String begrunnelse = overstyring.getBegrunnelse();

                if (Set.of(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING, ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER).contains(handling)) {
                    dto.leggTilKilde(ArbeidsforholdKilde.SAKSBEHANDLER);
                    var stillingsprosent = overstyring.getStillingsprosent();
                    dto.setStillingsprosent(stillingsprosent.getVerdi());
                    dto.setAnsettelsesPerioder(mapAnsettelsesPerioder(overstyring.getArbeidsforholdOverstyrtePerioder()));
                } else {
                    throw new UnsupportedOperationException("Kan ikke mappe overstyring:" + overstyring + ", {begrunnelse=" + begrunnelse + "} til en ArbeidsforholdKilde");
                }
                dto.setHandlingType(handling);
                dto.setBegrunnelse(begrunnelse);
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
        var finnEksaktArbeidsforhold = new FinnEksaktArbeidsforhold(this.result, null);
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
                var dto = finnEksaktArbeidsforhold.finn(vurd.getKey(), af.getRef());
                if (dto.isPresent()) {
                    var aksjonspunktÅrsaker = af.getÅrsaker().stream().map(it -> ArbeidsforholdAksjonspunktÅrsak.fraKode(it.name())).collect(Collectors.toCollection(LinkedHashSet::new));
                    dto.get().setAksjonspunktÅrsaker(aksjonspunktÅrsaker);
                } else {
                    // inkonsistens i data - noe som er fjernet?
                    throw new IllegalStateException(
                        String.format("Inkonsistens i informasjonsmodell: Fant ingen arbeidsforhold for angitt vurdering [%s, arbeidsforholdMedÅrsak=%s], blant: %s", vurd.getKey(), af, result));
                }
            });
        });
    }

    public Set<InntektArbeidYtelseArbeidsforholdV2Dto> getArbeidsforhold() {
        return Collections.unmodifiableSet(result);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "<result=" + result +
            (arbeidsforholdInformasjon.isPresent() ? ", arbeidsforholdInformasjon=" + arbeidsforholdInformasjon : "") +
            ">";
    }

    public boolean harArbeidsforhold() {
        return !getArbeidsforhold().isEmpty();
    }

    static class FinnEksaktArbeidsforhold {

        private final Set<InntektArbeidYtelseArbeidsforholdV2Dto> input;
        private Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon;

        FinnEksaktArbeidsforhold(Set<InntektArbeidYtelseArbeidsforholdV2Dto> input, Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
            this.input = Collections.unmodifiableSet(input);
            this.arbeidsforholdInformasjon = arbeidsforholdInformasjon;

        }

        InntektArbeidYtelseArbeidsforholdV2Dto finnEllerOpprett(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
            Objects.requireNonNull(arbeidsgiver, "arbeidsgiver");
            var collect = finn(arbeidsgiver, arbeidsforholdRef);
            if (collect.isEmpty()) {
                var dto = new InntektArbeidYtelseArbeidsforholdV2Dto(arbeidsgiver, mapArbeidsforholdsId(arbeidsgiver, arbeidsforholdRef, Objects.requireNonNull(arbeidsforholdInformasjon)));
                return dto;
            } else {
                return collect.get();
            }
        }

        Optional<InntektArbeidYtelseArbeidsforholdV2Dto> finn(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
            Objects.requireNonNull(arbeidsgiver, "arbeidsgiver");

            var collect = input.stream()
                .filter(it -> gjelderEksaktArbeidsforhold(it, arbeidsgiver, arbeidsforholdRef))
                .collect(Collectors.toCollection(LinkedHashSet::new));
            if (collect.size() > 1) {
                throw new IllegalStateException("Flere arbeidsforhold med samme nøkkel for [" + arbeidsforholdRef + "], kan ikke forekomme, men fant " + collect + ", blant:" + input);
            } else if (collect.size() == 1) {
                return Optional.of(collect.iterator().next());
            } else {
                return Optional.empty();
            }
        }

        private boolean gjelderEksaktArbeidsforhold(InntektArbeidYtelseArbeidsforholdV2Dto it, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
            return it.getArbeidsgiver().getIdentifikator().equals(arbeidsgiver.getIdentifikator()) &&
                Objects.equals(arbeidsforholdRef, InternArbeidsforholdRef.ref(it.getArbeidsforhold().getInternArbeidsforholdId()));
        }

        private ArbeidsforholdIdDto mapArbeidsforholdsId(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
            return new ArbeidsforholdIdDto(arbeidsforholdRef.getUUIDReferanse(),
                arbeidsforholdInformasjon.map(it -> it.finnEkstern(arbeidsgiver, arbeidsforholdRef)).map(EksternArbeidsforholdRef::getReferanse).orElse(null));
        }

    }

    static class FinnMatchArbeidsforhold {
        private final Set<InntektArbeidYtelseArbeidsforholdV2Dto> input;

        FinnMatchArbeidsforhold(Set<InntektArbeidYtelseArbeidsforholdV2Dto> input) {
            this.input = Collections.unmodifiableSet(input);

        }

        Set<InntektArbeidYtelseArbeidsforholdV2Dto> finnEllerOpprett(Inntektsmelding im) {
            var angitt = im.getArbeidsforholdRef();
            var collect = input.stream()
                .filter(it -> {
                    boolean match = gjelderSammeArbeidsforhold(it, im.getArbeidsgiver(), angitt);
                    var fraListe = InternArbeidsforholdRef.ref(it.getArbeidsforhold().getInternArbeidsforholdId());
                    return match && (fraListe.gjelderForSpesifiktArbeidsforhold() /* i listen må være spesifikt */
                        || !(angitt.gjelderForSpesifiktArbeidsforhold()) /* angitt er ikke spesifikt */
                    );
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));

            var utvalgt = new LinkedHashSet<InntektArbeidYtelseArbeidsforholdV2Dto>();

            if (collect.isEmpty()) {
                var dto = nyDto(im, angitt);
                utvalgt.add(dto);
            } else {
                utvalgt.addAll(collect);

                if (!im.gjelderForEtSpesifiktArbeidsforhold()) {
                    boolean finnerIngenEksaktMatchende = new FinnEksaktArbeidsforhold(collect, Optional.empty()).finn(im.getArbeidsgiver(), im.getArbeidsforholdRef()).isEmpty();
                    if (finnerIngenEksaktMatchende) {
                        var dto = nyDto(im, angitt);
                        utvalgt.add(dto);
                    }
                }

            }

            return utvalgt;
        }

        private InntektArbeidYtelseArbeidsforholdV2Dto nyDto(Inntektsmelding im, InternArbeidsforholdRef angitt) {
            var dto = new InntektArbeidYtelseArbeidsforholdV2Dto(im.getArbeidsgiver(),
                new ArbeidsforholdIdDto(angitt.getUUIDReferanse(), im.getEksternArbeidsforholdRef().map(EksternArbeidsforholdRef::getReferanse).orElse(null)));
            return dto;
        }

        private boolean gjelderSammeArbeidsforhold(InntektArbeidYtelseArbeidsforholdV2Dto it, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
            return it.getArbeidsgiver().getIdentifikator().equals(arbeidsgiver.getIdentifikator()) &&
                arbeidsforholdRef.gjelderFor(InternArbeidsforholdRef.ref(it.getArbeidsforhold().getInternArbeidsforholdId()));
        }
    }

}
