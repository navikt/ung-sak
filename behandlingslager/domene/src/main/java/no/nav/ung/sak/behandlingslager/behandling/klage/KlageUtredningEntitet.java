package no.nav.ung.sak.behandlingslager.behandling.klage;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.part.PartEntitet;

import java.util.*;

@Entity(name = "KlageUtredning")
@Table(name = "KLAGE_UTREDNING")
public class KlageUtredningEntitet extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KLAGE_UTREDNING")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @Column(name = "paaklaget_behandling_id", nullable = false, updatable = false, unique = true)
    private Long påklagetBehandlingId;

    @Column(name = "behandlende_enhet")
    private String opprinneligBehandlendeEnhet;

    @Column(name = "godkjent_av_medunderskriver", nullable = false)
    private boolean godkjentAvMedunderskriver;

    @OneToOne(cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name = "formkrav_id")
    private KlageFormkravEntitet formkrav;

    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name = "klage_utredning_id")
    private Set<KlageVurderingEntitet> klagevurderinger;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "part_id")
    private PartEntitet klagendePart;

    public KlageUtredningEntitet() {
        // Hibernate
    }

    public static Builder builder() {
        return new Builder();
    }

//    public Optional<UUID> getPåKlagBehandlingRef() {
//        return Optional.ofNullable(påKlagdBehandlingRef);
//    }
//
//    public void setPåKlagdBehandlingRef(UUID påKlagdBehandlingRef) {
//        this.påKlagdBehandlingRef = påKlagdBehandlingRef;
//    }
//
//    public Optional<BehandlingType> getPåKlagdBehandlingType() {
//        return Optional.ofNullable(påKlagdBehandlingType);
//    }
//
//    public void setPåKlagdBehandlingType(BehandlingType påKlagdBehandlingType) {
//        this.påKlagdBehandlingType = påKlagdBehandlingType;
//    }

    public boolean isGodkjentAvMedunderskriver() {
        return godkjentAvMedunderskriver;
    }

    public void setGodkjentAvMedunderskriver(boolean godkjentAvMedunderskriver) {
        this.godkjentAvMedunderskriver = godkjentAvMedunderskriver;
    }

    public Optional<KlageFormkravAdapter> getFormkrav() {
        // Formkrav er ikke tilgjengelig for klagevurdering mottatt fra Kabal
        return Optional.ofNullable(formkrav == null ? null : formkrav.tilFormkrav());
    }

    public void setFormkrav(KlageFormkravAdapter formkravAdapter) {
        if (!harFormkrav()) {
            formkrav = new KlageFormkravEntitet();
        }
        formkrav.oppdater(formkravAdapter);
    }

    public boolean erKlageHjemsendt() {
        return KlageVurderingType.HJEMSENDE_UTEN_Å_OPPHEVE.equals(hentGjeldendeKlagevurderingType());
    }

    public KlageVurderingType hentGjeldendeKlagevurderingType() {
        return getKlageVurderingType(KlageVurdertAv.NK_KABAL).or(() ->
            getKlageVurderingType(KlageVurdertAv.NAY)
        ).orElse(null);
    }

    public Optional<KlageVurderingType> getKlageVurderingType(KlageVurdertAv klageVurdertAv) {
        var klagevurdering = getKlagevurdering(klageVurdertAv);
        return klagevurdering
            .map(kv -> kv.getKlageresultat().getKlageVurdering())
            .orElseGet(() -> formkrav.tilFormkrav().erAvvist() ? Optional.of(KlageVurderingType.AVVIS_KLAGE) : Optional.empty());
    }

    public boolean harFormkrav() {
        return formkrav != null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof KlageUtredningEntitet)) {
            return false;
        }
        KlageUtredningEntitet other = (KlageUtredningEntitet) obj;
        return Objects.equals(this.id, other.id) //Skal det sammenliknes på id?
            && Objects.equals(this.behandlingId, other.behandlingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, behandlingId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            (id != null ? "id=" + id + ", " : "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "klageBehandling=" + behandlingId + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + ">"; //$NON-NLS-1$
    }

    public String getOpprinneligBehandlendeEnhet() {
        return opprinneligBehandlendeEnhet;
    }

    public void setKlagevurdering(KlageVurderingAdapter adapter) {
        Vurderingresultat nyVurdering = new Vurderingresultat(adapter);
        var klagevurdering = getKlagevurdering(adapter.getKlageVurdertAv());
        klagevurdering.ifPresentOrElse(kv ->
            kv.setKlageresultat(nyVurdering),
            () -> {
                KlageVurderingEntitet.Builder klageVurderingResultatBuilder = new KlageVurderingEntitet.Builder()
                    .medKlageVurdertAv(adapter.getKlageVurdertAv())
                    .medKlageutredningId(this.id)
                    .medResultat(new Vurderingresultat(adapter));
                klagevurderinger.add(klageVurderingResultatBuilder.build());
            }
        );
    }

    public void nullstillVurdering() {
        formkrav = null;
        klagevurderinger = null;
    }

    public Optional<PartEntitet> getKlagendePart() {
        return Optional.ofNullable(klagendePart);
    }

    public void setKlagendePart(PartEntitet klagendePart) {
        this.klagendePart = klagendePart;
    }

    public static class Builder {
        private KlageUtredningEntitet klageUtredningMal;

        public Builder() {
            klageUtredningMal = new KlageUtredningEntitet();
        }

        public Builder medOpprinneligBehandlendeEnhet(String opprinneligBehandlendeEnhet) {
            klageUtredningMal.opprinneligBehandlendeEnhet = opprinneligBehandlendeEnhet;
            return this;
        }

        public Builder medKlageBehandling(Behandling klageBehandling) {
            klageUtredningMal.behandlingId = klageBehandling.getId();
            return this;
        }

        public Builder medPåklagetBehandlingId(Long påklagetBehandlingId) {
            klageUtredningMal.påklagetBehandlingId = påklagetBehandlingId;
            return this;
        }

        public KlageUtredningEntitet.Builder medFormkrav(KlageFormkravAdapter formkrav) {
            klageUtredningMal.formkrav = KlageFormkravEntitet.builder()
                .medFormkrav(formkrav)
                .build();
            return this;
        }

        public KlageUtredningEntitet build() {
            verifyStateForBuild();
            return klageUtredningMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(klageUtredningMal.behandlingId, "KlageBehandling");
        }
    }

    public Optional<KlageVurderingEntitet> getKlagevurdering(KlageVurdertAv klageVurdertAv) {
        return klagevurderinger.stream()
            .filter(klageVurderingEntitet -> klageVurdertAv.equals(klageVurderingEntitet.getVurdertAvEnhet()))
            .findFirst();
    }
}
