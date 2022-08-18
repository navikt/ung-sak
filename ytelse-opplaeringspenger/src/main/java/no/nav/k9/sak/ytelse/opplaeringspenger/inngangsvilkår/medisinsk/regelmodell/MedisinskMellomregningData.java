package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell;

import java.util.List;
import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

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

    void registrerDokumentasjonLangvarigSykdom(LocalDateTimeline<LangvarigSykdomDokumentasjon> dokumentertLangvarigSykdom) {
        dokumentasjonStatusLangvarigSykdomTidslinje = dokumentasjonStatusLangvarigSykdomTidslinje.combine(dokumentertLangvarigSykdom,
            StandardCombinators::coalesceRightHandSide,
            LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    LocalDateTimeline<LangvarigSykdomDokumentasjon> getVurdertTidslinje() {
        return dokumentasjonStatusLangvarigSykdomTidslinje.compress();
    }

    List<LangvarigSykdomPeriode> getDokumentasjonStatusLangvarigSykdomPerioder() {
        return dokumentasjonStatusLangvarigSykdomTidslinje.compress()
            .stream()
            .map(segment -> new LangvarigSykdomPeriode(segment.getFom(), segment.getTom(), segment.getValue()))
            .toList();
    }
}
