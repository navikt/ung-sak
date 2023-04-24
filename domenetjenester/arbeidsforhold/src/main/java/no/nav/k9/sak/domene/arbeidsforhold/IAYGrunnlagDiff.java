package no.nav.k9.sak.domene.arbeidsforhold;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.arbeidsforhold.LønnsinntektBeskrivelse;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

public class IAYGrunnlagDiff {
    private static final Set<FagsakYtelseType> EKSLUSIVE_TYPER = Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.ENGANGSTØNAD);
    private InntektArbeidYtelseGrunnlag grunnlag1;
    private InntektArbeidYtelseGrunnlag grunnlag2;

    public IAYGrunnlagDiff(InntektArbeidYtelseGrunnlag grunnlag1, InntektArbeidYtelseGrunnlag grunnlag2) {
        this.grunnlag1 = grunnlag1;
        this.grunnlag2 = grunnlag2;
    }

    public DiffResult diffResultat(boolean onlyCheckTrackedFields) {
        return new IAYDiffsjekker(onlyCheckTrackedFields).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    public boolean erEndringPåInntektsmelding() {
        var eksisterende = Optional.ofNullable(grunnlag1).flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger);
        var nye = Optional.ofNullable(grunnlag2).flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger);

        // quick check
        if (eksisterende.isPresent() != nye.isPresent()) {
            return true;
        } else if (eksisterende.isEmpty()) {
            return false;
        } else {
            if (eksisterende.get().getAlleInntektsmeldinger().size() != nye.get().getAlleInntektsmeldinger().size()) {
                return true;
            } else {
                DiffResult diff = new IAYDiffsjekker().getDiffEntity().diff(eksisterende.get(), nye.get());
                return !diff.isEmpty();
            }
        }
    }

    public boolean erEndringPåAktørArbeidForAktør(LocalDate skjæringstidspunkt, AktørId aktørId) {
        var eksisterendeAktørArbeid = Optional.ofNullable(grunnlag1).flatMap(it -> it.getAktørArbeidFraRegister(aktørId));
        var nyAktørArbeid = Optional.ofNullable(grunnlag2).flatMap(it -> it.getAktørArbeidFraRegister(aktørId));

        // quick check
        if (eksisterendeAktørArbeid.isPresent() != nyAktørArbeid.isPresent()) {
            return true;
        } else if (eksisterendeAktørArbeid.isEmpty()) {
            return false;
        } else {
            var eksisterendeFilter = new YrkesaktivitetFilter(null, eksisterendeAktørArbeid).før(skjæringstidspunkt);
            var nyFilter = new YrkesaktivitetFilter(null, nyAktørArbeid).før(skjæringstidspunkt);
            if (eksisterendeFilter.getYrkesaktiviteter().size() != nyFilter.getYrkesaktiviteter().size()
                || eksisterendeFilter.getAnsettelsesPerioder().size() != nyFilter.getAnsettelsesPerioder().size()) {
                return true;
            }
        }

        // deep check
        DiffResult diff = new IAYDiffsjekker().getDiffEntity().diff(eksisterendeAktørArbeid.get(), nyAktørArbeid.get());
        return !diff.isEmpty();
    }

    public boolean erEndringPåAktørArbeidForAktør(DatoIntervallEntitet periode, AktørId aktørId) {
        var eksisterendeAktørArbeid = Optional.ofNullable(grunnlag1).flatMap(it -> it.getAktørArbeidFraRegister(aktørId));
        var nyAktørArbeid = Optional.ofNullable(grunnlag2).flatMap(it -> it.getAktørArbeidFraRegister(aktørId));

        // quick check
        if (eksisterendeAktørArbeid.isPresent() != nyAktørArbeid.isPresent()) {
            return true;
        } else if (eksisterendeAktørArbeid.isEmpty()) {
            return false;
        } else {
            var eksisterendeFilter = new YrkesaktivitetFilter(null, eksisterendeAktørArbeid).i(periode);
            var nyFilter = new YrkesaktivitetFilter(null, nyAktørArbeid).i(periode);
            if (eksisterendeFilter.getYrkesaktiviteter().size() != nyFilter.getYrkesaktiviteter().size()
                || eksisterendeFilter.getAnsettelsesPerioder().size() != nyFilter.getAnsettelsesPerioder().size()) {
                return true;
            }
        }

        // deep check
        DiffResult diff = new IAYDiffsjekker().getDiffEntity().diff(eksisterendeAktørArbeid.get(), nyAktørArbeid.get());
        return !diff.isEmpty();
    }

    public boolean erEndringPåAktørInntektForAktør(LocalDate skjæringstidspunkt, AktørId aktørId) {

        var eksisterende = Optional.ofNullable(grunnlag1).flatMap(it -> it.getAktørInntektFraRegister(aktørId));
        var nye = Optional.ofNullable(grunnlag2).flatMap(it -> it.getAktørInntektFraRegister(aktørId));

        // quick check
        if (eksisterende.isPresent() != nye.isPresent()) {
            return true;
        } else if (eksisterende.isEmpty()) {
            return false;
        } else {
            var eksisterendeInntektFilter = new InntektFilter(eksisterende).før(skjæringstidspunkt);
            var nyeInntektFilter = new InntektFilter(nye).før(skjæringstidspunkt);
            // TODO - raffinere med tanke på Startpunkt BEREGNING. Kan sjekke på diff pensjonsgivende, beregning og Sigrun
            if (eksisterendeInntektFilter.getFiltrertInntektsposter().size() != nyeInntektFilter.getFiltrertInntektsposter().size()) {
                return true;
            }
        }
        // deep check
        DiffResult diff = new IAYDiffsjekker().getDiffEntity().diff(eksisterende.get(), nye.get());
        return !diff.isEmpty();
    }

    public boolean erEndringPåAktørInntektForAktør(DatoIntervallEntitet periode, AktørId aktørId) {

        var eksisterende = Optional.ofNullable(grunnlag1).flatMap(it -> it.getAktørInntektFraRegister(aktørId));
        var nye = Optional.ofNullable(grunnlag2).flatMap(it -> it.getAktørInntektFraRegister(aktørId));

        // quick check
        if (eksisterende.isPresent() != nye.isPresent()) {
            return true;
        } else if (eksisterende.isEmpty()) {
            return false;
        } else {
            var eksisterendeInntektFilter = new InntektFilter(eksisterende).i(periode);
            var nyeInntektFilter = new InntektFilter(nye).i(periode);
            // TODO - raffinere med tanke på Startpunkt BEREGNING. Kan sjekke på diff pensjonsgivende, beregning og Sigrun
            if (eksisterendeInntektFilter.getFiltrertInntektsposter().size() != nyeInntektFilter.getFiltrertInntektsposter().size()) {
                return true;
            }
        }
        // deep check
        DiffResult diff = new IAYDiffsjekker().getDiffEntity().diff(eksisterende.get(), nye.get());
        return !diff.isEmpty();
    }

    public AktørYtelseEndring endringPåAktørYtelseForAktør(Saksnummer egetSaksnummer, LocalDate skjæringstidspunkt, AktørId aktørId) {
        Predicate<Ytelse> predikatEksklusiveTyper = ytelse -> EKSLUSIVE_TYPER.contains(ytelse.getYtelseType())
            && (ytelse.getSaksnummer() == null || !ytelse.getSaksnummer().equals(egetSaksnummer));
        Predicate<Ytelse> predikatAndreYtelseTyper = ytelse -> !EKSLUSIVE_TYPER.contains(ytelse.getYtelseType())
            && (ytelse.getSaksnummer() == null || !ytelse.getSaksnummer().equals(egetSaksnummer));
        // Setter fris for å få med nye "parallelle" søknader, men unngår overlapp med neste barn. Kan tunes. Annen søknad får AP når denne vedtatt
        LocalDate datoForEksklusiveTyper = LocalDate.now().isAfter(skjæringstidspunkt) ? skjæringstidspunkt.plusMonths(3L) : skjæringstidspunkt;

        List<Ytelse> førYtelserFpsak = hentYtelserForAktør(grunnlag1, datoForEksklusiveTyper, aktørId, predikatEksklusiveTyper);
        List<Ytelse> nåYtelserFpsak = hentYtelserForAktør(grunnlag2, datoForEksklusiveTyper, aktørId, predikatEksklusiveTyper);
        boolean erEksklusiveYtlserEndret = !new IAYDiffsjekker().getDiffEntity().diff(førYtelserFpsak, nåYtelserFpsak).isEmpty();

        List<Ytelse> førYtelserEkstern = hentYtelserForAktør(grunnlag1, skjæringstidspunkt, aktørId, predikatAndreYtelseTyper);
        List<Ytelse> nåYtelserEkstern = hentYtelserForAktør(grunnlag2, skjæringstidspunkt, aktørId, predikatAndreYtelseTyper);
        boolean erAndreYtelserEndret = !new IAYDiffsjekker().getDiffEntity().diff(førYtelserEkstern, nåYtelserEkstern).isEmpty();

        return new AktørYtelseEndring(erEksklusiveYtlserEndret, erAndreYtelserEndret);
    }

    public boolean endringPåAktørYtelseForAktør(Saksnummer egetSaksnummer, DatoIntervallEntitet periode, AktørId aktørId) {
        Predicate<Ytelse> predikatYtelseTyper = ytelse -> (ytelse.getSaksnummer() == null || !ytelse.getSaksnummer().equals(egetSaksnummer));
        // Setter fris for å få med nye "parallelle" søknader, men unngår overlapp med neste barn. Kan tunes. Annen søknad får AP når denne vedtatt

        List<Ytelse> førYtelserEkstern = hentYtelserForAktør(grunnlag1, periode, aktørId, predikatYtelseTyper);
        List<Ytelse> nåYtelserEkstern = hentYtelserForAktør(grunnlag2, periode, aktørId, predikatYtelseTyper);
        boolean erAndreYtelserEndret = !new IAYDiffsjekker().getDiffEntity().diff(førYtelserEkstern, nåYtelserEkstern).isEmpty();

        return erAndreYtelserEndret;
    }

    public boolean endringAvMottakAvOmsorgsstønadOgFosterhjemsgodtgjørelse(DatoIntervallEntitet periode, AktørId aktørId) {
        boolean førHarOmsorgsstønad = harAktørOmsorgsstønadIPeriode(grunnlag1, periode, aktørId);
        boolean etterHarOmsorgsstønad = harAktørOmsorgsstønadIPeriode(grunnlag2, periode, aktørId);
        return førHarOmsorgsstønad != etterHarOmsorgsstønad;
    }

    private List<Ytelse> hentYtelserForAktør(InntektArbeidYtelseGrunnlag grunnlag, LocalDate skjæringstidspunkt, AktørId aktørId,
                                             Predicate<Ytelse> predikatYtelseskilde) {
        if (grunnlag == null) {
            return List.of();
        }
        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunkt);
        return filter.getFiltrertYtelser().stream()
            .filter(predikatYtelseskilde)
            .collect(Collectors.toList());
    }

    private boolean harAktørOmsorgsstønadIPeriode(InntektArbeidYtelseGrunnlag grunnlag, DatoIntervallEntitet periode, AktørId aktørId) {
        if (grunnlag == null) {
            return false;
        }
        var filter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId));
        return filter.getFiltrertInntektsposter().stream()
            .filter(it -> it.getPeriode().overlapper(periode))
            .anyMatch(it -> it.getLønnsinntektBeskrivelse() != null && it.getLønnsinntektBeskrivelse().equals(LønnsinntektBeskrivelse.KOMMUNAL_OMSORGSLOENN_OG_FOSTERHJEMSGODTGJOERELSE));
    }


    private List<Ytelse> hentYtelserForAktør(InntektArbeidYtelseGrunnlag grunnlag, DatoIntervallEntitet periode, AktørId aktørId,
                                             Predicate<Ytelse> predikatYtelseskilde) {
        if (grunnlag == null) {
            return List.of();
        }
        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId)).i(periode);
        return filter.getFiltrertYtelser().stream()
            .filter(predikatYtelseskilde)
            .collect(Collectors.toList());
    }
}
