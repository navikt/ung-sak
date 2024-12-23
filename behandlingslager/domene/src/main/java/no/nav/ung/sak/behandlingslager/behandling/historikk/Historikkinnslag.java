package no.nav.ung.sak.behandlingslager.behandling.historikk;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.NaturalId;

import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.historikk.HistorikkinnslagType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

@Entity(name = "Historikkinnslag")
@Table(name = "HISTORIKKINNSLAG")
@DynamicInsert
@DynamicUpdate
public class Historikkinnslag extends BaseEntitet {

    public static final Comparator<? super Historikkinnslag> COMP_REKKEFØLGE = Comparator.comparing(Historikkinnslag::getOpprettetTidspunkt, Comparator.nullsLast(Comparator.naturalOrder()));

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_HISTORIKKINNSLAG")
    private Long id;

    /**
     * UUID for historikkinnslaget - Vil være generert i det system som originalt oppretter innslaget,
     * Så det vil enten være FPSAK eller systemet som står i
     */
    @NaturalId
    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "behandling_id", updatable = false)
    private Long behandlingId;

    @Column(name = "fagsak_id", nullable = false, updatable = false)
    private Long fagsakId;

    @Convert(converter = HistorikkAktørKodeverdiConverter.class)
    @Column(name = "historikk_aktoer_id", nullable = false)
    private HistorikkAktør aktør = HistorikkAktør.UDEFINERT;

    @Convert(converter = HistorikkinnslagTypeKodeverdiConverter.class)
    @Column(name = "historikkinnslag_type", nullable = false)
    private HistorikkinnslagType type = HistorikkinnslagType.UDEFINERT;

    @OneToMany(mappedBy = "historikkinnslag", cascade = CascadeType.ALL)
    private List<HistorikkinnslagDokumentLink> dokumentLinker = new ArrayList<>();

    @OneToMany(mappedBy = "historikkinnslag")
    private List<HistorikkinnslagDel> historikkinnslagDeler = new ArrayList<>();

    /**
     * Foreløpig inneholder denne kun tidspunkt for når historikkinnslag er opprettet eksternt
     * Eksisterende opprettet tid burde migreres og kopieres over hit, så en enkelt kan sortere kronologisk på
     * når historikkhendelsen egentlig oppstod
     */
    @Column(name = "historikk_tid", updatable = false)
    private LocalDateTime historikkTid;

    @Column(name = "opprettet_i_system", updatable = false)
    private String opprettetISystem = Fagsystem.K9SAK.getKode();  // FIXME: Bytt til UNG_SAK når det er støttet

    public Historikkinnslag() {
        this.uuid = UUID.randomUUID();
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        this.behandlingId = behandlingId;
    }

    public void setBehandling(Behandling behandling) {
        this.behandlingId = behandling.getId();
        this.fagsakId = behandling.getFagsakId();
    }

    public HistorikkAktør getAktør() {
        return Objects.equals(HistorikkAktør.UDEFINERT, aktør) ? null : aktør;
    }

    public void setAktør(HistorikkAktør aktør) {
        this.aktør = aktør == null ? HistorikkAktør.UDEFINERT : aktør;
    }

    public HistorikkinnslagType getType() {
        return type;
    }

    public void setType(HistorikkinnslagType type) {
        this.type = type;
    }

    public List<HistorikkinnslagDokumentLink> getDokumentLinker() {
        return dokumentLinker;
    }

    public void setDokumentLinker(List<HistorikkinnslagDokumentLink> dokumentLinker) {
        this.dokumentLinker = dokumentLinker;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public void setFagsakId(Long fagsakId) {
        this.fagsakId = fagsakId;
    }

    public List<HistorikkinnslagDel> getHistorikkinnslagDeler() {
        return historikkinnslagDeler;
    }

    public void setHistorikkinnslagDeler(List<HistorikkinnslagDel> historikkinnslagDeler) {
        historikkinnslagDeler.forEach(del -> HistorikkinnslagDel.builder(del).medHistorikkinnslag(this));
        this.historikkinnslagDeler = historikkinnslagDeler;
    }

    public String getOpprettetISystem() {
        return opprettetISystem;
    }

    public LocalDateTime getHistorikkTid() {
        return historikkTid;
    }

    public void setOpprettetAv(String ansvarligSaksbehandler) {
        this.opprettetAv = ansvarligSaksbehandler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Historikkinnslag)) {
            return false;
        }
        Historikkinnslag that = (Historikkinnslag) o;
        // FIXME: Her burde ikke generert id vært del av equals/hashcode. Bør fjernes. Evt. droppe equals/hashcode helt
        return Objects.equals(id, that.id) &&
            Objects.equals(behandlingId, that.behandlingId) &&
            Objects.equals(fagsakId, that.fagsakId) &&
            Objects.equals(getAktør(), that.getAktør()) &&
            Objects.equals(type, that.type) &&
            Objects.equals(getDokumentLinker(), that.getDokumentLinker());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, behandlingId, fagsakId, getAktør(), type, getDokumentLinker());
    }

    public static class Builder {
        private Historikkinnslag historikkinnslag;

        public Builder() {
            historikkinnslag = new Historikkinnslag();
        }

        public Builder medBehandlingId(Long behandlingId) {
            historikkinnslag.behandlingId = behandlingId;
            return this;
        }

        public Builder medUuid(UUID uuid) {
            historikkinnslag.uuid = uuid;
            return this;
        }

        public Builder medFagsakId(Long fagsakId) {
            historikkinnslag.fagsakId = fagsakId;
            return this;
        }

        public Builder medAktør(HistorikkAktør historikkAktør) {
            historikkinnslag.aktør = historikkAktør;
            return this;
        }

        public Builder medType(HistorikkinnslagType type) {
            historikkinnslag.type = type;
            return this;
        }

        public Builder medHistorikkTid(LocalDateTime historikkTid) {
            historikkinnslag.historikkTid = historikkTid;
            return this;
        }

        public Builder medOpprettetISystem(String opprettetISystem) {
            historikkinnslag.opprettetISystem = opprettetISystem;
            return this;
        }

        public Builder medOpprettetAv(String opprettetAv) {
            historikkinnslag.opprettetAv = opprettetAv;
            return this;
        }

        public Builder medDokumentLinker(List<HistorikkinnslagDokumentLink> dokumentLinker) {
            if (historikkinnslag.dokumentLinker == null) {
                historikkinnslag.dokumentLinker = dokumentLinker;
            } else if (dokumentLinker != null) {
                historikkinnslag.dokumentLinker.addAll(dokumentLinker);
            }
            return this;
        }

        public Historikkinnslag build() {
            return historikkinnslag;
        }
    }
}
