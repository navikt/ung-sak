package no.nav.k9.sak.domene.iay.modell;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.arbeidsforhold.LønnsinntektBeskrivelse;
import no.nav.k9.kodeverk.arbeidsforhold.NæringsinntektType;
import no.nav.k9.kodeverk.arbeidsforhold.OffentligYtelseType;
import no.nav.k9.kodeverk.arbeidsforhold.PensjonTrygdType;
import no.nav.k9.kodeverk.arbeidsforhold.SkatteOgAvgiftsregelType;
import no.nav.k9.kodeverk.arbeidsforhold.YtelseType;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Beløp;

public class Inntektspost implements IndexKey {

    private static final Map<String, Map<String, ? extends YtelseType>> YTELSE_TYPER = new LinkedHashMap<>();

    static {
        YTELSE_TYPER.put(OffentligYtelseType.KODEVERK, OffentligYtelseType.kodeMap());
        YTELSE_TYPER.put(NæringsinntektType.KODEVERK, NæringsinntektType.kodeMap());
        YTELSE_TYPER.put(PensjonTrygdType.KODEVERK, PensjonTrygdType.kodeMap());

    }
    private InntektspostType inntektspostType;

    private SkatteOgAvgiftsregelType skatteOgAvgiftsregelType = SkatteOgAvgiftsregelType.UDEFINERT;

    private LønnsinntektBeskrivelse lønnsinntektBeskrivelse = LønnsinntektBeskrivelse.UDEFINERT;

    /** Brukes kun til FK validering. Default OffentligYtelseType. Må settes sammen med {@link #ytelse} */
    private String ytelseType = OffentligYtelseType.KODEVERK;

    private String ytelse = OffentligYtelseType.UDEFINERT.getKode();

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
        this.ytelse = inntektspost.getYtelseType().getKode();
        this.periode = inntektspost.getPeriode();
        this.beløp = inntektspost.getBeløp();
        this.ytelseType = inntektspost.getYtelseType().getKodeverk();
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { getInntektspostType(), getYtelseType().getKodeverk(), getYtelseType().getKode(), getSkatteOgAvgiftsregelType(), getLønnsinntektBeskrivelse(), periode };
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

    public YtelseType getYtelseType() {
        var yt = YTELSE_TYPER.getOrDefault(ytelseType, Collections.emptyMap()).get(ytelse);
        return yt != null ? yt : OffentligYtelseType.UDEFINERT;
    }

    void setYtelse(YtelseType ytelse) {
        this.ytelseType = ytelse.getKodeverk();
        this.ytelse = ytelse.getKode();
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
            && Objects.equals(this.getYtelseType(), other.getYtelseType())
            && Objects.equals(this.getSkatteOgAvgiftsregelType(), other.getSkatteOgAvgiftsregelType())
            && Objects.equals(this.getPeriode().getFomDato(), other.getPeriode().getFomDato())
            && Objects.equals(this.getPeriode().getTomDato(), other.getPeriode().getTomDato());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInntektspostType(), getYtelseType(), getSkatteOgAvgiftsregelType(), getPeriode().getFomDato(), getPeriode().getTomDato());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "ytelseType=" + ytelseType +
            "inntektspostType=" + inntektspostType +
            "skatteOgAvgiftsregelType=" + skatteOgAvgiftsregelType +
            ", fraOgMed=" + periode.getFomDato() +
            ", tilOgMed=" + periode.getTomDato() +
            ", beløp=" + beløp +
            '>';
    }

    public boolean hasValues() {
        return inntektspostType != null || periode.getFomDato() != null || periode.getTomDato() != null || beløp != null;
    }

}
