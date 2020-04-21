package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class MapUttakFrisinnTilRegel {

    private MapUttakFrisinnTilRegel() {
        // Skjuler default
    }

    public static List<UttakResultatPeriode> map(UttakAktivitet fastsattUttak) {
        return mapPerioder(fastsattUttak.getPerioder());
    }

    private static List<UttakResultatPeriode> mapPerioder(Set<UttakAktivitetPeriode> perioder) {
        List<UttakResultatPeriode> resultat = new ArrayList<>();
        perioder.forEach(periode -> {
            UttakResultatPeriode uttakResultatPeriode = new UttakResultatPeriode(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), mapUttakAkvititeter(periode), false);
            resultat.add(uttakResultatPeriode);
        });
        return resultat;
    }

    private static List<no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet> mapUttakAkvititeter(UttakAktivitetPeriode periode) {
        return Collections.singletonList(new no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet(
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(100),
            null,
            periode.getAktivitetType(),
            periode.getSkalJobbeProsent().compareTo(BigDecimal.ZERO) > 0));
    }
}
