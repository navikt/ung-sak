package no.nav.k9.sak.domene.iay.modell;

import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.BekreftetPermisjonStatus;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Stillingsprosent;

public class ArbeidsforholdOverstyring implements IndexKey {

    private Arbeidsgiver arbeidsgiver;

    private InternArbeidsforholdRef arbeidsforholdRef;

    private InternArbeidsforholdRef nyArbeidsforholdRef;

    @ChangeTracked
    private ArbeidsforholdHandlingType handling = ArbeidsforholdHandlingType.UDEFINERT;

    private String begrunnelse;

    private String navn;

    private Stillingsprosent stillingsprosent;

    private List<ArbeidsforholdOverstyrtePerioder> arbeidsforholdOverstyrtePerioder = new ArrayList<>();

    private BekreftetPermisjon bekreftetPermisjon = new BekreftetPermisjon();

    ArbeidsforholdOverstyring() {}

    ArbeidsforholdOverstyring(ArbeidsforholdOverstyring arbeidsforholdOverstyringEntitet) {
        this.arbeidsgiver = arbeidsforholdOverstyringEntitet.getArbeidsgiver();
        this.arbeidsforholdRef = arbeidsforholdOverstyringEntitet.arbeidsforholdRef;
        this.handling = arbeidsforholdOverstyringEntitet.getHandling();
        this.nyArbeidsforholdRef = arbeidsforholdOverstyringEntitet.nyArbeidsforholdRef;
        this.bekreftetPermisjon = arbeidsforholdOverstyringEntitet.bekreftetPermisjon;
        this.navn = arbeidsforholdOverstyringEntitet.getArbeidsgiverNavn();
        this.stillingsprosent = arbeidsforholdOverstyringEntitet.getStillingsprosent();
        this.begrunnelse = arbeidsforholdOverstyringEntitet.getBegrunnelse();
        leggTilOverstyrtePerioder(arbeidsforholdOverstyringEntitet);
    }

    private void leggTilOverstyrtePerioder(ArbeidsforholdOverstyring arbeidsforholdOverstyringEntitet) {
        for (ArbeidsforholdOverstyrtePerioder overstyrtePerioderEntitet : arbeidsforholdOverstyringEntitet.getArbeidsforholdOverstyrtePerioder()) {
            ArbeidsforholdOverstyrtePerioder perioderEntitet = new ArbeidsforholdOverstyrtePerioder(overstyrtePerioderEntitet);
            perioderEntitet.setArbeidsforholdOverstyring(this);
            this.arbeidsforholdOverstyrtePerioder.add(perioderEntitet);
        }
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    void setArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef != null ? arbeidsforholdRef : InternArbeidsforholdRef.nullRef();
    }

    void setArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
        this.arbeidsforholdRef = arbeidsforholdRef;
    }

    public ArbeidsforholdHandlingType getHandling() {
        return handling;
    }

    void setHandling(ArbeidsforholdHandlingType handling) {
        this.handling = handling;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    void setBeskrivelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    void leggTilOverstyrtPeriode(LocalDate fom, LocalDate tom) {
        ArbeidsforholdOverstyrtePerioder overstyrtPeriode = new ArbeidsforholdOverstyrtePerioder();
        overstyrtPeriode.setPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        overstyrtPeriode.setArbeidsforholdOverstyring(this);
        arbeidsforholdOverstyrtePerioder.add(overstyrtPeriode);
    }

    public List<ArbeidsforholdOverstyrtePerioder> getArbeidsforholdOverstyrtePerioder() {
        return arbeidsforholdOverstyrtePerioder;
    }

    public InternArbeidsforholdRef getNyArbeidsforholdRef() {
        return nyArbeidsforholdRef;
    }

    void setNyArbeidsforholdRef(InternArbeidsforholdRef nyArbeidsforholdRef) {
        this.nyArbeidsforholdRef = nyArbeidsforholdRef != null && !InternArbeidsforholdRef.nullRef().equals(nyArbeidsforholdRef) ? nyArbeidsforholdRef : null;
    }

    public Optional<BekreftetPermisjon> getBekreftetPermisjon() {
        if (bekreftetPermisjon.getStatus().equals(BekreftetPermisjonStatus.UDEFINERT)) {
            return Optional.empty();
        }
        return Optional.of(bekreftetPermisjon);
    }

    void setBekreftetPermisjon(BekreftetPermisjon bekreftetPermisjon) {
        this.bekreftetPermisjon = bekreftetPermisjon;
    }

    public boolean erOverstyrt(){
        return !Objects.equals(ArbeidsforholdHandlingType.BRUK, handling)
            || ( Objects.equals(ArbeidsforholdHandlingType.BRUK, handling) &&
            !Objects.equals(bekreftetPermisjon.getStatus(), BekreftetPermisjonStatus.UDEFINERT) );
    }

    public boolean kreverIkkeInntektsmelding() {
        return Set.of(LAGT_TIL_AV_SAKSBEHANDLER, BRUK_UTEN_INNTEKTSMELDING,
            BRUK_MED_OVERSTYRT_PERIODE, INNTEKT_IKKE_MED_I_BG).contains(handling);
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { arbeidsgiver, arbeidsforholdRef };
        return IndexKeyComposer.createKey(keyParts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ArbeidsforholdOverstyring)) return false;
        ArbeidsforholdOverstyring that = (ArbeidsforholdOverstyring) o;
        return Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
            Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, arbeidsforholdRef);
    }

    @Override
    public String toString() {
        return "ArbeidsforholdOverstyringEntitet{" +
            "arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdRef=" + arbeidsforholdRef +
            ", handling=" + handling +
            '}';
    }

    public Stillingsprosent getStillingsprosent() {
        return stillingsprosent;
    }

    public String getArbeidsgiverNavn() {
        return navn;
    }

    void setNavn(String navn) {
        this.navn = navn;
    }

    void setStillingsprosent(Stillingsprosent stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }
}
