package no.nav.ung.sak.domene.iay.modell;

import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.DiffIgnore;

import java.time.LocalDateTime;
import java.util.*;

public class InntektArbeidYtelseAggregat {

    private UUID uuid;

    @ChangeTracked
    private Inntekter inntekter;

    @DiffIgnore
    private LocalDateTime opprettetTidspunkt;

    InntektArbeidYtelseAggregat() {
        // hibernate
    }

    InntektArbeidYtelseAggregat(UUID angittEksternReferanse, LocalDateTime angittOpprettetTidspunkt) {
        setOpprettetTidspunkt(angittOpprettetTidspunkt);
        uuid = angittEksternReferanse;
    }

    private void setOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    /** copy constructor men med angitt referanse og tidspunkt. Hvis unikt kan denne instansen brukes til lagring. */
    InntektArbeidYtelseAggregat(UUID eksternReferanse, LocalDateTime opprettetTidspunkt, InntektArbeidYtelseAggregat kopierFra) {
        this.setInntekter(new Inntekter(kopierFra.getInntekter()));
        setOpprettetTidspunkt(opprettetTidspunkt);
        this.uuid = eksternReferanse;

    }

    /**
     * Copy constructor - inklusiv angitt referanse og opprettet tid. Brukes for immutable copy internt i minne. Hvis lagres vil gi unik
     * constraint exception.
     */
    InntektArbeidYtelseAggregat(InntektArbeidYtelseAggregat kopierFra) {
        this(kopierFra.getEksternReferanse(), kopierFra.getOpprettetTidspunkt(), kopierFra);
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    /** Identifisere en immutable instans av grunnlaget unikt og er egnet for utveksling (eks. til abakus eller andre systemer) */
    public UUID getEksternReferanse() {
        return uuid;
    }

    public Inntekter getInntekter() {
        return inntekter;
    }

    void setInntekter(Inntekter inntekter) {
        this.inntekter = inntekter;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof InntektArbeidYtelseAggregat)) {
            return false;
        }
        InntektArbeidYtelseAggregat other = (InntektArbeidYtelseAggregat) obj;
        return Objects.equals(this.getInntekter(), other.getInntekter());
    }

    @Override
    public int hashCode() {
        return Objects.hash(inntekter);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "inntekter=" + inntekter +
            '>';
    }

}
