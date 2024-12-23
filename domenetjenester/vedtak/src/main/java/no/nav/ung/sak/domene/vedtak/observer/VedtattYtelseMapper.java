package no.nav.ung.sak.domene.vedtak.observer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.abakus.vedtak.ytelse.Desimaltall;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.AnvistAndel;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.ArbeidsgiverIdent;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Inntektklasse;
import no.nav.ung.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.ung.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;

class VedtattYtelseMapper {


    static List<Anvisning> mapAnvisninger(BeregningsresultatEntitet beregningsresultatEntitet) {
        if (beregningsresultatEntitet == null) {
            return List.of();
        }
        return beregningsresultatEntitet.getBeregningsresultatPerioder().stream()
            .filter(periode -> periode.getDagsats() > 0)
            .map(p -> map(p))
            .collect(Collectors.toList());
    }

    private static Anvisning map(BeregningsresultatPeriode periode) {
        final Anvisning anvisning = new Anvisning();
        final Periode p = new Periode();
        p.setFom(periode.getBeregningsresultatPeriodeFom());
        p.setTom(periode.getBeregningsresultatPeriodeTom());
        anvisning.setPeriode(p);
        anvisning.setDagsats(new Desimaltall(new BigDecimal(periode.getDagsats())));
        anvisning.setUtbetalingsgrad(periode.getLavestUtbetalingsgrad().map(Desimaltall::new).orElse(null));
        anvisning.setAndeler(mapAndeler(periode.getBeregningsresultatAndelList()));
        return anvisning;
    }

    private static List<AnvistAndel> mapAndeler(List<BeregningsresultatAndel> beregningsresultatAndelList) {
        Map<AnvistAndelNøkkel, List<BeregningsresultatAndel>> resultatPrNøkkkel = beregningsresultatAndelList.stream()
            .collect(Collectors.groupingBy(a -> new AnvistAndelNøkkel(a.getArbeidsgiver().orElse(null), a.getArbeidsforholdRef(), a.getInntektskategori(), a.getAktivitetStatus())));
        return resultatPrNøkkkel.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(VedtattYtelseMapper::mapTilAnvistAndel)
            .collect(Collectors.toList());
    }

    private static AnvistAndel mapTilAnvistAndel(Map.Entry<AnvistAndelNøkkel, List<BeregningsresultatAndel>> e) {
        return new AnvistAndel(
            null,
            null,
            new Desimaltall(finnTotalBeløp(e.getValue())),
            finnUtbetalingsgrad(e.getValue()),
            new Desimaltall(finnRefusjonsgrad(e.getValue())),
            mapInntektklasse(e.getKey().getInntektskategori())
        );
    }

    private static Inntektklasse mapInntektklasse(Inntektskategori inntektskategori) {
        return switch(inntektskategori) {
            case ARBEIDSTAKER -> Inntektklasse.ARBEIDSTAKER;
            case ARBEIDSTAKER_UTEN_FERIEPENGER -> Inntektklasse.ARBEIDSTAKER_UTEN_FERIEPENGER;
            case FRILANSER -> Inntektklasse.FRILANSER;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> Inntektklasse.SELVSTENDIG_NÆRINGSDRIVENDE;
            case DAGPENGER -> Inntektklasse.DAGPENGER;
            case ARBEIDSAVKLARINGSPENGER -> Inntektklasse.ARBEIDSAVKLARINGSPENGER;
            case SJØMANN -> Inntektklasse.MARITIM;
            case DAGMAMMA -> Inntektklasse.DAGMAMMA;
            case JORDBRUKER -> Inntektklasse.JORDBRUKER;
            case FISKER -> Inntektklasse.FISKER;
            default -> Inntektklasse.INGEN;
        };
    }

