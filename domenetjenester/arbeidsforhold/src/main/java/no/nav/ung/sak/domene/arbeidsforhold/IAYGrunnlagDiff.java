package no.nav.ung.sak.domene.arbeidsforhold;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.diff.DiffResult;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.iay.modell.InntektFilter;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public class IAYGrunnlagDiff {
    private InntektArbeidYtelseGrunnlag grunnlag1;
    private InntektArbeidYtelseGrunnlag grunnlag2;

    public IAYGrunnlagDiff(InntektArbeidYtelseGrunnlag grunnlag1, InntektArbeidYtelseGrunnlag grunnlag2) {
        this.grunnlag1 = grunnlag1;
        this.grunnlag2 = grunnlag2;
    }

    public DiffResult diffResultat(boolean onlyCheckTrackedFields) {
        return new IAYDiffsjekker(onlyCheckTrackedFields).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    public  boolean erEndringPåAktørInntektForAktør(LocalDateTimeline<?> perioder, AktørId aktørId) {

        var eksisterende = Optional.ofNullable(grunnlag1).flatMap(it -> it.getAktørInntektFraRegister(aktørId));
        var nye = Optional.ofNullable(grunnlag2).flatMap(it -> it.getAktørInntektFraRegister(aktørId));

        // quick check
        if (eksisterende.isPresent() != nye.isPresent()) {
            return true;
        } else if (eksisterende.isEmpty()) {
            return false;
        } else {
            var eksisterendeInntektFilter = new InntektFilter(eksisterende).i(perioder);
            var nyeInntektFilter = new InntektFilter(nye).i(perioder);
            if (eksisterendeInntektFilter.getFiltrertInntektsposter().size() != nyeInntektFilter.getFiltrertInntektsposter().size()) {
                return true;
            }
        }
        // deep check
        DiffResult diff = new IAYDiffsjekker().getDiffEntity().diff(eksisterende.get(), nye.get());
        return !diff.isEmpty();
    }

}
