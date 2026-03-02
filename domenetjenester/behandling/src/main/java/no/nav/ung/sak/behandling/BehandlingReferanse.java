package no.nav.ung.sak.behandling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Minimal metadata for en behandling.
 */
public class BehandlingReferanse {

    private Saksnummer saksnummer;

    private Long fagsakId;

    private Long behandlingId;

    private FagsakYtelseType fagsakYtelseType;

    /**
     * Søkers aktørid.
     */
    private AktørId aktørId;

    private BehandlingType behandlingType;

    /**
     * Original behandling id (i tilfelle dette f.eks er en revurdering av en annen behandling.
     */
    private Optional<Long> originalBehandlingId;

    private BehandlingStatus behandlingStatus;

    /** Eksternt refererbar UUID for behandling. */
    private UUID behandlingUuid;

    private BehandlingResultatType behandlingResultatType;

    private DatoIntervallEntitet fagsakPeriode;

    BehandlingReferanse() {
    }

    private BehandlingReferanse(FagsakYtelseType fagsakYtelseType, // NOSONAR
                                BehandlingType behandlingType,
                                BehandlingResultatType behandlingResultatType,
                                AktørId aktørId,
                                Saksnummer saksnummer,
                                Long fagsakId,
                                Long behandlingId,
                                UUID behandlingUuid,
                                Optional<Long> originalBehandlingId,
                                BehandlingStatus behandlingStatus,
                                DatoIntervallEntitet fagsakPeriode) { // NOSONAR
        this.fagsakYtelseType = fagsakYtelseType;
        this.behandlingType = behandlingType;
        this.behandlingResultatType = behandlingResultatType;
        this.aktørId = aktørId;
        this.saksnummer = saksnummer;
        this.fagsakId = fagsakId;
        this.behandlingId = behandlingId;
        this.behandlingUuid = behandlingUuid;
        this.originalBehandlingId = originalBehandlingId;
        this.behandlingStatus = behandlingStatus;
        this.fagsakPeriode = fagsakPeriode;
    }

    public static BehandlingReferanse fra(Behandling behandling) {
        return new BehandlingReferanse(behandling.getFagsakYtelseType(),
            behandling.getType(),
            behandling.getBehandlingResultatType(),
            behandling.getAktørId(),
            behandling.getFagsak().getSaksnummer(),
            behandling.getFagsakId(),
            behandling.getId(),
            behandling.getUuid(),
            behandling.getOriginalBehandlingId(),
            behandling.getStatus(),
            behandling.getFagsak().getPeriode());
    }

    public static BehandlingReferanse fra(FagsakYtelseType fagsakYtelseType, // NOSONAR
                                          BehandlingType behandlingType,
                                          BehandlingResultatType behandlingResultatType,
                                          AktørId aktørId,
                                          Saksnummer saksnummer,
                                          Long fagsakId,
                                          Long behandlingId,
                                          UUID behandlingUuid,
                                          Optional<Long> originalBehandlingId,
                                          BehandlingStatus behandlingStatus,
                                          DatoIntervallEntitet fagsakPeriode) { // NOSONAR
        return new BehandlingReferanse(fagsakYtelseType,
            behandlingType,
            behandlingResultatType,
            aktørId,
            saksnummer,
            fagsakId,
            behandlingId,
            behandlingUuid,
            originalBehandlingId,
            behandlingStatus,
            fagsakPeriode);
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public Long getId() {
        return getBehandlingId();
    }

    public Optional<Long> getOriginalBehandlingId() {
        return originalBehandlingId;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return fagsakYtelseType;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    /**
     * returnerer BehandlingResultatType. Hvis det endrer seg underveis i prosessen som pågår, så vær oppmerksom på at dette er et snapshot fra
     * da steget startet, og reflekterer ikke nødvendigvis endringer.
     */
    public BehandlingResultatType getBehandlingResultat() {
        return behandlingResultatType;
    }

    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    public boolean erRevurdering() {
        return BehandlingType.REVURDERING.equals(behandlingType);
    }

    public DatoIntervallEntitet getFagsakPeriode() {
        return fagsakPeriode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnummer, behandlingId, originalBehandlingId, fagsakYtelseType, behandlingType, aktørId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        BehandlingReferanse other = (BehandlingReferanse) obj;
        return Objects.equals(behandlingId, other.behandlingId)
            && Objects.equals(saksnummer, other.saksnummer)
            && Objects.equals(aktørId, other.aktørId)
            && Objects.equals(fagsakYtelseType, other.fagsakYtelseType)
            && Objects.equals(behandlingType, other.behandlingType)
            && Objects.equals(originalBehandlingId, other.originalBehandlingId)
        // tar ikke med status i equals siden den kan endre seg
        ;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + String.format(
            "<saksnummer=%s, behandlingId=%s, fagsakType=%s, behandlingType=%s, status=%s, originalBehandlingId=%s>",
            saksnummer, behandlingId, fagsakYtelseType, behandlingType, behandlingStatus, originalBehandlingId);
    }

    public BehandlingStatus getBehandlingStatus() {
        return behandlingStatus;
    }

}
