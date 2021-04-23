package no.nav.k9.sak.domene.iay.modell;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.typer.JournalpostId;

public class OppgittOpptjening {

    private UUID uuid;

    private JournalpostId journalpostId;

    private LocalDateTime innsendingstidspunkt;

    @ChangeTracked
    private List<OppgittArbeidsforhold> oppgittArbeidsforhold;

    @ChangeTracked
    private List<OppgittEgenNæring> egenNæring;

    @ChangeTracked
    private List<OppgittAnnenAktivitet> annenAktivitet;

    @ChangeTracked
    private OppgittFrilans frilans;

    private LocalDateTime opprettetTidspunkt;

    OppgittOpptjening() {
    }

    public OppgittOpptjening(UUID eksternReferanse) {
        Objects.requireNonNull(eksternReferanse, "eksternReferanse");
        this.uuid = eksternReferanse;
        // setter tidspunkt til nå slik at dette også er satt for nybakte objekter uten å lagring
        this.opprettetTidspunkt = LocalDateTime.now();
    }

    OppgittOpptjening(UUID eksternReferanse, LocalDateTime opprettetTidspunktOriginalt) {
        Objects.requireNonNull(eksternReferanse, "eksternReferanse");
        this.uuid = eksternReferanse;
        this.opprettetTidspunkt = opprettetTidspunktOriginalt;
    }

    OppgittOpptjening(OppgittOpptjening kopierFra) {
        this.journalpostId = kopierFra.journalpostId;
        this.innsendingstidspunkt = kopierFra.innsendingstidspunkt;

        this.oppgittArbeidsforhold = kopierFra.oppgittArbeidsforhold == null
            ? new ArrayList<>()
            : kopierFra.oppgittArbeidsforhold.stream().map(OppgittArbeidsforhold::new).collect(Collectors.toList());

        this.egenNæring = kopierFra.egenNæring == null
            ? new ArrayList<>()
            : kopierFra.egenNæring.stream().map(OppgittEgenNæring::new).collect(Collectors.toList());

        this.annenAktivitet = kopierFra.annenAktivitet == null
            ? new ArrayList<>()
            : kopierFra.annenAktivitet.stream().map(OppgittAnnenAktivitet::new).collect(Collectors.toList());

        this.frilans = kopierFra.frilans == null ? null : new OppgittFrilans(kopierFra.frilans);
    }

    OppgittOpptjening(OppgittOpptjening kopierFra, UUID eksternReferanse, LocalDateTime opprettetTidspunktOriginalt) {
        Objects.requireNonNull(eksternReferanse, "eksternReferanse");
        this.uuid = eksternReferanse;
        this.opprettetTidspunkt = opprettetTidspunktOriginalt;

        this.oppgittArbeidsforhold = kopierFra.oppgittArbeidsforhold == null
            ? new ArrayList<>()
            : kopierFra.oppgittArbeidsforhold.stream().map(OppgittArbeidsforhold::new).collect(Collectors.toList());

        this.egenNæring = kopierFra.egenNæring == null
            ? new ArrayList<>()
            : kopierFra.egenNæring.stream().map(OppgittEgenNæring::new).collect(Collectors.toList());

        this.annenAktivitet = kopierFra.annenAktivitet == null
            ? new ArrayList<>()
            : kopierFra.annenAktivitet.stream().map(OppgittAnnenAktivitet::new).collect(Collectors.toList());

        this.frilans = kopierFra.frilans == null ? null : new OppgittFrilans(kopierFra.frilans);
    }

    /**
     * Identifisere en immutable instans av grunnlaget unikt og er egnet for utveksling (eks. til abakus eller andre systemer)
     */
    public UUID getEksternReferanse() {
        return uuid;
    }

    public List<OppgittArbeidsforhold> getOppgittArbeidsforhold() {
        if (this.oppgittArbeidsforhold == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(oppgittArbeidsforhold);
    }

    public List<OppgittEgenNæring> getEgenNæring() {
        if (this.egenNæring == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(egenNæring);
    }

    public List<OppgittAnnenAktivitet> getAnnenAktivitet() {
        if (this.annenAktivitet == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(annenAktivitet);
    }

    public Optional<OppgittFrilans> getFrilans() {
        return Optional.ofNullable(frilans);
    }

    void setFrilans(OppgittFrilans frilans) {
        this.frilans = frilans;
    }

    void leggTilAnnenAktivitet(OppgittAnnenAktivitet annenAktivitet) {
        if (this.annenAktivitet == null) {
            this.annenAktivitet = new ArrayList<>();
        }
        if (annenAktivitet != null) {
            this.annenAktivitet.add(annenAktivitet);
        }
    }

    void leggTilEgenNæring(OppgittEgenNæring egenNæring) {
        if (this.egenNæring == null) {
            this.egenNæring = new ArrayList<>();
        }
        if (egenNæring != null) {
            this.egenNæring.add(egenNæring);
        }
    }

    void leggTilOppgittArbeidsforhold(OppgittArbeidsforhold oppgittArbeidsforhold) {
        if (this.oppgittArbeidsforhold == null) {
            this.oppgittArbeidsforhold = new ArrayList<>();
        }
        if (oppgittArbeidsforhold != null) {
            this.oppgittArbeidsforhold.add(oppgittArbeidsforhold);
        }
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    void setJournalpostId(JournalpostId journalpostId) {
        this.journalpostId = journalpostId;
    }

    // Obs: Ønsker du å bruke denne, eller tidspunkt på {@link MottattDokument#getInnsendingstidspunkt}?
    public LocalDateTime getInnsendingstidspunkt() {
        return innsendingstidspunkt;
    }

    void setInnsendingstidspunkt(LocalDateTime innsendingstidspunkt) {
        this.innsendingstidspunkt = innsendingstidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof OppgittOpptjening))
            return false;
        OppgittOpptjening that = (OppgittOpptjening) o;
        return Objects.equals(oppgittArbeidsforhold, that.oppgittArbeidsforhold) &&
            Objects.equals(egenNæring, that.egenNæring) &&
            Objects.equals(annenAktivitet, that.annenAktivitet) &&
            Objects.equals(frilans, that.frilans) &&
            Objects.equals(journalpostId, that.journalpostId) &&
            Objects.equals(innsendingstidspunkt, that.innsendingstidspunkt);

    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgittArbeidsforhold, egenNæring, annenAktivitet, frilans, journalpostId, innsendingstidspunkt);
    }

    @Override
    public String toString() {
        return "OppgittOpptjeningEntitet{" +
            "oppgittArbeidsforhold=" + oppgittArbeidsforhold +
            ", egenNæring=" + egenNæring +
            ", frilans=" + frilans +
            ", annenAktivitet=" + annenAktivitet +
            '}';
    }

    /**
     * Brukes til å filtrere bort tomme oppgitt opptjening elementer ved migrering. Bør ikke være nødvendig til annet.
     * <p>
     * har minst noe av oppgitt arbeidsforhold, egen næring, annen aktivitet eller frilans.
     */
    public boolean harOpptjening() {
        return !getOppgittArbeidsforhold().isEmpty() || !getEgenNæring().isEmpty() || !getAnnenAktivitet().isEmpty() || !getFrilans().isEmpty();
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }
}
