package no.nav.foreldrepenger.ytelse.beregning.psb;

import static java.util.stream.Collectors.toSet;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatusPeriode;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.ytelse.beregning.UttakResultatRepoMapper;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.vedtak.felles.jpa.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PsbUttakResultatRepoMapper implements UttakResultatRepoMapper {
    // FIXME K9 Dette er en mockimplementasjon for å komme videre med verdikjedetest. Implementasjon må på plass når regler for uttak er klart.

    PsbUttakResultatRepoMapper() {
        //for proxy
    }

    private static Function<Map.Entry<DatoIntervallEntitet, Set<BeregningsgrunnlagStatusPeriode>>, UttakResultatPeriode> toUttakResultatPeriode() {
        return entry -> new UttakResultatPeriode(
            entry.getKey().getFomDato(),
            entry.getKey().getTomDato(),
            toUttakAktiviteter(entry.getValue()),
            true
        );
    }

    private static List<UttakAktivitet> toUttakAktiviteter(Set<BeregningsgrunnlagStatusPeriode> beregningsgrunnlagStatusPeriode) {
        // Arbeidsforhold arbeidsforhold, AktivitetStatus aktivitetStatus, boolean erGradering) {
        return beregningsgrunnlagStatusPeriode.stream()
            .map(PsbUttakResultatRepoMapper::mapTilUttaksAktiviteter)
            .collect(Collectors.toList());
    }

    private static UttakAktivitet mapTilUttaksAktiviteter(BeregningsgrunnlagStatusPeriode beregningsgrunnlagStatusPeriode) {
        final BigDecimal stillingsgrad = BigDecimal.valueOf(1);
        final BigDecimal utbetalingsgrad = BigDecimal.valueOf(1);

        final Optional<Arbeidsgiver> arbeidsgiver = beregningsgrunnlagStatusPeriode.getArbeidsgiver();
        final Arbeidsforhold.Builder arbeidsforholdBuilder = Arbeidsforhold.builder();
        arbeidsgiver.ifPresent(a -> {
            if (a.getOrgnr() != null) {
                arbeidsforholdBuilder.medOrgnr(a.getOrgnr());
            } else {
                arbeidsforholdBuilder.medAktørId(a.getAktørId().getId());
            }
        });

        final Arbeidsforhold arbeidsforhold = arbeidsforholdBuilder.build();
        final AktivitetStatus aktivitetStatus = AktivitetStatus.ATFL;
        final boolean erGradering = false;
        return new UttakAktivitet(stillingsgrad, utbetalingsgrad, arbeidsforhold, aktivitetStatus, erGradering);
    }

    @Override
    public UttakResultat hentOgMapUttakResultat(UttakInput input) {
        final UttakResultat uttakResultat = new UttakResultat(mapUttakResultatPeriodes(input));
        return uttakResultat;
    }

    private List<UttakResultatPeriode> mapUttakResultatPeriodes(UttakInput input) {
        return input.getBeregningsgrunnlagStatusPerioder()
            .stream()
            .collect(Collectors.groupingBy(BeregningsgrunnlagStatusPeriode::getPeriode, toSet()))
            .entrySet()
            .stream()
            .map(toUttakResultatPeriode())
            .collect(Collectors.toList());
    }

}
