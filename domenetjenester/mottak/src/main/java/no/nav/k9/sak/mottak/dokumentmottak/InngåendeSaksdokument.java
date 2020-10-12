package no.nav.k9.sak.mottak.dokumentmottak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.typer.JournalpostId;

public class InngåendeSaksdokument {

    private Long fagsakId;
    private JournalpostId journalpostId;
    private LocalDate forsendelseMottatt;
    private LocalDateTime forsendelseMottattTidspunkt;
    private Boolean elektroniskSøknad;
    private String payload;
    private UUID forsendelseId;
    private String kanalreferanse;
    private String journalEnhet;
    private Brevkode type;
    private FagsakYtelseType fagsakYtelseType;

    private InngåendeSaksdokument() {
        // Skjult.
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public LocalDate getForsendelseMottatt() {
        return forsendelseMottatt;
    }

    public LocalDateTime getForsendelseMottattTidspunkt() {
        return forsendelseMottattTidspunkt;
    }

    public Boolean isElektroniskSøknad() {
        return elektroniskSøknad;
    }

    public String getPayload() {
        return payload;
    }

    public Brevkode getType() {
        return type;
    }

    public UUID getForsendelseId() {
        return forsendelseId;
    }

    public String getKanalreferanse() {
        return kanalreferanse;
    }

    public String getJournalEnhet() {
        return journalEnhet;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return fagsakYtelseType;
    }

    public static Builder builder() {
        return new Builder(new InngåendeSaksdokument());
    }

    public static class Builder {
        private final InngåendeSaksdokument kladd;
        private boolean built;

        Builder(InngåendeSaksdokument kladd) {
            this.kladd = kladd;
            this.kladd.elektroniskSøknad = Boolean.TRUE;
        }

        public InngåendeSaksdokument.Builder medFagsak(Long fagsakId, FagsakYtelseType fagsakYtelseType) {
            this.kladd.fagsakId = fagsakId;
            this.kladd.fagsakYtelseType = fagsakYtelseType;
            return this;
        }

        public InngåendeSaksdokument.Builder medJournalpostId(JournalpostId journalpostId) {
            this.kladd.journalpostId = journalpostId;
            return this;
        }

        public InngåendeSaksdokument.Builder medForsendelseId(UUID forsendelseId) {
            this.kladd.forsendelseId = forsendelseId;
            return this;
        }

        public InngåendeSaksdokument.Builder medForsendelseMottatt(LocalDate forsendelseMottatt) {
            this.kladd.forsendelseMottatt = forsendelseMottatt;
            return this;
        }

        public InngåendeSaksdokument.Builder medForsendelseMottatt(LocalDateTime forsendelseMottatt) {
            this.kladd.forsendelseMottattTidspunkt = forsendelseMottatt;
            return this;
        }

        public InngåendeSaksdokument.Builder medElektroniskSøknad(Boolean elektroniskSøknad) {
            this.kladd.elektroniskSøknad = elektroniskSøknad;
            return this;
        }

        public InngåendeSaksdokument.Builder medKanalreferanse(String kanalreferanse) {
            this.kladd.kanalreferanse = kanalreferanse;
            return this;
        }

        public InngåendeSaksdokument.Builder medJournalførendeEnhet(String journalEnhet) {
            this.kladd.journalEnhet = journalEnhet;
            return this;
        }

        public InngåendeSaksdokument.Builder medPayload(String payloadXml) {
            this.kladd.payload = payloadXml;
            return this;
        }

        public Builder medType(Brevkode type) {
            this.kladd.type = type;
            return this;
        }

        public InngåendeSaksdokument build() {
            if (built) {
                throw new IllegalStateException("Kan ikke kalle flere ganger for " + InngåendeSaksdokument.class.getSimpleName() + ", journalpostId=" + kladd.journalpostId);
            }
            built = true;
            return kladd;
        }

    }
}
