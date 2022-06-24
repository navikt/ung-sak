package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import java.util.List;
import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

public class MedisinskMellomregningData {

    private final MedisinskVilkårGrunnlag grunnlag;
    private LocalDateTimeline<Pleielokasjon> pleiesHjemmetidslinje;
    private LocalDateTimeline<LivetsSluttfaseDokumentasjon> dokumentasjonStatusLivetsSluttfaseTidslinje;

    MedisinskMellomregningData(MedisinskVilkårGrunnlag grunnlag) {
        Objects.requireNonNull(grunnlag);
        this.grunnlag = grunnlag;
        this.pleiesHjemmetidslinje = new LocalDateTimeline<>(grunnlag.getFom(), grunnlag.getTom(), Pleielokasjon.HJEMME);
        this.dokumentasjonStatusLivetsSluttfaseTidslinje = new LocalDateTimeline<>(grunnlag.getFom(), grunnlag.getTom(), LivetsSluttfaseDokumentasjon.IKKE_DOKUMENTERT);
    }

    public MedisinskVilkårGrunnlag getGrunnlag() {
        return grunnlag;
    }

    void registrerInnleggelser(LocalDateTimeline<Void> innleggelsePerioder) {
        pleiesHjemmetidslinje = pleiesHjemmetidslinje.combine(
            innleggelsePerioder.mapValue(f -> Pleielokasjon.INNLAGT),
            StandardCombinators::coalesceRightHandSide,
            LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    void registrerDokumentasjonLivetsSluttfase(LocalDateTimeline<Void> dokumentertLivetsSluttfase) {
        dokumentasjonStatusLivetsSluttfaseTidslinje = dokumentasjonStatusLivetsSluttfaseTidslinje.combine(
            dokumentertLivetsSluttfase.mapValue(f -> LivetsSluttfaseDokumentasjon.DOKUMENTERT),
            StandardCombinators::coalesceRightHandSide,
            LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    List<PleiePeriode> getBeregnedePerioderMedPleielokasjon() {
        return pleiesHjemmetidslinje.compress()
            .stream()
            .map(segment -> new PleiePeriode(segment.getFom(), segment.getTom(), segment.getValue()))
            .toList();
    }

    List<LivetsSluttfaseDokumentasjonPeriode> getDokumentasjonStatusLivetsSluttfasePerioder() {
        return dokumentasjonStatusLivetsSluttfaseTidslinje.compress()
            .stream()
            .map(segment -> new LivetsSluttfaseDokumentasjonPeriode(segment.getFom(), segment.getTom(), segment.getValue()))
            .toList();
    }

    void oppdaterResultat(MedisinskVilkårResultat resultatStruktur) {
        Objects.requireNonNull(resultatStruktur);

        resultatStruktur.setPleieperioder(getBeregnedePerioderMedPleielokasjon());
        resultatStruktur.setDokumentasjonLivetsSluttfasePerioder(getDokumentasjonStatusLivetsSluttfasePerioder());
    }

    @Override
    public String toString() {
        return "MedisinskMellomregningData{" +
            "grunnlag=" + grunnlag +
            ", pleiesHjemmetidslinje=" + getDokumentasjonStatusLivetsSluttfasePerioder() +
            ", dokumentertLivetsSluttfasePerioder=" + getDokumentasjonStatusLivetsSluttfasePerioder() +
            '}';
    }
}
