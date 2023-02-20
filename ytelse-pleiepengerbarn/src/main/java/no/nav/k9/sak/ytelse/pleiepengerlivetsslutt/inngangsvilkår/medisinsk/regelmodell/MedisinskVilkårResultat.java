package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class MedisinskVilkårResultat {

    public static final String PLEIEPERIODER_MED_PLEIELOKASJON = "resultat.pleieperioder";
    public static final String DOKUMENTASJON_LIVETS_SLUTTFASE_PERIODER = "resultat.dokumentasjon.livetssluttfase";

    private List<PleiePeriode> pleieperioder;
    private List<LivetsSluttfaseDokumentasjonPeriode> dokumentasjonLivetsSluttfasePerioder;

    public List<PleiePeriode> getPleieperioder() {
        return List.copyOf(pleieperioder);
    }

    public LocalDateTimeline<Pleielokasjon> tidslinjePleielokasjon() {
        return new LocalDateTimeline<>(pleieperioder.stream()
            .map(p -> new LocalDateSegment<>(p.getFraOgMed(), p.getTilOgMed(), p.getPleielokasjon()))
            .toList());
    }

    public LocalDateTimeline<LivetsSluttfaseDokumentasjon> tidslinjeLivetsSluttfaseDokumentasjon() {
        return new LocalDateTimeline<>(dokumentasjonLivetsSluttfasePerioder.stream()
            .map(p -> new LocalDateSegment<>(p.getFraOgMed(), p.getTilOgMed(), p.getLivetsSluttfaseDokumentasjon()))
            .toList());
    }

    public void setPleieperioder(List<PleiePeriode> pleieperioder) {
        this.pleieperioder = pleieperioder;
    }

    public List<LivetsSluttfaseDokumentasjonPeriode> getDokumentasjonLivetsSluttfasePerioder() {
        return dokumentasjonLivetsSluttfasePerioder;
    }

    public void setDokumentasjonLivetsSluttfasePerioder(List<LivetsSluttfaseDokumentasjonPeriode> dokumentasjonLivetsSluttfasePerioder) {
        this.dokumentasjonLivetsSluttfasePerioder = dokumentasjonLivetsSluttfasePerioder;
    }

    @Override
    public String toString() {
        return "PleiesHjemmeVilkårResultat{" +
            "pleieperioder=" + pleieperioder +
            "dokumentasjonLivetsSluttfasePperioder=" + dokumentasjonLivetsSluttfasePerioder +
            '}';
    }
}
