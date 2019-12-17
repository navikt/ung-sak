package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.kodeverk.MapOpptjeningAktivitetTypeFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Aktivitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.skjæringstidspunkt.regelmodell.AktivPeriode;
import no.nav.folketrygdloven.skjæringstidspunkt.regelmodell.AktivitetStatusModell;
import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;

@ApplicationScoped
public class MapBGStatuserFraVLTilRegel {

    private static final String INGEN_AKTIVITET_MELDING = "Må ha aktiviteter for å sette status.";

    public MapBGStatuserFraVLTilRegel() {
        // for CDI proxy
    }

    public AktivitetStatusModell map(BeregningAktivitetAggregatEntitet beregningAktivitetAggregat) {
        AktivitetStatusModell regelmodell = new AktivitetStatusModell();
        regelmodell.setSkjæringstidspunktForOpptjening(beregningAktivitetAggregat.getSkjæringstidspunktOpptjening());
        leggTilAktiviteter(beregningAktivitetAggregat, regelmodell);
        return regelmodell;
    }

    private void leggTilAktiviteter(BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                    AktivitetStatusModell modell) {
        List<BeregningAktivitetEntitet> relevanteAktiviteter = beregningAktivitetAggregat.getBeregningAktiviteter();
        if (relevanteAktiviteter.isEmpty()) {  // For enklere feilsøking når det mangler aktiviteter
            throw new IllegalStateException(INGEN_AKTIVITET_MELDING);
        } else {
            relevanteAktiviteter.forEach(a -> modell.leggTilEllerOppdaterAktivPeriode(lagAktivPerioder(a)));
        }
    }

    private AktivPeriode lagAktivPerioder(BeregningAktivitetEntitet ba) {
        Aktivitet aktivitetType = MapOpptjeningAktivitetTypeFraVLTilRegel.map(ba.getOpptjeningAktivitetType());
        ÅpenDatoIntervallEntitet periode = ba.getPeriode();
        Periode regelPeriode = Periode.of(periode.getFomDato(), periode.getTomDato());
        if (Aktivitet.FRILANSINNTEKT.equals(aktivitetType)) {
            return AktivPeriode.forFrilanser(regelPeriode);
        }
        if (Aktivitet.ARBEIDSTAKERINNTEKT.equals(aktivitetType)) {
            return lagAktivPeriodeForArbeidstaker(ba, aktivitetType, regelPeriode);
        }
        return AktivPeriode.forAndre(aktivitetType, regelPeriode);
    }

    private AktivPeriode lagAktivPeriodeForArbeidstaker(BeregningAktivitetEntitet beregningAktivitet,
                                                        Aktivitet aktivitetType,
                                                        Periode gjeldendePeriode) {
        if (beregningAktivitet.getArbeidsgiver().erAktørId()) {
            return lagAktivePerioderForArbeidstakerHosPrivatperson(beregningAktivitet, gjeldendePeriode);
        } else if (beregningAktivitet.getArbeidsgiver().getErVirksomhet()) {
            return lagAktivePerioderForArbeidstakerHosVirksomhet(beregningAktivitet, aktivitetType, gjeldendePeriode);
        }
        throw new IllegalStateException("Arbeidsgiver må være enten aktør eller virksomhet.");
    }

    private AktivPeriode lagAktivePerioderForArbeidstakerHosPrivatperson(BeregningAktivitetEntitet beregningAktivitet, Periode gjeldendePeriode) {
        // Da vi ikke kan motta inntektsmeldinger ønsker vi ikke å sette arbeidsforholdId på arbeidsforholdet
        String aktørId = beregningAktivitet.getArbeidsgiver().getAktørId().getId();
        if (aktørId == null) {
            throw new IllegalArgumentException("Kan ikke lage periode for arbeidsforhold med arbeidsgiver som privatperson om aktørId er null");
        }
        return AktivPeriode.forArbeidstakerHosPrivatperson(gjeldendePeriode, aktørId);
    }

    private AktivPeriode lagAktivePerioderForArbeidstakerHosVirksomhet(BeregningAktivitetEntitet beregningAktivitet,
                                                                       Aktivitet aktivitetType,
                                                                       Periode gjeldendePeriode) {
        String orgnr = mapTilRegelmodellForOrgnr(aktivitetType, beregningAktivitet);
        String arbeidsforholdRef = beregningAktivitet.getArbeidsforholdRef().getReferanse();
        return AktivPeriode.forArbeidstakerHosVirksomhet(gjeldendePeriode, orgnr, arbeidsforholdRef);
    }

    private String mapTilRegelmodellForOrgnr(Aktivitet aktivitetType, BeregningAktivitetEntitet beregningAktivitet) {
        return Aktivitet.ARBEIDSTAKERINNTEKT.equals(aktivitetType) ? beregningAktivitet.getArbeidsgiver().getOrgnr() : null;
    }
}
