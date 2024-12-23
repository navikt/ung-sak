package no.nav.ung.sak.domene.iay.modell;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.DiffIgnore;

public class InntektArbeidYtelseAggregat {

    private UUID uuid;

    @ChangeTracked
    private Set<AktørInntekt> aktørInntekt = new LinkedHashSet<>();

    @ChangeTracked
    private Set<AktørYtelse> aktørYtelse = new LinkedHashSet<>();

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
        this.setAktørInntekt(kopierFra.getAktørInntekt().stream().map(ai -> {
            AktørInntekt aktørInntekt = new AktørInntekt(ai);
            return aktørInntekt;
        }).collect(Collectors.toList()));

        this.setAktørYtelse(kopierFra.getAktørYtelse().stream().map(ay -> {
            AktørYtelse aktørYtelse = new AktørYtelse(ay);
            return aktørYtelse;
        }).collect(Collectors.toList()));

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

    public Collection<AktørInntekt> getAktørInntekt() {
        return Collections.unmodifiableSet(aktørInntekt);
    }

    void setAktørInntekt(Collection<AktørInntekt> aktørInntekt) {
        this.aktørInntekt = new LinkedHashSet<>(aktørInntekt);
    }

    void leggTilAktørInntekt(AktørInntekt aktørInntekt) {
        this.aktørInntekt.add(aktørInntekt);
    }


    void leggTilAktørYtelse(AktørYtelse aktørYtelse) {
        this.aktørYtelse.add(aktørYtelse);
    }



    public Collection<AktørYtelse> getAktørYtelse() {
        return Collections.unmodifiableSet(aktørYtelse);
    }

    void setAktørYtelse(Collection<AktørYtelse> aktørYtelse) {
        this.aktørYtelse = new LinkedHashSet<>(aktørYtelse);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof InntektArbeidYtelseAggregat)) {
            return false;
        }
        InntektArbeidYtelseAggregat other = (InntektArbeidYtelseAggregat) obj;
        return Objects.equals(this.getAktørInntekt(), other.getAktørInntekt())
            && Objects.equals(this.getAktørYtelse(), other.getAktørYtelse());
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørInntekt, aktørYtelse);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "aktørInntekt=" + aktørInntekt +
            ", aktørYtelse=" + aktørYtelse +
            '>';
    }

}
