package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

import java.util.List;
import java.util.Objects;

public class MedisinskMellomregningData {

    private final MedisinskVilkårGrunnlag grunnlag;
    private LocalDateTimeline<LangvarigSykdomDokumentasjon> dokumentasjonStatusLangvarigSykdomTidslinje;

    MedisinskMellomregningData(MedisinskVilkårGrunnlag grunnlag) {
        Objects.requireNonNull(grunnlag);
        this.grunnlag = grunnlag;
        this.dokumentasjonStatusLangvarigSykdomTidslinje = new LocalDateTimeline<>(grunnlag.getFom(), grunnlag.getTom(), LangvarigSykdomDokumentasjon.IKKE_DOKUMENTERT);
    }

    public MedisinskVilkårGrunnlag getGrunnlag() {
        return grunnlag;
    }

    public void oppdaterResultat(MedisinskVilkårResultat resultatStruktur) {
        Objects.requireNonNull(resultatStruktur);

        resultatStruktur.setLangvarigSykdomPerioder(getDokumentasjonStatusLangvarigSykdomPerioder());
    }

    void registrerDokumentasjonLangvarigSykdom(LocalDateTimeline<Void> dokumentertLangvarigSykdom) {
        dokumentasjonStatusLangvarigSykdomTidslinje = dokumentasjonStatusLangvarigSykdomTidslinje.combine(
            dokumentertLangvarigSykdom.mapValue(f -> LangvarigSykdomDokumentasjon.DOKUMENTERT),
            StandardCombinators::coalesceRightHandSide,
            LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    List<LangvarigSykdomPeriode> getDokumentasjonStatusLangvarigSykdomPerioder() {
        return dokumentasjonStatusLangvarigSykdomTidslinje.compress()
            .stream()
            .map(segment -> new LangvarigSykdomPeriode(segment.getFom(), segment.getTom(), segment.getValue()))
            .toList();
    }
}
