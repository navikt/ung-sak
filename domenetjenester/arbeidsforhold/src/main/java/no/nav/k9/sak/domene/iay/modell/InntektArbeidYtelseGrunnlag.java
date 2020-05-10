package no.nav.k9.sak.domene.iay.modell;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;

public class InntektArbeidYtelseGrunnlag {

    @DiffIgnore
    private UUID uuid;

    @DiffIgnore
    private Long behandlingId;

    @ChangeTracked
    private InntektArbeidYtelseAggregat register;

    @ChangeTracked
    private InntektArbeidYtelseAggregat saksbehandlet;

    @ChangeTracked
    private OppgittOpptjening oppgittOpptjening;

    @ChangeTracked
    private OppgittOpptjening overstyrtOppgittOpptjening;

    @ChangeTracked
    private InntektsmeldingAggregat inntektsmeldinger;

    @ChangeTracked
    private ArbeidsforholdInformasjon arbeidsforholdInformasjon;

    private boolean aktiv = true;

    @DiffIgnore
    private LocalDateTime opprettetTidspunkt;

    InntektArbeidYtelseGrunnlag() {
    }

    public InntektArbeidYtelseGrunnlag(InntektArbeidYtelseGrunnlag grunnlag) {
        this(UUID.randomUUID(), LocalDateTime.now());

        // NB! skal ikke lage ny versjon av oppgitt opptjening eller andre underlag! Lenker bare inn på ferskt grunnlag
        grunnlag.getOppgittOpptjening().ifPresent(this::setOppgittOpptjening);
        grunnlag.getOverstyrtOppgittOpptjening().ifPresent(this::setOverstyrtOppgittOpptjening);
        grunnlag.getRegisterVersjon().ifPresent(this::setRegister);
        grunnlag.getSaksbehandletVersjon().ifPresent(this::setSaksbehandlet);
        grunnlag.getInntektsmeldinger().ifPresent(this::setInntektsmeldinger);
        grunnlag.getArbeidsforholdInformasjon().ifPresent(this::setInformasjon);
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

    /** Identifisere en immutable instans av grunnlaget unikt og er egnet for utveksling (eks. til abakus eller andre systemer) */
    public UUID getEksternReferanse() {
        return uuid;
    }

    /**
     * Returnerer en overstyrt versjon av aggregat. Hvis saksbehandler har løst et aksjonspunkt i forbindele med
     * opptjening vil det finnes et overstyrt aggregat, gjelder for FØR første dag i permisjonsuttaket (skjæringstidspunktet)
     */
    public Optional<InntektArbeidYtelseAggregat> getSaksbehandletVersjon() {
        return Optional.ofNullable(saksbehandlet);
    }

    void setSaksbehandlet(InntektArbeidYtelseAggregat saksbehandletFør) {
        this.saksbehandlet = saksbehandletFør;
    }

    /**
     * Returnerer innhentede registeropplysninger som aggregat. Tar ikke hensyn til saksbehandlers overstyringer (se
     * {@link #getSaksbehandletVersjon()}.
     */
    public Optional<InntektArbeidYtelseAggregat> getRegisterVersjon() {
        return Optional.ofNullable(register);
    }

    /**
     * Returnere TRUE hvis det finnes en overstyr versjon, sjekker både FØR og ETTER
     */
    public boolean harBlittSaksbehandlet() {
        return getSaksbehandletVersjon().isPresent();
    }

    /**
     * Returnerer aggregat som holder alle inntektsmeldingene som benyttes i behandlingen.
     */
    public Optional<InntektsmeldingAggregat> getInntektsmeldinger() {
        return Optional.ofNullable(inntektsmeldinger);
    }

    void setInntektsmeldinger(InntektsmeldingAggregat inntektsmeldingAggregat) {
        this.inntektsmeldinger = inntektsmeldingAggregat;
    }

    /**
     * sjekkom bekreftet annen opptjening. Oppgi aktørId for matchende behandling (dvs.normalt søker).
     */
    public Optional<AktørArbeid> getBekreftetAnnenOpptjening(AktørId aktørId) {
        return getSaksbehandletVersjon()
            .map(InntektArbeidYtelseAggregat::getAktørArbeid)
            .flatMap(it -> it.stream().filter(aa -> aa.getAktørId().equals(aktørId))
                .findFirst());
    }

    public Optional<AktørArbeid> getAktørArbeidFraRegister(AktørId aktørId) {
        if (register != null) {
            var aktørArbeid = register.getAktørArbeid().stream().filter(aa -> Objects.equals(aa.getAktørId(), aktørId)).collect(Collectors.toList());
            if (aktørArbeid.size() > 1) {
                throw new IllegalStateException("Kan kun ha ett innslag av AktørArbeid for aktørId:" + aktørId + " i  grunnlag " + this.getEksternReferanse());
            }
            return aktørArbeid.stream().findFirst();
        }
        return Optional.empty();
    }

    public Optional<AktørYtelse> getAktørYtelseFraRegister(AktørId aktørId) {
        if (register != null) {
            var aktørYtelse = register.getAktørYtelse().stream().filter(aa -> Objects.equals(aa.getAktørId(), aktørId)).collect(Collectors.toList());
            if (aktørYtelse.size() > 1) {
                throw new IllegalStateException("Kan kun ha ett innslag av AktørYtelse for aktørId:" + aktørId + " i  grunnlag " + this.getEksternReferanse());
            }
            return aktørYtelse.stream().findFirst();
        }
        return Optional.empty();
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

    public Collection<AktørInntekt> getAlleAktørInntektFraRegister() {
        return register != null ? register.getAktørInntekt() : Collections.emptyList();
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

    public List<InntektsmeldingSomIkkeKommer> getInntektsmeldingerSomIkkeKommer() {
        if (arbeidsforholdInformasjon == null) {
            return Collections.emptyList();
        } else {
            var overstyringer = arbeidsforholdInformasjon.getOverstyringer();
            return overstyringer.stream()
                .filter(ov -> ov.kreverIkkeInntektsmelding())
                .map(ov -> {
                    // TODO (FC): fiks/fjern eksternRef herfra
                    EksternArbeidsforholdRef eksternRef = null; // arbeidsforholdInformasjon.finnEkstern(ov.getArbeidsgiver(), ov.getArbeidsforholdRef()); //
                                                                // NOSONAR
                    return new InntektsmeldingSomIkkeKommer(ov.getArbeidsgiver(), ov.getArbeidsforholdRef(), eksternRef);
                }) // NOSONAR
                .collect(Collectors.toList());
        }
    }

    public List<ArbeidsforholdOverstyring> getArbeidsforholdOverstyringer() {
        if (arbeidsforholdInformasjon == null) {
            return Collections.emptyList();
        }
        return arbeidsforholdInformasjon.getOverstyringer();
    }

    void setBehandling(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    void setAktivt(boolean aktiv) {
        this.aktiv = aktiv;
    }

    /** Hvorvidt dette er det siste (aktive grunnlaget) for en behandling. */
    public boolean isAktiv() {
        return aktiv;
    }

    void setRegister(InntektArbeidYtelseAggregat registerFør) {
        this.register = registerFør;
    }

    public Optional<ArbeidsforholdInformasjon> getArbeidsforholdInformasjon() {
        return Optional.ofNullable(arbeidsforholdInformasjon);
    }

    public Optional<UUID> getKoblingReferanse() {
        return Optional.empty();
    }

    void setInformasjon(ArbeidsforholdInformasjon informasjon) {
        this.arbeidsforholdInformasjon = informasjon;
    }

    void taHensynTilBetraktninger() {
        Optional.ofNullable(inntektsmeldinger).ifPresent(it -> it.taHensynTilBetraktninger(this.arbeidsforholdInformasjon));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof InntektArbeidYtelseGrunnlag))
            return false;
        InntektArbeidYtelseGrunnlag that = (InntektArbeidYtelseGrunnlag) o;
        return aktiv == that.aktiv &&
            Objects.equals(register, that.register) &&
            Objects.equals(saksbehandlet, that.saksbehandlet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(register, saksbehandlet);
    }

    public void fjernSaksbehandlet() {
        saksbehandlet = null;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

}
