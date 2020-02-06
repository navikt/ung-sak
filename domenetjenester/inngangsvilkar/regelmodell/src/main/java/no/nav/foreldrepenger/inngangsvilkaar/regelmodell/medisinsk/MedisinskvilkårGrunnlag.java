package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;

@RuleDocumentationGrunnlag
public class MedisinskvilkårGrunnlag implements VilkårGrunnlag {

    private final LocalDate fom;
    private final LocalDate tom;

    private List<InnleggelsesPeriode> innleggelsesPerioder = List.of();
    private List<PeriodeMedKontinuerligTilsyn> kontinuerligTilsyn = List.of();
    private List<PeriodeMedUtvidetBehov> utvidetBehov = List.of();
    private String diagnoseKode;
    private DiagnoseKilde diagnoseKilde;

    public MedisinskvilkårGrunnlag(LocalDate fom, LocalDate tom) {
        this.fom = fom;
        this.tom = tom;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public List<InnleggelsesPeriode> getInnleggelsesPerioder() {
        return innleggelsesPerioder;
    }

    public MedisinskvilkårGrunnlag medInnleggelsesPerioder(List<InnleggelsesPeriode> innleggelsesPerioder) {
        this.innleggelsesPerioder = innleggelsesPerioder;
        return this;
    }

    public List<PeriodeMedKontinuerligTilsyn> getPerioderMedKontinuerligTilsyn() {
        return kontinuerligTilsyn;
    }

    public MedisinskvilkårGrunnlag medKontinuerligTilsyn(List<PeriodeMedKontinuerligTilsyn> kontinuerligTilsyn) {
        this.kontinuerligTilsyn = kontinuerligTilsyn;
        return this;
    }

    public List<PeriodeMedUtvidetBehov> getPerioderMedUtvidetBehov() {
        return utvidetBehov;
    }

    public MedisinskvilkårGrunnlag medUtvidetBehov(List<PeriodeMedUtvidetBehov> utvidetBehov) {
        this.utvidetBehov = utvidetBehov;
        return this;
    }

    public DiagnoseKilde getDiagnoseKilde() {
        return diagnoseKilde;
    }

    public MedisinskvilkårGrunnlag medDiagnoseKilde(DiagnoseKilde diagnoseKilde) {
        this.diagnoseKilde = diagnoseKilde;
        return this;
    }

    public String getDiagnoseKode() {
        return diagnoseKode;
    }

    public MedisinskvilkårGrunnlag medDiagnoseKode(String diagnoseKode) {
        this.diagnoseKode = diagnoseKode;
        return this;
    }

    @Override
    public String toString() {
        return "MedisinskvilkårGrunnlag{" +
            "fom=" + fom +
            ", tom=" + tom +
            ", innleggelsesPerioder=" + innleggelsesPerioder +
            ", kontinuerligTilsyn=" + kontinuerligTilsyn +
            ", utvidetBehov=" + utvidetBehov +
            ", diagnoseKode='" + diagnoseKode + '\'' +
            ", diagnoseKilde=" + diagnoseKilde +
            '}';
    }
}
