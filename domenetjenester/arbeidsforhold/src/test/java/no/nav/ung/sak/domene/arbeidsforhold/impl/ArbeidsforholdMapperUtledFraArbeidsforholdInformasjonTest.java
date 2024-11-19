package no.nav.ung.sak.domene.arbeidsforhold.impl;

import static no.nav.ung.sak.domene.arbeidsforhold.impl.ArbeidsforholdMapperUtledFraArbeidsforholdInformasjonTest.GenererArbinfoArbeidsforhold.overstyringer;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.ung.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.arbeidsforhold.ArbeidsforholdIdDto;
import no.nav.ung.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseArbeidsforholdV2Dto;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.EksternArbeidsforholdRef;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;
import no.nav.ung.sak.typer.Stillingsprosent;

public class ArbeidsforholdMapperUtledFraArbeidsforholdInformasjonTest {

    private static final String ET_ORGNR = "60";

    private GenererArbinfoArbeidsforhold generator = new GenererArbinfoArbeidsforhold();

    @Test
    void utled_arbeidsforhold_fra_overstyringer() throws Exception {

        var internArbUten1 = generator.arbeidsforholdUten();
        var internArbMed1 = generator.arbeidsforholdMed();

        var overstyringUten = generator.overstyringLagtTil(internArbUten1);
        var overstyringMed = generator.overstyringLagtTil(internArbMed1);
        var mapper = new ArbeidsforholdMapper(generator.arbeidsforholdInformasjon());
        mapper.utledArbeidsforholdFraArbeidsforholdInformasjon(overstyringer(overstyringUten, overstyringMed));

        List<Arbeidsgiver> arbeidsgivere = mapper.getArbeidsforhold().stream().map(InntektArbeidYtelseArbeidsforholdV2Dto::getArbeidsgiver).collect(Collectors.toList());
        assertThat(arbeidsgivere).hasSize(2).allSatisfy(a -> assertThat(a.getArbeidsgiverOrgnr()).as("sjekk orgnr").isEqualTo(ET_ORGNR));

        // sjekk vi har fått med begge arbeidsforhold (med og uten ekstern referanse)
        List<ArbeidsforholdIdDto> arbeidsforhold = mapper.getArbeidsforhold().stream().map(InntektArbeidYtelseArbeidsforholdV2Dto::getArbeidsforhold).collect(Collectors.toList());
        assertThat(arbeidsforhold).hasSize(2);
        assertThat(arbeidsforhold).anySatisfy(a -> {
            assertThat(a.getInternArbeidsforholdId()).isEqualTo(internArbUten1.getUUIDReferanse());
            assertThat(a.getEksternArbeidsforholdId()).isNull();
        });
        assertThat(arbeidsforhold).anySatisfy(a -> {
            assertThat(a.getInternArbeidsforholdId()).isEqualTo(internArbMed1.getUUIDReferanse());
            assertThat(a.getEksternArbeidsforholdId()).isNotNull();
        });

    }

    static class GenererArbinfoArbeidsforhold {

        private static final Arbeidsgiver VIRKSOMHET = Arbeidsgiver.virksomhet(ET_ORGNR);

        private final AtomicInteger counter = new AtomicInteger(0);

        private Stillingsprosent defaultStillingsprosent = new Stillingsprosent(BigDecimal.valueOf(100));
        private ArbeidsforholdHandlingType defaultHandling = ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER;

        private LocalDate fom = LocalDate.now().minusDays(100);
        private LocalDate tom = fom.plusDays(100);

        private Arbeidsgiver virksomhet;

        private final ArbeidsforholdInformasjonBuilder arbInfo;

        GenererArbinfoArbeidsforhold() {
            this(VIRKSOMHET);
        }

        GenererArbinfoArbeidsforhold(Arbeidsgiver virksomhet) {
            this(virksomhet, ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty()));
        }

        GenererArbinfoArbeidsforhold(Arbeidsgiver virksomhet, ArbeidsforholdInformasjonBuilder arbInfo) {
            this.virksomhet = virksomhet;
            this.arbInfo = arbInfo;
        }

        GenererArbinfoArbeidsforhold(ArbeidsforholdInformasjonBuilder arbInfo) {
            this(VIRKSOMHET, arbInfo);
        }

