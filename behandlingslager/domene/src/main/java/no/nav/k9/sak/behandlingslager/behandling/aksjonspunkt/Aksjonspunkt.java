package no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.InternalUtil;
import no.nav.k9.sak.behandlingslager.kodeverk.AksjonspunktDefinisjonKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.AksjonspunktStatusKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.BehandlingStegTypeKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.VenteårsakKodeverdiConverter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "Aksjonspunkt")
@Table(name = "AKSJONSPUNKT")
@DynamicInsert
@DynamicUpdate
public class Aksjonspunkt extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_AKSJONSPUNKT")
    private Long id;

    @Column(name = "frist_tid")
    private LocalDateTime fristTid;

    @Convert(converter = AksjonspunktDefinisjonKodeverdiConverter.class)
    @Column(name = "aksjonspunkt_def", nullable = false, updatable = false)
    private AksjonspunktDefinisjon aksjonspunktDefinisjon;

    @Convert(converter = BehandlingStegTypeKodeverdiConverter.class)
    @Column(name = "behandling_steg_funnet")
    private BehandlingStegType behandlingSteg;

    @Convert(converter = AksjonspunktStatusKodeverdiConverter.class)
    @Column(name = "aksjonspunkt_status", nullable = false)
    private AksjonspunktStatus status;

    @Convert(converter = VenteårsakKodeverdiConverter.class)
    @Column(name="vent_aarsak")
    private Venteårsak venteårsak = Venteårsak.UDEFINERT;

    @Column(name="vent_aarsak_variant")
    private String venteårsakVariant;

    @Column(name="ansvarlig_saksbehandler")
    private String ansvarligSaksbehandler;

    @Version
    @Column(name = "versjon", nullable = false)
    private Long versjon;

    /**
     * Saksbehandler begrunnelse som settes ifm at et aksjonspunkt settes til utført.
     */
    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fomDato", column = @Column(name = "periode_fom")),
            @AttributeOverride(name = "tomDato", column = @Column(name = "periode_tom"))
    })
    private DatoIntervallEntitet periode;


    @Column(name = "TOTRINN_BEHANDLING", nullable = false)
    private boolean toTrinnsBehandling;

    Aksjonspunkt() {
        // for hibernate
    }

    protected Aksjonspunkt(AksjonspunktDefinisjon aksjonspunktDef, BehandlingStegType behandlingStegFunnet) {
        Objects.requireNonNull(behandlingStegFunnet, "behandlingStegFunnet"); //$NON-NLS-1$
        Objects.requireNonNull(aksjonspunktDef, "aksjonspunktDef"); //$NON-NLS-1$
        this.behandlingSteg = behandlingStegFunnet;
        this.aksjonspunktDefinisjon = aksjonspunktDef;
        this.toTrinnsBehandling = aksjonspunktDef.getDefaultTotrinnBehandling();
        this.status = AksjonspunktStatus.OPPRETTET;
    }

    protected Aksjonspunkt(AksjonspunktDefinisjon aksjonspunktDef) {
        Objects.requireNonNull(aksjonspunktDef, "aksjonspunktDef"); //$NON-NLS-1$
        this.aksjonspunktDefinisjon = aksjonspunktDef;
        this.toTrinnsBehandling = aksjonspunktDef.getDefaultTotrinnBehandling();
        this.status = AksjonspunktStatus.OPPRETTET;
    }

    public Long getId() {
        return id;
    }

    /**
     * Hvorvidt et Aksjonspunkt er av typen Autopunkt.
     * <p>
     * NB: Ikke bruk dette til å styre på vent eller lignende. Bruk
     * egenskapene til Aksjonspunktet i stedet (eks. hvorvidt det har en frist).
     */
    public boolean erAutopunkt() {
        return getAksjonspunktDefinisjon() != null && getAksjonspunktDefinisjon().erAutopunkt();
    }

    public boolean erManueltOpprettet() {
        return this.aksjonspunktDefinisjon.getAksjonspunktType() != null && this.aksjonspunktDefinisjon.getAksjonspunktType().erOverstyringpunkt();
    }

    public AksjonspunktDefinisjon getAksjonspunktDefinisjon() {
        return aksjonspunktDefinisjon;
    }

    public AksjonspunktStatus getStatus() {
        return status;
    }

    public boolean tilbakehoppVedGjenopptakelse() {
        return aksjonspunktDefinisjon.tilbakehoppVedGjenopptakelse();
    }

    /**
     * Sett til utført med gitt begrunnelse. Returner true dersom ble endret, false dersom allerede var utfør og hadde
     * samme begrunnelse.
     *
     * @return true hvis status eller begrunnelse er endret.
     */
    boolean setStatus(AksjonspunktStatus nyStatus, String begrunnelse) {
        boolean statusEndret = !Objects.equals(getStatus(), nyStatus);

        if (statusEndret) {
            if (Objects.equals(nyStatus, AksjonspunktStatus.UTFØRT)) {
                validerIkkeAvbruttAllerede();
            }

            this.status = nyStatus;
        }

        boolean begrunnelseEndret = !Objects.equals(getBegrunnelse(), begrunnelse);
        if (begrunnelseEndret) {
            setBegrunnelse(begrunnelse);
        }

        return begrunnelseEndret || statusEndret;
    }

    public BehandlingStegType getBehandlingStegFunnet() {
        return behandlingSteg;
    }

    public LocalDateTime getFristTid() {
        return fristTid;
    }

    void setFristTid(LocalDateTime fristTid) {
        this.fristTid = fristTid;
    }

    public boolean erOpprettet() {
        return Objects.equals(getStatus(), AksjonspunktStatus.OPPRETTET);
    }

    public boolean erÅpentAksjonspunkt() {
        return status.erÅpentAksjonspunkt();
    }

    public boolean skalAvbrytesVedTilbakehopp() {
        return aksjonspunktDefinisjon.getSkalAvbrytesVedTilbakeføring();
    }

    static Optional<Aksjonspunkt> finnEksisterende(Behandling behandling, AksjonspunktDefinisjon ap) {
        return behandling.getAksjonspunktMedDefinisjonOptional(ap);
    }

    /**
     * Returner liste av abstraktpunkt definisjon koder.
     */
    public static List<String> getKoder(List<Aksjonspunkt> abstraktpunkter) {
        return abstraktpunkter.stream().map(ap -> ap.getAksjonspunktDefinisjon().getKode()).collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Aksjonspunkt)) {
            return false;
        }
        Aksjonspunkt kontrollpunkt = (Aksjonspunkt) object;
        return Objects.equals(getAksjonspunktDefinisjon(), kontrollpunkt.getAksjonspunktDefinisjon())
            && Objects.equals(getStatus(), kontrollpunkt.getStatus())
            && Objects.equals(getPeriode(), kontrollpunkt.getPeriode())
            && Objects.equals(getFristTid(), kontrollpunkt.getFristTid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAksjonspunktDefinisjon(), getStatus(), getPeriode(), getFristTid());
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public boolean isToTrinnsBehandling() {
        return toTrinnsBehandling || aksjonspunktDefinisjon.getDefaultTotrinnBehandling();
    }

    void settToTrinnsFlag() {
        validerIkkeUtførtAvbruttAllerede();
        this.setToTrinnsBehandling(true);
    }

    void fjernToTrinnsFlagg() {
        validerIkkeUtførtAvbruttAllerede();
        this.setToTrinnsBehandling(false);
    }

    private void validerIkkeUtførtAvbruttAllerede() {
        if (erUtført() || erAvbrutt()) {
            // TODO (FC): håndteres av låsing allerede? Kaster exception nå for å se om GUI kan være ute av synk.
            throw new IllegalStateException("Forsøkte å bekrefte et allerede lukket aksjonspunkt:" + this); //$NON-NLS-1$
        }
    }

    private void validerIkkeAvbruttAllerede() {
        if (erAvbrutt()) {
            throw new IllegalStateException("Forsøkte å bekrefte et allerede lukket aksjonspunkt:" + this); //$NON-NLS-1$
        }
    }

    public Venteårsak getVenteårsak() {
        return venteårsak;
    }

    /** Variant av venteårsak - er opp til den som setter å håndtere (ikke noe predefinert kodeverk). */
    public String getVenteårsakVariant() {
        return venteårsakVariant;
    }

    void setVenteårsakVariant(String venteårsakVariant) {
        this.venteårsakVariant = venteårsakVariant;
    }

    void setVenteårsak(Venteårsak venteårsak) {
        this.venteårsak = venteårsak;
    }

    public String getAnsvarligSaksbehandler() {
        return ansvarligSaksbehandler;
    }

    public void setAnsvarligSaksbehandler(String ansvarligSaksbehandler) {
        this.ansvarligSaksbehandler = ansvarligSaksbehandler;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    /**
     * (optional)
     * Periode aksjonspunktet peker på.
     */
    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    /**
     * Intern Builder. Bruk Repository-klasser til å legge til og endre {@link Aksjonspunkt}.
     */
    static class Builder {
        private Aksjonspunkt opprinneligAp;
        private Aksjonspunkt aksjonspunkt;

        Builder(AksjonspunktDefinisjon aksjonspunktDefinisjon, BehandlingStegType behandlingStegFunnet) {
            this.aksjonspunkt = new Aksjonspunkt(aksjonspunktDefinisjon, behandlingStegFunnet);
        }

        Builder(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
            this.aksjonspunkt = new Aksjonspunkt(aksjonspunktDefinisjon);
        }

        Builder(Aksjonspunkt opprinneligAp) {
            this.opprinneligAp = opprinneligAp;
            this.aksjonspunkt = new Aksjonspunkt(opprinneligAp.getAksjonspunktDefinisjon());
        }

        /**
         * Angir om aksjonspunktet gjelder en spesifikk periode. Forutsetter at opplysningene aksjonspunktet er
         * opprettet for er periodisert.
         * <p>
         * NB: Skal kun brukes for aksjonspunkt som kan repteres flere multipliser for en behandling (eks. per periode i
         * utgangsvilkår).
         */
        Aksjonspunkt.Builder medPeriode(DatoIntervallEntitet status) {
            sjekkTilstand();
            this.aksjonspunkt.setPeriode(status);
            return this;
        }

        private void sjekkTilstand() {
            if (aksjonspunkt == null) {
                throw new IllegalStateException("Aksjonpunkt ikke definert"); //$NON-NLS-1$
            }
        }

        Aksjonspunkt buildFor(Behandling behandling) {
            Aksjonspunkt ap = this.aksjonspunkt;
            if (this.opprinneligAp != null) {
                if (behandling.erRevurdering()) {
                    kopierAlleFelter(opprinneligAp, ap, false);
                } else {
                    kopierAlleFelter(opprinneligAp, ap, true);
                }
            }
            Optional<Aksjonspunkt> eksisterende = finnEksisterende(behandling, ap.aksjonspunktDefinisjon);
            if (eksisterende.isPresent()) {
                // Oppdater eksisterende.
                Aksjonspunkt eksisterendeAksjonspunkt = eksisterende.get();
                kopierBasisfelter(ap, eksisterendeAksjonspunkt);
                return eksisterendeAksjonspunkt;
            } else {
                // Opprett ny og knytt til behandlingsresultat
                InternalUtil.leggTilAksjonspunkt(behandling, ap);
                return ap;
            }
        }

        private void kopierAlleFelter(Aksjonspunkt fra, Aksjonspunkt til, boolean medTotrinnsfelter) {
            kopierBasisfelter(fra, til);
            if (medTotrinnsfelter) {
                til.setToTrinnsBehandling(fra.isToTrinnsBehandling());
            }
            til.setBehandlingSteg(fra.getBehandlingStegFunnet());
        }

        private void kopierBasisfelter(Aksjonspunkt fra, Aksjonspunkt til) {
            til.setBegrunnelse(fra.getBegrunnelse());
            til.setPeriode(fra.getPeriode());
            til.setVenteårsak(fra.getVenteårsak());
            til.setVenteårsakVariant(fra.getVenteårsakVariant());
            til.setAnsvarligSaksbehandler(fra.getAnsvarligSaksbehandler());
            til.setFristTid(fra.getFristTid());
            til.setStatus(fra.getStatus(), fra.getBegrunnelse());
        }

        Aksjonspunkt.Builder medFristTid(LocalDateTime fristTid) {
            aksjonspunkt.setFristTid(fristTid);
            return this;
        }

        Aksjonspunkt.Builder medVenteårsak(Venteårsak venteårsak, String variant) {
            aksjonspunkt.setVenteårsak(venteårsak);
            aksjonspunkt.setVenteårsakVariant(variant);
            return this;
        }

        Aksjonspunkt.Builder medAnsvarligSaksbehandler(String ansvarligSaksbehandler) {
            aksjonspunkt.setAnsvarligSaksbehandler(ansvarligSaksbehandler);
            return this;
        }

        Aksjonspunkt.Builder medTotrinnskontroll(boolean toTrinnsbehandling) {
            aksjonspunkt.setToTrinnsBehandling(toTrinnsbehandling);
            return this;
        }
    }

    public boolean erUtført() {
        return Objects.equals(status, AksjonspunktStatus.UTFØRT);
    }

    public boolean erAvbrutt() {
        return Objects.equals(status, AksjonspunktStatus.AVBRUTT);
    }

    public boolean avbryt() {
        if(erÅpentAksjonspunkt()) {
            this.status = AksjonspunktStatus.AVBRUTT;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Aksjonspunkt{" +
            "id=" + id +
            ", aksjonspunktDefinisjon=" + getAksjonspunktDefinisjon() +
            ", status=" + status +
            ", behandlingStegFunnet=" + getBehandlingStegFunnet() +
            ", versjon=" + versjon +
            ", periode=" + periode +
            ", toTrinnsBehandling=" + isToTrinnsBehandling() +
            ", fristTid=" + getFristTid() +
            '}';
    }

    void setBehandlingSteg(BehandlingStegType stegType) {
        this.behandlingSteg = stegType;
    }

    private void setToTrinnsBehandling(boolean toTrinnsBehandling) {
        this.toTrinnsBehandling = toTrinnsBehandling;
    }

}