    private static BigDecimal finnRefusjonsgrad(List<BeregningsresultatAndel> resultatAndeler) {
        var refusjon = resultatAndeler.stream()
            .filter(a -> !a.erBrukerMottaker())
            .map(BeregningsresultatAndel::getDagsats)
            .reduce(Integer::sum)
            .map(BigDecimal::valueOf)
            .orElse(BigDecimal.ZERO);

        var total = finnTotalBeløp(resultatAndeler);

        return total.compareTo(BigDecimal.ZERO) > 0 ?
            refusjon.multiply(BigDecimal.valueOf(100)).divide(total, 10, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
    }

    private static Desimaltall finnUtbetalingsgrad(List<BeregningsresultatAndel> resultatAndeler) {
        var utbetalingsgrader = resultatAndeler.stream().map(BeregningsresultatAndel::getUtbetalingsgrad)
            .collect(Collectors.toList());
        if (utbetalingsgrader.size() == 2) {
            if (!Objects.equals(utbetalingsgrader.get(0), utbetalingsgrader.get(1))) {
                throw new IllegalStateException("Forventet at utbetalingsgrad for samme nøkkel er like.");
            }
        } else if (utbetalingsgrader.size() != 1) {
            throw new IllegalStateException("Forventet å finne en eller to andeler. Fant " + utbetalingsgrader.size());
        }
        BigDecimal utbetalingsgrad = utbetalingsgrader.get(0) == null ? BigDecimal.ZERO : utbetalingsgrader.get(0);
        return new Desimaltall(utbetalingsgrad);
    }

    private static BigDecimal finnTotalBeløp(List<BeregningsresultatAndel> resultatAndeler) {
        return resultatAndeler.stream().map(BeregningsresultatAndel::getDagsats)
            .reduce(Integer::sum)
            .map(BigDecimal::valueOf)
            .orElse(BigDecimal.ZERO);
    }

    private static ArbeidsgiverIdent mapAktør(Arbeidsgiver arbeidsgiver) {
        return arbeidsgiver != null ? new ArbeidsgiverIdent(arbeidsgiver.getIdentifikator()) : null;
    }

    private static class AnvistAndelNøkkel implements Comparable<AnvistAndelNøkkel> {
        private final Arbeidsgiver arbeidsgiver;
        private final InternArbeidsforholdRef arbeidsforholdRef;
        private final Inntektskategori inntektskategori;

        private final AktivitetStatus aktivitetStatus;

        public AnvistAndelNøkkel(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, Inntektskategori inntektskategori, AktivitetStatus aktivitetStatus) {
            this.arbeidsgiver = arbeidsgiver;
            this.arbeidsforholdRef = arbeidsforholdRef;
            this.inntektskategori = inntektskategori;
            this.aktivitetStatus = aktivitetStatus;
        }

        public Arbeidsgiver getArbeidsgiver() {
            return arbeidsgiver;
        }

        public InternArbeidsforholdRef getArbeidsforholdRef() {
            return arbeidsforholdRef;
        }

        public Inntektskategori getInntektskategori() {
            return inntektskategori;
        }

        public AktivitetStatus getAktivitetStatus() {
            return aktivitetStatus;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnvistAndelNøkkel that = (AnvistAndelNøkkel) o;
            return Objects.equals(arbeidsgiver, that.arbeidsgiver) && Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef) && inntektskategori == that.inntektskategori && aktivitetStatus == that.aktivitetStatus;
        }

        @Override
        public int hashCode() {
            return Objects.hash(arbeidsgiver, arbeidsforholdRef, inntektskategori, aktivitetStatus);
        }

        @Override
        public int compareTo(AnvistAndelNøkkel o) {
            if (this.equals(o)) {
                return 0;
            }
            boolean arbeidsgiverErLik = Objects.equals(this.arbeidsgiver, o.getArbeidsgiver());
            if (arbeidsgiverErLik) {
                boolean arbeidsforholdRefErLik = Objects.equals(this.arbeidsforholdRef, o.getArbeidsforholdRef());
                if (arbeidsforholdRefErLik) {
                    return this.getInntektskategori().compareTo(o.getInntektskategori());
                }
                if (this.arbeidsforholdRef.getReferanse() != null && o.getArbeidsforholdRef().getReferanse() != null) {
                    return this.arbeidsforholdRef.getReferanse().compareTo(o.getArbeidsforholdRef().getReferanse());
                }
                return this.arbeidsforholdRef.getReferanse() != null ? 1 : -1;
            }
            if (this.getArbeidsgiver() != null && o.getArbeidsgiver() != null) {
                return this.getArbeidsgiver().getIdentifikator().compareTo(o.getArbeidsgiver().getIdentifikator());
            }
            return this.arbeidsgiver != null ? 1 : -1;
        }
    }


}
