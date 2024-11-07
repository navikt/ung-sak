package no.nav.k9.sak.domene.iay.modell;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.arbeidsforhold.InntektYtelseType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.arbeidsforhold.LønnsinntektBeskrivelse;
import no.nav.k9.kodeverk.arbeidsforhold.SkatteOgAvgiftsregelType;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Beløp;

import java.time.LocalDate;
import java.util.Objects;

public class Inntektspost implements IndexKey {

    private InntektspostType inntektspostType;

    private SkatteOgAvgiftsregelType skatteOgAvgiftsregelType = SkatteOgAvgiftsregelType.UDEFINERT;

    private LønnsinntektBeskrivelse lønnsinntektBeskrivelse = LønnsinntektBeskrivelse.UDEFINERT;

    private InntektYtelseType inntektYtelseType;

    private DatoIntervallEntitet periode;

    @ChangeTracked
    private Beløp beløp;

    Inntektspost() {
    }

    /**
     * Deep copy.
     */
    Inntektspost(Inntektspost inntektspost) {
        this.inntektspostType = inntektspost.getInntektspostType();
        this.skatteOgAvgiftsregelType = inntektspost.getSkatteOgAvgiftsregelType();
        this.lønnsinntektBeskrivelse = inntektspost.getLønnsinntektBeskrivelse();
        this.periode = inntektspost.getPeriode();
        this.beløp = inntektspost.getBeløp();
        this.inntektYtelseType = inntektspost.getInntektYtelseType();
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { getInntektspostType(), getInntektYtelseType(), getSkatteOgAvgiftsregelType(), getLønnsinntektBeskrivelse(), periode };
        return IndexKeyComposer.createKey(keyParts);
    }

    /**
     * Underkategori av utbetaling
     * <p>
     * F.eks
     * <ul>
     * <li>Lønn</li>
     * <li>Ytelse</li>
     * <li>Næringsinntekt</li>
     * </ul>
     *
     * @return {@link InntektspostType}
     */
    public InntektspostType getInntektspostType() {
        return inntektspostType;
    }

    void setInntektspostType(InntektspostType inntektspostType) {
        this.inntektspostType = inntektspostType;
    }

    /**
     * En kodeverksverdi som angir særskilt beskatningsregel.
     * Den er ikke alltid satt, og kommer fra inntektskomponenten
     *
     * @return {@link SkatteOgAvgiftsregelType}
     */
    public SkatteOgAvgiftsregelType getSkatteOgAvgiftsregelType() {
        return skatteOgAvgiftsregelType;
    }

    void setSkatteOgAvgiftsregelType(SkatteOgAvgiftsregelType skatteOgAvgiftsregelType) {
        this.skatteOgAvgiftsregelType = skatteOgAvgiftsregelType;
    }

    public InntektYtelseType getInntektYtelseType() {
        return inntektYtelseType;
    }

    public void setInntektYtelseType(InntektYtelseType inntektYtelseType) {
        this.inntektYtelseType = inntektYtelseType;
    }

    public LønnsinntektBeskrivelse getLønnsinntektBeskrivelse() {
        return lønnsinntektBeskrivelse;
    }

    public void setLønnsinntektBeskrivelse(LønnsinntektBeskrivelse lønnsinntektBeskrivelse) {
        this.lønnsinntektBeskrivelse = lønnsinntektBeskrivelse;
    }

    void setPeriode(LocalDate fom, LocalDate tom) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    /**
     * Beløpet som har blitt utbetalt i perioden
     *
     * @return Beløpet
     */
    public Beløp getBeløp() {
        return beløp;
    }

    void setBeløp(Beløp beløp) {
        this.beløp = beløp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !(obj instanceof Inntektspost)) {
            return false;
        }
        Inntektspost other = (Inntektspost) obj;
        return Objects.equals(this.getInntektspostType(), other.getInntektspostType())
            && Objects.equals(this.getSkatteOgAvgiftsregelType(), other.getSkatteOgAvgiftsregelType())
            && Objects.equals(this.getLønnsinntektBeskrivelse(), other.getLønnsinntektBeskrivelse())
            && Objects.equals(this.getPeriode().getFomDato(), other.getPeriode().getFomDato())
            && Objects.equals(this.getPeriode().getTomDato(), other.getPeriode().getTomDato());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInntektspostType(), getInntektYtelseType(), getSkatteOgAvgiftsregelType(), getLønnsinntektBeskrivelse(), getPeriode().getFomDato(), getPeriode().getTomDato());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "inntektYtelseType=" + inntektYtelseType +
            "inntektspostType=" + inntektspostType +
            "skatteOgAvgiftsregelType=" + skatteOgAvgiftsregelType +
            "lønnsinntektBeskrivelse=" + lønnsinntektBeskrivelse +
            ", fraOgMed=" + periode.getFomDato() +
            ", tilOgMed=" + periode.getTomDato() +
            ", beløp=" + beløp +
            '>';
    }

    public boolean hasValues() {
        return inntektspostType != null || periode.getFomDato() != null || periode.getTomDato() != null || beløp != null;
    }

}
