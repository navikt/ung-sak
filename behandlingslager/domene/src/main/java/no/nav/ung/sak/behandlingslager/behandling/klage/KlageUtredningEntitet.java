package no.nav.ung.sak.behandlingslager.behandling.klage;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.klage.KlageAvvistÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.part.PartEntitet;
import no.nav.ung.sak.behandlingslager.kodeverk.BehandlingTypeKodeverdiConverter;

import java.util.*;

@Entity(name = "KlageUtredning")
@Table(name = "KLAGE_UTREDNING")
public class KlageUtredningEntitet extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KLAGE_UTREDNING")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Column(name = "paaklagd_behandling_uuid")
    private UUID påklagdBehandlingUuid;

    @Convert(converter = BehandlingTypeKodeverdiConverter.class)
    @Column(name = "paaklagd_behandling_type")
    private BehandlingType påklagdBehandlingType;

    @Column(name = "behandlende_enhet", nullable = false, updatable = false)
    private String opprinneligBehandlendeEnhet;

    @Column(name = "godkjent_av_medunderskriver", nullable = false)
    private boolean godkjentAvMedunderskriver;

    @OneToOne(cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name = "formkrav_id")
    private KlageFormkravEntitet formkrav;

    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name = "klage_utredning_id")
    private Set<KlageVurderingEntitet> klagevurderinger = new HashSet<>();

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "part_id")
    private PartEntitet klagendePart;

    public KlageUtredningEntitet() {
        // Hibernate
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<UUID> getpåklagdBehandlingRef() {
        return Optional.ofNullable(påklagdBehandlingUuid);
    }

    public void setpåklagdBehandlingRef(UUID påklagdBehandlingUuid) {
        this.påklagdBehandlingUuid = påklagdBehandlingUuid;
    }

    public Optional<BehandlingType> getpåklagdBehandlingType() {
        return Optional.ofNullable(påklagdBehandlingType);
    }

    public void setpåklagdBehandlingType(BehandlingType påklagdBehandlingType) {
        this.påklagdBehandlingType = påklagdBehandlingType;
    }

    public boolean isGodkjentAvMedunderskriver() {
        return godkjentAvMedunderskriver;
    }

    public void setGodkjentAvMedunderskriver(boolean godkjentAvMedunderskriver) {
        this.godkjentAvMedunderskriver = godkjentAvMedunderskriver;
    }

    public Optional<KlageFormkravEntitet> getFormkrav() {
        // Formkrav er ikke tilgjengelig for klagevurdering mottatt fra Kabal
        return Optional.ofNullable(formkrav);
    }

    public Optional<KlageAvvistÅrsak> setFormkrav(KlageFormkravAdapter formkravAdapter) {
        if (!harFormkrav()) {
            formkrav = new KlageFormkravEntitet();
        }
        formkrav.oppdater(formkravAdapter);
        if (!formkrav.hentAvvistÅrsaker().isEmpty()) {
            // TODO: Utled og lagre hjemmel brukt i avvisning, i vurdering
            setKlagevurdering(KlageVurderingAdapter.Templates.AVVIST_VURDERING_VEDTAKSINSTANS);
        }
        return formkrav.utledAvvistÅrsak();
    }

    public boolean erKlageHjemsendt() {
        return KlageVurderingType.HJEMSENDE_UTEN_Å_OPPHEVE.equals(hentGjeldendeKlagevurderingType());
    }

    public boolean erKlageAvvist() {
        return KlageVurderingType.AVVIS_KLAGE.equals(hentGjeldendeKlagevurderingType());
    }

    public KlageVurderingType hentGjeldendeKlagevurderingType() {
        return getKlageVurderingType(KlageVurdertAv.KLAGEINSTANS).or(() ->
            getKlageVurderingType(KlageVurdertAv.VEDTAKSINSTANS)
        ).orElse(null);
    }

    public Optional<KlageVurderingType> getKlageVurderingType(KlageVurdertAv klageVurdertAv) {
        var klagevurdering = hentKlagevurdering(klageVurdertAv);
        return klagevurdering
            .map(kv -> kv.getKlageresultat().getKlageVurdering())
            .orElseGet(() -> getFormkrav()
                .map(KlageFormkravEntitet::tilFormkrav)
                .flatMap(KlageFormkravAdapter::hentVurderingTypeHvisAvvist));
    }

    public boolean harFormkrav() {
        return formkrav != null;
    }

    public String getOpprinneligBehandlendeEnhet() {
        return opprinneligBehandlendeEnhet;
    }

    public void setKlagevurdering(KlageVurderingAdapter adapter) {
        Vurderingresultat nyVurdering = new Vurderingresultat(adapter);
        var klagevurdering = hentKlagevurdering(adapter.getKlageVurdertAv());
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

    public Optional<KlageVurderingEntitet> hentKlagevurdering(KlageVurdertAv klageVurdertAv) {
        return klagevurderinger.stream()
            .filter(klageVurderingEntitet -> klageVurdertAv.equals(klageVurderingEntitet.getVurdertAvEnhet()))
            .findFirst();
    }

    public void fjernKlageVurderingVedtaksinstans() {
        klagevurderinger.removeIf((vurdering) ->
            KlageVurdertAv.VEDTAKSINSTANS.equals(vurdering.getVurdertAvEnhet()));
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

        public Builder medpåklagdBehandlingId(UUID påklagdBehandlingUuid) {
            klageUtredningMal.påklagdBehandlingUuid = påklagdBehandlingUuid;
            return this;
        }

        public KlageUtredningEntitet.Builder medFormkrav(KlageFormkravAdapter formkrav) {
            klageUtredningMal.formkrav = KlageFormkravEntitet.builder()
                .medFormkrav(formkrav)
                .build();
            return this;
        }

        public Builder medId(Long id) {
            klageUtredningMal.id = id;
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
}
