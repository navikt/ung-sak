package no.nav.k9.sak.ytelse.beregning.psb;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.InnvilgetUttaksplanperiode;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.Periode;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.UttakUtbetalingsgrad;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.Uttaksplan;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.Uttaksplanperiode;
import no.nav.k9.sak.ytelse.beregning.UttakResultatInput;
import no.nav.k9.sak.ytelse.beregning.UttakResultatMapper;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PsbUttakResultatMapper implements UttakResultatMapper {

    PsbUttakResultatMapper() {
        // for proxy
    }

    private static UttakResultatPeriode toUttakResultatPeriode(LocalDate fom, LocalDate tom, Uttaksplanperiode uttak) {
        switch (uttak.getUtfall()) {
            case INNVILGET:
                return new UttakResultatPeriode(fom, tom, toUttakAktiviteter((InnvilgetUttaksplanperiode) uttak), false);
            case AVSLÅTT:
                return new UttakResultatPeriode(fom, tom, null, true); // TODO: indikerer opphold,bør ha med avslagsårsaker?
            default:
                throw new UnsupportedOperationException("Støtter ikke uttaksplanperiode av type: " + uttak);
        }
    }

    private static List<UttakAktivitet> toUttakAktiviteter(InnvilgetUttaksplanperiode uttaksplanperiode) {
        return uttaksplanperiode.getUtbetalingsgrader().stream()
            .map(PsbUttakResultatMapper::mapTilUttaksAktiviteter)
            .collect(Collectors.toList());
    }

    private static UttakAktivitet mapTilUttaksAktiviteter(UttakUtbetalingsgrad data) {
        BigDecimal stillingsgrad = BigDecimal.ZERO; // bruker ikke for Pleiepenger barn, bruker kun utbetalingsgrad
        boolean erGradering = false; // setter alltid false (bruker alltid utbetalingsgrad, framfor stillingsprosent
        BigDecimal utbetalingsgrad = data.getUtbetalingsgrad();

        var arb = data.getArbeidsforhold();
        final Arbeidsforhold.Builder arbeidsforholdBuilder = Arbeidsforhold.builder();
        if (arb.getOrganisasjonsnummer() != null) {
            arbeidsforholdBuilder.medOrgnr(arb.getOrganisasjonsnummer());
        } else if (arb.getAktørId() != null) {
            arbeidsforholdBuilder.medAktørId(arb.getAktørId().getId());
        }

        var arbeidsforhold = arbeidsforholdBuilder.build();
        return new UttakAktivitet(stillingsgrad, utbetalingsgrad, arbeidsforhold, data.getArbeidsforhold().getType(), erGradering);
    }

    @Override
    public UttakResultat hentOgMapUttakResultat(UttakResultatInput input) {
        var uttaksplan = input.getUttaksplan();
        final UttakResultat uttakResultat = new UttakResultat(mapUttakResultatPeriodes(uttaksplan));
        return uttakResultat;
    }

    private List<UttakResultatPeriode> mapUttakResultatPeriodes(Uttaksplan uttaksplan) {
        var uttakTimeline = getTimeline(uttaksplan);
        List<UttakResultatPeriode> res = new ArrayList<>();

        uttakTimeline.toSegments().forEach(seg -> {
            res.add(toUttakResultatPeriode(seg.getFom(), seg.getTom(), seg.getValue()));
        });
        return res;
    }
    
    private static LocalDateTimeline<Uttaksplanperiode> getTimeline(Uttaksplan uttaksplan) {
        return new LocalDateTimeline<>(uttaksplan.getPerioder().entrySet().stream().map(e -> toSegment(e.getKey(), e.getValue())).collect(Collectors.toList()));
    }

    private static LocalDateSegment<Uttaksplanperiode> toSegment(Periode periode, Uttaksplanperiode value) {
        return new LocalDateSegment<>(periode.getFom(), periode.getTom(), value);
    }
    

}
