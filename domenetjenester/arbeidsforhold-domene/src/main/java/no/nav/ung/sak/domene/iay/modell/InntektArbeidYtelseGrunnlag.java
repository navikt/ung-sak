package no.nav.ung.sak.domene.iay.modell;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.DiffIgnore;
import no.nav.ung.sak.typer.AktørId;

/**
 * NB navnet her brukes av @GrunnlagRef i BehandlingÅrsakUtleder/StartpunktUtleder.
 */
public class InntektArbeidYtelseGrunnlag {

    @DiffIgnore
    private UUID uuid;

    @DiffIgnore
    private Long behandlingId;

    @ChangeTracked
    private InntektArbeidYtelseAggregat register;

    @ChangeTracked
    private OppgittOpptjening oppgittOpptjening;

    @ChangeTracked
    private OppgittOpptjeningAggregat oppgittOpptjeningAggregat;

    @ChangeTracked
    private OppgittOpptjening overstyrtOppgittOpptjening;

    private boolean aktiv = true;

    @DiffIgnore
    private LocalDateTime opprettetTidspunkt;

    InntektArbeidYtelseGrunnlag() {
    }

    public InntektArbeidYtelseGrunnlag(InntektArbeidYtelseGrunnlag grunnlag) {
        this(UUID.randomUUID(), LocalDateTime.now());

        // NB! skal ikke lage ny versjon av oppgitt opptjening eller andre underlag! Lenker bare inn på ferskt grunnlag
        grunnlag.getOppgittOpptjening().ifPresent(this::setOppgittOpptjening);
        grunnlag.getOppgittOpptjeningAggregat().ifPresent(this::setOppgittOpptjeningAggregat);
        grunnlag.getOverstyrtOppgittOpptjening().ifPresent(this::setOverstyrtOppgittOpptjening);
        grunnlag.getRegisterVersjon().ifPresent(this::setRegister);
    }

    public InntektArbeidYtelseGrunnlag(UUID grunnlagReferanse, LocalDateTime opprettetTidspunkt) {
        this.uuid = Objects.requireNonNull(grunnlagReferanse, "grunnlagReferanse");
        setOpprettetTidspunkt(opprettetTidspunkt);
    }

    private void setOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public InntektArbeidYtelseGrunnlag(UUID grunnlagReferanse, LocalDateTime opprettetTidspunkt, InntektArbeidYtelseGrunnlag grunnlag) {
        this(grunnlag);
        this.uuid = Objects.requireNonNull(grunnlagReferanse, "grunnlagReferanse");
        setOpprettetTidspunkt(opprettetTidspunkt);
    }

    /**
     * Identifisere en immutable instans av grunnlaget unikt og er egnet for utveksling (eks. til abakus eller andre systemer)
     */
    public UUID getEksternReferanse() {
        return uuid;
    }


    /**
     * Returnerer innhentede registeropplysninger som aggregat
     */
    public Optional<InntektArbeidYtelseAggregat> getRegisterVersjon() {
        return Optional.ofNullable(register);
    }


    public Optional<OppgittOpptjeningAggregat> getOppgittOpptjeningAggregat() {
        return Optional.ofNullable(oppgittOpptjeningAggregat);
    }

    public void setOppgittOpptjeningAggregat(OppgittOpptjeningAggregat oppgittOpptjeningAggregat) {
        this.oppgittOpptjeningAggregat = oppgittOpptjeningAggregat;
    }

    public Optional<AktørInntekt> getAktørInntektFraRegister(AktørId aktørId) {
        if (register != null) {
            var aktørInntekt = register.getAktørInntekt().stream().filter(aa -> Objects.equals(aa.getAktørId(), aktørId)).collect(Collectors.toList());
            if (aktørInntekt.size() > 1) {
                throw new IllegalStateException("Kan kun ha ett innslag av AktørInntekt for aktørId:" + aktørId + " i  grunnlag " + this.getEksternReferanse());
            }
            return aktørInntekt.stream().findFirst();
        }
        return Optional.empty();
    }

    /**
     * Returnerer oppgitt opptjening hvis det finnes. (Inneholder opplysninger søker opplyser om i søknaden)
     */
    public Optional<OppgittOpptjening> getOppgittOpptjening() {
        return Optional.ofNullable(oppgittOpptjening);
    }

    void setOppgittOpptjening(OppgittOpptjening oppgittOpptjening) {
        this.oppgittOpptjening = oppgittOpptjening;
    }

    void setOverstyrtOppgittOpptjening(OppgittOpptjening overstyrtOppgittOpptjening) {
        this.overstyrtOppgittOpptjening = overstyrtOppgittOpptjening;
    }

    /**
     * Returnerer overstyrt oppgitt opptjening hvis det finnes. (Inneholder overstyrt opplysninger søker opplyser om i søknaden)
     */
    public Optional<OppgittOpptjening> getOverstyrtOppgittOpptjening() {
        return Optional.ofNullable(overstyrtOppgittOpptjening);
    }


    public Long getBehandlingId() {
        return behandlingId;
    }

    void setAktivt(boolean aktiv) {
        this.aktiv = aktiv;
    }

    /**
     * Hvorvidt dette er det siste (aktive grunnlaget) for en behandling.
     */
    public boolean isAktiv() {
        return aktiv;
    }

    void setRegister(InntektArbeidYtelseAggregat registerFør) {
        this.register = registerFør;
    }


    public Optional<UUID> getKoblingReferanse() {
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof InntektArbeidYtelseGrunnlag))
            return false;
        InntektArbeidYtelseGrunnlag that = (InntektArbeidYtelseGrunnlag) o;
        return aktiv == that.aktiv &&
            Objects.equals(oppgittOpptjeningAggregat, that.oppgittOpptjeningAggregat) &&
            Objects.equals(register, that.register);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgittOpptjeningAggregat, register);
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

}