        InternArbeidsforholdRef arbeidsforholdUten() {
            return arbeidsforholdUten(VIRKSOMHET);
        }

        InternArbeidsforholdRef arbeidsforholdUten(Arbeidsgiver arbeidsgiver) {
            InternArbeidsforholdRef internRef = InternArbeidsforholdRef.nullRef();
            arbeidsforhold(arbeidsgiver, internRef, null);
            return internRef;
        }

        InternArbeidsforholdRef arbeidsforholdMed() {
            return arbeidsforholdMed(VIRKSOMHET);
        }

        InternArbeidsforholdRef arbeidsforholdMed(Arbeidsgiver arbeidsgiver) {
            EksternArbeidsforholdRef eksternRef = EksternArbeidsforholdRef.ref("arbInfo-ref-" + counter.incrementAndGet());
            return arbeidsforholdMed(arbeidsgiver, eksternRef);
        }

        InternArbeidsforholdRef arbeidsforholdMed(Arbeidsgiver arbeidsgiver, EksternArbeidsforholdRef eksternRef) {
            InternArbeidsforholdRef internRef = InternArbeidsforholdRef.nyRef();
            arbeidsforhold(arbeidsgiver, internRef, eksternRef);
            return internRef;
        }

        InternArbeidsforholdRef arbeidsforhold(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internRef, EksternArbeidsforholdRef eksternRef) {
            arbInfo.leggTilNyReferanse(new ArbeidsforholdReferanse(arbeidsgiver, internRef, eksternRef));
            return internRef;
        }

        ArbeidsforholdInformasjon arbeidsforholdInformasjon() {
            return arbeidsforholdInformasjon(null);
        }

        ArbeidsforholdInformasjon arbeidsforholdInformasjon(ArbeidsforholdOverstyring overstyring) {
            if (overstyring != null) {
                arbInfo.leggTil(overstyring);
            }
            return arbInfo.build();
        }

        ArbeidsforholdOverstyring overstyringLagtTil(InternArbeidsforholdRef internRef) {
            return overstyringLagtTil(virksomhet, internRef, fom, tom);
        }

        ArbeidsforholdOverstyring overstyringLagtTil(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internRef) {
            return overstyringLagtTil(arbeidsgiver, internRef, fom, tom);
        }

        ArbeidsforholdOverstyring overstyringLagtTil(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internRef, LocalDate fom, LocalDate tom) {
            DatoIntervallEntitet overstyrtPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
            return overstyringLagtTil(arbeidsgiver, internRef, overstyrtPeriode, defaultStillingsprosent);
        }

        ArbeidsforholdOverstyring overstyringLagtTil(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internRef, DatoIntervallEntitet overstyrtPeriode, Stillingsprosent stillingsprosent) {
            return overstyring(arbeidsgiver, internRef, defaultHandling, overstyrtPeriode, stillingsprosent);
        }

        ArbeidsforholdOverstyring overstyring(Arbeidsgiver arbeidsgiver,
                                              InternArbeidsforholdRef internRef,
                                              ArbeidsforholdHandlingType handlingType,
                                              DatoIntervallEntitet overstyrtPeriode,
                                              Stillingsprosent stillingsprosent) {
            ArbeidsforholdOverstyringBuilder overstyringBuilder = ArbeidsforholdOverstyringBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(virksomhet)
                .medHandling(handlingType)
                .medArbeidsgiver(Objects.requireNonNull(arbeidsgiver))
                .medArbeidsforholdRef(Objects.requireNonNull(internRef));
            if (overstyrtPeriode != null) {
                overstyringBuilder.leggTilOverstyrtPeriode(overstyrtPeriode);
            }
            if (stillingsprosent != null) {
                overstyringBuilder.medAngittStillingsprosent(stillingsprosent);
            }

            var overstyring = overstyringBuilder.build();
            return overstyring;
        }

        static List<ArbeidsforholdOverstyring> overstyringer(ArbeidsforholdOverstyring... ovs) {
            return List.of(ovs);
        }

        public ArbeidsforholdOverstyring overstyring(Arbeidsgiver arbeidsgiver, ArbeidsforholdHandlingType handlingType) {
            return overstyring(arbeidsgiver, InternArbeidsforholdRef.nullRef(), handlingType, DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), defaultStillingsprosent);
        }
    }

}
