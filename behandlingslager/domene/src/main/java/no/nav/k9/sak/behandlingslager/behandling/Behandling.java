package no.nav.k9.sak.behandlingslager.behandling;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Where;

import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.behandlingslager.kodeverk.BehandlingResultatKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.BehandlingStatusKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.BehandlingTypeKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.FagsystemKodeverkConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.StartpunktTypeKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.pip.PipBehandlingsData;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.feil.FeilFactory;

@SqlResultSetMappings(value = {
    @SqlResultSetMapping(name = "PipDataResult", classes = {
        @ConstructorResult(targetClass = PipBehandlingsData.class, columns = {
            @ColumnResult(name = "behandligStatus"),
            @ColumnResult(name = "ansvarligSaksbehandler"),
            @ColumnResult(name = "fagsakId"),
            @ColumnResult(name = "fagsakStatus")
        })
    })
})
@Entity(name = "Behandling")
@Table(name = "BEHANDLING")
@DynamicInsert
@DynamicUpdate
public class Behandling extends BaseEntitet {

    // Null safe
    private static final Comparator<BaseEntitet> COMPARATOR_OPPRETTET_TID = Comparator
        .comparing(BaseEntitet::getOpprettetTidspunkt, (a, b) -> {
            if (a != null && b != null) {
                return a.compareTo(b);
            } else if (a == null && b == null) {
                return 0;
            } else {
                return a == null ? -1 : 1;
            }
        });

    // Null safe
    private static final Comparator<BaseEntitet> COMPARATOR_ENDRET_TID = Comparator
        .comparing(BaseEntitet::getEndretTidspunkt, (a, b) -> {
            if (a != null && b != null) {
                return a.compareTo(b);
            } else if (a == null && b == null) {
                return 0;
            } else {
                return a == null ? -1 : 1;
            }
        });

    private static final Comparator<BaseEntitet> COMP_DESC_TID = COMPARATOR_OPPRETTET_TID.reversed()
        .thenComparing(Comparator.nullsLast(COMPARATOR_ENDRET_TID).reversed());

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING")
    private Long id;

    @NaturalId
    @Column(name = "uuid")
    private UUID uuid;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fagsak_id", nullable = false, updatable = false)
    private Fagsak fagsak;

    @Column(name = "original_behandling_id", updatable = false)
    private Long originalBehandlingId;

    @Convert(converter = BehandlingStatusKodeverdiConverter.class)
    @Column(name = "behandling_status", nullable = false)
    private BehandlingStatus status = BehandlingStatus.OPPRETTET;

    @OneToMany(cascade = { CascadeType.ALL }, orphanRemoval = true /* ok med orphanremoval siden behandlingårsaker er eid av denne */)
    @JoinColumn(name = "behandling_id", nullable = false)
    @OrderBy(value = "opprettetTidspunkt desc, endretTidspunkt desc nulls first")
    @Where(clause = "aktiv=true")
    private List<BehandlingStegTilstand> behandlingStegTilstander = new ArrayList<>(2);

    @Convert(converter = BehandlingTypeKodeverdiConverter.class)
    @Column(name = "behandling_type", nullable = false)
    private BehandlingType behandlingType = BehandlingType.UDEFINERT;

    // CascadeType.ALL + orphanRemoval=true må til for at aksjonspunkter skal bli slettet fra databasen ved fjerning fra HashSet
    @OneToMany(cascade = { CascadeType.ALL }, orphanRemoval = true)
    @JoinColumn(name = "behandling_id", nullable = false)
    private Set<Aksjonspunkt> aksjonspunkter = new HashSet<>(2);

    @OneToMany(cascade = { CascadeType.ALL }, orphanRemoval = true /* ok med orphanremoval siden behandlingårsaker er eid av denne */)
    @JoinColumn(name = "behandling_id", nullable = false)
    @BatchSize(size = 20)
    private Set<BehandlingÅrsak> behandlingÅrsaker = new HashSet<>(2);

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Convert(converter = StartpunktTypeKodeverdiConverter.class)
    @Column(name = "startpunkt_type", nullable = false)
    private StartpunktType startpunkt = StartpunktType.UDEFINERT;

    /**
     * --------------------------------------------------------------
     * FIXME: Produksjonstyringsinformasjon bør flyttes ut av Behandling klassen.
     * Gjelder feltene under
     * --------------------------------------------------------------
     */
    @Column(name = "opprettet_dato", nullable = false, updatable = false)
    private LocalDateTime opprettetDato;

    @Column(name = "avsluttet_dato")
    private LocalDateTime avsluttetDato;

    @Column(name = "totrinnsbehandling", nullable = false)

    private boolean toTrinnsBehandling = false;

    @Column(name = "ansvarlig_saksbehandler")
    private String ansvarligSaksbehandler;

    @Column(name = "ansvarlig_beslutter")
    private String ansvarligBeslutter;

    @Column(name = "behandlende_enhet")
    private String behandlendeEnhet;

    @Column(name = "behandlende_enhet_navn")
    private String behandlendeEnhetNavn;

    @Column(name = "behandlende_enhet_arsak")
    private String behandlendeEnhetÅrsak;

    @Column(name = "behandlingstid_frist", nullable = false)
    private LocalDate behandlingstidFrist;

    @Convert(converter = BehandlingResultatKodeverdiConverter.class)
    @Column(name = "behandling_resultat_type", nullable = false)
    private BehandlingResultatType behandlingResultatType = BehandlingResultatType.IKKE_FASTSATT;

    @Column(name = "aapnet_for_endring", nullable = false)
    private boolean åpnetForEndring = false;

    @ChangeTracked
    @Convert(converter = FagsystemKodeverkConverter.class)
    @Column(name = "migrert_kilde", nullable = false)
    private Fagsystem migrertKilde = Fagsystem.UDEFINERT;

    Behandling() {
        // Hibernate
    }

    private Behandling(Fagsak fagsak, BehandlingType type) {
        Objects.requireNonNull(fagsak, "Behandling må tilknyttes parent Fagsak"); //$NON-NLS-1$
        this.fagsak = fagsak;
        if (type != null) {
            this.behandlingType = type;
        }

        // generer ny behandling uuid
        this.uuid = UUID.randomUUID();
        setOpprettetTidspunkt(LocalDateTime.now());
    }

    /**
     * Skal kun brukes av BehandlingskontrollTjeneste for prod kode slik at events fyres.
     * <p>
     * Denne oppretter en Builder for å bygge en {@link Behandling}.
     *
     * <h4>NB! BRUKES VED FØRSTE FØRSTEGANGSBEHANDLING</h4>
     * <h4>NB2! FOR TESTER - FORTREKK (TestScenarioBuilder) eller (TestScenarioBuilder). De
     * forenkler
     * test oppsett</h4>
     * <p>
     * Ved senere behandlinger på samme Fagsak, bruk {@link #fraTidligereBehandling(Behandling, BehandlingType)}.
     */
    public static Behandling.Builder forFørstegangssøknad(Fagsak fagsak) {
        return nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD);
    }

    /**
     * Skal kun brukes av BehandlingskontrollTjeneste for prod kode slik at events fyres.
     *
     * @see #forFørstegangssøknad(Fagsak)
     */
    public static Builder nyBehandlingFor(Fagsak fagsak, BehandlingType behandlingType) {
        return new Builder(fagsak, behandlingType);
    }

    /**
     * Skal kun brukes av BehandlingskontrollTjeneste for prod kode slik at events fyres.
     * <p>
     * Denne oppretter en Builder for å bygge en {@link Behandling} basert på et eksisterende behandling.
     * <p>
     * Ved Endringssøknad eller REVURD_OPPR er det normalt DENNE som skal brukes.
     * <p>
     * NB! FOR TESTER - FORTREKK (TestScenarioBuilder) eller (TestScenarioBuilder). De forenkler
     * test oppsett basert på vanlige defaults.
     */
    public static Behandling.Builder fraTidligereBehandling(Behandling forrigeBehandling, BehandlingType behandlingType) {
        return new Builder(forrigeBehandling, behandlingType);
    }

    public List<BehandlingÅrsak> getBehandlingÅrsaker() {
        return new ArrayList<>(behandlingÅrsaker);
    }

    void leggTilBehandlingÅrsaker(List<BehandlingÅrsak> behandlingÅrsaker) {
        if (erAvsluttet() && erHenlagt()) {
            throw new IllegalStateException("Utvikler-feil: kan ikke legge til årsaker på en behandling som er avsluttet.");
        }
        behandlingÅrsaker.forEach(bå -> {
            this.behandlingÅrsaker.add(bå);
        });
    }

    public boolean harBehandlingÅrsak(BehandlingÅrsakType behandlingÅrsak) {
        return getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .anyMatch(behandlingÅrsak::equals);
    }

    public Optional<Long> getOriginalBehandlingId() {
        return Optional.ofNullable(originalBehandlingId);
    }

    public boolean erManueltOpprettet() {
        return getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::erManueltOpprettet)
            .collect(Collectors.toList())
            .contains(true);
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Long getFagsakId() {
        return getFagsak().getId();
    }

    public AktørId getAktørId() {
        return getFagsak().getAktørId();
    }

    public BehandlingStatus getStatus() {
        return status;
    }

    /**
     * Oppdater behandlingssteg og tilhørende status.
     * <p>
     * NB::NB::NB Dette skal normalt kun gjøres fra Behandlingskontroll slik at bokføring og events blir riktig.
     * Er ikke en del av offentlig API.
     *
     * @param stegTilstand - tilstand for steg behandlingen er i
     */
    void oppdaterBehandlingStegOgStatus(BehandlingStegTilstand stegTilstand) {
        Objects.requireNonNull(stegTilstand, "behandlingStegTilstand"); //$NON-NLS-1$

        getSisteBehandlingStegTilstand().ifPresent(BehandlingStegTilstand::deaktiver);

        // legg til ny
        this.behandlingStegTilstander.add(stegTilstand);
        BehandlingStegType behandlingSteg = stegTilstand.getBehandlingSteg();
        this.status = behandlingSteg.getDefinertBehandlingStatus();
    }

    /**
     * Marker behandling som avsluttet.
     */
    public void avsluttBehandling() {
        getAksjonspunkterStream().filter(a -> a.erÅpentAksjonspunkt()).forEach(a -> a.avbryt());

        lukkBehandlingStegStatuser(this.behandlingStegTilstander, BehandlingStegStatus.UTFØRT);
        this.status = BehandlingStatus.AVSLUTTET;
        this.avsluttetDato = LocalDateTime.now();
    }

    private void lukkBehandlingStegStatuser(Collection<BehandlingStegTilstand> stegTilstander, BehandlingStegStatus sluttStatusForSteg) {
        stegTilstander.stream()
            .filter(t -> !BehandlingStegStatus.erSluttStatus(t.getBehandlingStegStatus()))
            .forEach(t -> t.setBehandlingStegStatus(sluttStatusForSteg));
    }

    public BehandlingType getType() {
        return behandlingType;
    }

    public LocalDateTime getOpprettetDato() {
        return opprettetDato;
    }

    public LocalDateTime getAvsluttetDato() {
        return avsluttetDato;
    }

    public LocalDate getBehandlingstidFrist() {
        return behandlingstidFrist;
    }

    public void setBehandlingstidFrist(LocalDate behandlingstidFrist) {
        guardTilstandPåBehandling();
        this.behandlingstidFrist = behandlingstidFrist;
    }

    public Optional<BehandlingStegTilstand> getBehandlingStegTilstand() {
        List<BehandlingStegTilstand> tilstander = behandlingStegTilstander.stream()
            .filter(t -> !BehandlingStegStatus.erSluttStatus(t.getBehandlingStegStatus()))
            .collect(Collectors.toList());
        if (tilstander.size() > 1) {
            throw new IllegalStateException("Utvikler-feil: Kan ikke ha flere steg samtidig åpne: " + tilstander); //$NON-NLS-1$
        }

        return tilstander.isEmpty() ? Optional.empty() : Optional.of(tilstander.get(0));
    }

    public Optional<BehandlingStegTilstand> getSisteBehandlingStegTilstand() {
        // sjekk "ikke-sluttstatuser" først
        Optional<BehandlingStegTilstand> sisteAktive = getBehandlingStegTilstand();

        if (sisteAktive.isPresent()) {
            return sisteAktive;
        }

        // tar nyeste.
        return behandlingStegTilstander.stream().sorted(COMP_DESC_TID).findFirst();
    }

    public Optional<BehandlingStegTilstand> getBehandlingStegTilstand(BehandlingStegType stegType) {
        List<BehandlingStegTilstand> tilstander = behandlingStegTilstander.stream()
            .filter(t -> !BehandlingStegStatus.erSluttStatus(t.getBehandlingStegStatus())
                && Objects.equals(stegType, t.getBehandlingSteg()))
            .collect(Collectors.toList());
        if (tilstander.size() > 1) {
            throw new IllegalStateException(
                "Utvikler-feil: Kan ikke ha flere steg samtidig åpne for stegType[" + stegType + "]: " + tilstander); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return tilstander.isEmpty() ? Optional.empty() : Optional.of(tilstander.get(0));
    }

    /**
     * @deprecated bygg fortrinnsvis logikk rundt eksistens av stegresultater (fx vedtaksdato). Slik at man evt kan dekoble tabeller (evt behold
     * en current her)
     */
    @Deprecated
    public Stream<BehandlingStegTilstand> getBehandlingStegTilstandHistorikk() {
        return behandlingStegTilstander.stream().sorted(COMPARATOR_OPPRETTET_TID);
    }

    public BehandlingStegType getAktivtBehandlingSteg() {
        BehandlingStegTilstand stegTilstand = getBehandlingStegTilstand().orElse(null);
        return stegTilstand == null ? null : stegTilstand.getBehandlingSteg();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Behandling)) {
            return false;
        }
        Behandling other = (Behandling) object;
        return Objects.equals(getFagsak(), other.getFagsak())
            && Objects.equals(getType(), other.getType())
            && Objects.equals(getOpprettetTidspunkt(), other.getOpprettetTidspunkt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFagsak(), getType(), getOpprettetTidspunkt());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" //$NON-NLS-1$
            + (id != null ? "id=" + id + ", " : "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "fagsak=" + fagsak + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "status=" + status + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "type=" + behandlingType + "," //$NON-NLS-1$ //$NON-NLS-2$
            + "steg=" + (getBehandlingStegTilstand().orElse(null)) + "," //$NON-NLS-1$ //$NON-NLS-2$
            + "opprettetTs=" + getOpprettetTidspunkt() //$NON-NLS-1$
            + ">"; //$NON-NLS-1$
    }

    public String getBehandlendeEnhetÅrsak() {
        return behandlendeEnhetÅrsak;
    }

    public void setBehandlendeEnhetÅrsak(String behandlendeEnhetÅrsak) {
        guardTilstandPåBehandling();
        this.behandlendeEnhetÅrsak = behandlendeEnhetÅrsak;
    }

    public String getBehandlendeEnhet() {
        return behandlendeEnhet;
    }

    public void setBehandlendeEnhet(OrganisasjonsEnhet enhet) {
        guardTilstandPåBehandling();
        this.behandlendeEnhet = enhet.getEnhetId();
        this.behandlendeEnhetNavn = enhet.getEnhetNavn();
    }

    public OrganisasjonsEnhet getBehandlendeOrganisasjonsEnhet() {
        return new OrganisasjonsEnhet(behandlendeEnhet, behandlendeEnhetNavn);
    }

    public Fagsak getFagsak() {
        return fagsak;
    }

    public Set<Aksjonspunkt> getAksjonspunkter() {
        return Collections.unmodifiableSet(aksjonspunkter);
    }

    public Optional<Aksjonspunkt> getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon definisjon) {
        return getAksjonspunkterStream()
            .filter(a -> a.getAksjonspunktDefinisjon().equals(definisjon))
            .findFirst();
    }

    public Optional<Aksjonspunkt> getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon definisjon) {
        return getÅpneAksjonspunkterStream()
            .filter(a -> a.getAksjonspunktDefinisjon().equals(definisjon))
            .findFirst();
    }

    public Aksjonspunkt getAksjonspunktFor(AksjonspunktDefinisjon definisjon) {
        return getAksjonspunkterStream()
            .filter(a -> a.getAksjonspunktDefinisjon().equals(definisjon))
            .findFirst()
            .orElseThrow(() -> FeilFactory.create(BehandlingFeil.class).aksjonspunktIkkeFunnet(definisjon.getKode()).toException());
    }

    public Optional<Aksjonspunkt> getAksjonspunktFor(String aksjonspunktDefinisjonKode) {
        return getAksjonspunkterStream()
            .filter(a -> a.getAksjonspunktDefinisjon().getKode().equals(aksjonspunktDefinisjonKode))
            .findFirst();
    }

    public List<Aksjonspunkt> getÅpneAksjonspunkter() {
        return getÅpneAksjonspunkterStream()
            .collect(Collectors.toList());
    }

    public List<Aksjonspunkt> getBehandledeAksjonspunkter() {
        return getAksjonspunkterStream()
            .filter(Aksjonspunkt::erUtført)
            .collect(Collectors.toList());
    }

    public List<Aksjonspunkt> getÅpneAksjonspunkter(AksjonspunktType aksjonspunktType) {
        return getÅpneAksjonspunkterStream()
            .filter(ad -> Objects.equals(aksjonspunktType, ad.getAksjonspunktDefinisjon().getAksjonspunktType()))
            .collect(Collectors.toList());
    }

    public List<Aksjonspunkt> getÅpneAksjonspunkter(Collection<AksjonspunktDefinisjon> matchKriterier) {
        return getÅpneAksjonspunkterStream()
            .filter(a -> matchKriterier.contains(a.getAksjonspunktDefinisjon()))
            .collect(Collectors.toList());
    }

    /**
     * Internt API, IKKE BRUK.
     */
    void addAksjonspunkt(Aksjonspunkt aksjonspunkt) {
        validerKanLeggeTilAksjonspunkt(aksjonspunkt.getStatus(), aksjonspunkt.getAksjonspunktDefinisjon());
        aksjonspunkter.add(aksjonspunkt);
    }

    private void validerKanLeggeTilAksjonspunkt(AksjonspunktStatus aksjonspunktStatus, AksjonspunktDefinisjon def) {
        if (!def.validerGyldigStatusEndring(aksjonspunktStatus, getStatus())) {
            throw new IllegalArgumentException("Kan ikke legge til aksjonspunkt: " + def + " i gjelden status for behandling:" + this);
        }
    }

    public List<Aksjonspunkt> getAksjonspunkterMedTotrinnskontroll() {
        return getAksjonspunkterStream()
            .filter(a -> !a.erAvbrutt() && a.isToTrinnsBehandling())
            .collect(Collectors.toList());
    }

    public boolean harAksjonspunktMedType(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return getAksjonspunkterStream()
            .anyMatch(ap -> aksjonspunktDefinisjon.equals(ap.getAksjonspunktDefinisjon()));
    }

    public boolean harÅpentAksjonspunktMedType(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return getÅpneAksjonspunkterStream().map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .anyMatch(aksjonspunktDefinisjon::equals);
    }

    public boolean harAksjonspunktMedTotrinnskontroll() {
        return getAksjonspunkterStream()
            .anyMatch(a -> !a.erAvbrutt() && a.isToTrinnsBehandling());
    }

    private Optional<Aksjonspunkt> getFørsteÅpneAutopunkt() {
        return getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT).stream()
            .findFirst();
    }

    public boolean isBehandlingPåVent() {
        return !getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT).isEmpty();
    }

    private Stream<Aksjonspunkt> getAksjonspunkterStream() {
        return aksjonspunkter.stream();
    }

    private Stream<Aksjonspunkt> getÅpneAksjonspunkterStream() {
        return getAksjonspunkterStream()
            .filter(Aksjonspunkt::erÅpentAksjonspunkt);
    }

    public Long getVersjon() {
        return versjon;
    }

    public BehandlingStegStatus getBehandlingStegStatus() {
        BehandlingStegTilstand stegTilstand = getBehandlingStegTilstand().orElse(null);
        return stegTilstand == null ? null : stegTilstand.getBehandlingStegStatus();
    }

    public boolean isToTrinnsBehandling() {
        return toTrinnsBehandling;
    }

    public void setToTrinnsBehandling() {
        guardTilstandPåBehandling();
        this.toTrinnsBehandling = true;
    }

    public void nullstillToTrinnsBehandling() {
        guardTilstandPåBehandling();
        this.toTrinnsBehandling = false;
    }

    public String getAnsvarligSaksbehandler() {
        return ansvarligSaksbehandler;
    }

    public void setAnsvarligSaksbehandler(String ansvarligSaksbehandler) {
        guardTilstandPåBehandling();
        this.ansvarligSaksbehandler = ansvarligSaksbehandler;
    }

    public String getAnsvarligBeslutter() {
        return ansvarligBeslutter;
    }

    public void setAnsvarligBeslutter(String ansvarligBeslutter) {
        guardTilstandPåBehandling();
        this.ansvarligBeslutter = ansvarligBeslutter;
    }

    public boolean isBehandlingHenlagt() {
        return erHenlagt();
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return getFagsak().getYtelseType();
    }

    public LocalDate getFristDatoBehandlingPåVent() {
        Optional<Aksjonspunkt> aksjonspunkt = getFørsteÅpneAutopunkt();
        LocalDateTime fristTid = null;
        if (aksjonspunkt.isPresent()) {
            fristTid = aksjonspunkt.get().getFristTid();
        }
        return fristTid == null ? null : fristTid.toLocalDate();
    }

    public AksjonspunktDefinisjon getBehandlingPåVentAksjonspunktDefinisjon() {
        Optional<Aksjonspunkt> aksjonspunkt = getFørsteÅpneAutopunkt();
        if (aksjonspunkt.isPresent()) {
            return aksjonspunkt.get().getAksjonspunktDefinisjon();
        }
        return null;
    }

    public Venteårsak getVenteårsak() {
        Optional<Aksjonspunkt> aksjonspunkt = getFørsteÅpneAutopunkt();
        if (aksjonspunkt.isPresent()) {
            return aksjonspunkt.get().getVenteårsak();
        }
        return null;
    }

    /**
     * @deprecated - fjernes når alle behandlinger har UUID og denne er satt NOT NULL i db. Inntil da sikrer denne lagring av UUID
     */
    @Deprecated
    @PreUpdate
    protected void onUpdateMigrerUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    public boolean erSaksbehandlingAvsluttet() {
        return (erStatusFerdigbehandlet() || erHenlagt());
    }

    public boolean erHenlagt() {
        return getBehandlingResultatType().isBehandlingHenlagt();
    }

    public boolean erUnderIverksettelse() {
        return Objects.equals(BehandlingStatus.IVERKSETTER_VEDTAK, getStatus());
    }

    public boolean erAvsluttet() {
        return Objects.equals(BehandlingStatus.AVSLUTTET, getStatus());
    }

    public boolean erStatusFerdigbehandlet() {
        return getStatus().erFerdigbehandletStatus();
    }

    public boolean erRevurdering() {
        return BehandlingType.REVURDERING.equals(getType());
    }

    public boolean erYtelseBehandling() {
        return getType().erYtelseBehandlingType();
    }

    public boolean harSattStartpunkt() {
        return !StartpunktType.UDEFINERT.equals(startpunkt);
    }

    public StartpunktType getStartpunkt() {
        return startpunkt;
    }

    public void setStartpunkt(StartpunktType startpunkt) {
        guardTilstandPåBehandling();
        this.startpunkt = startpunkt;
    }

    public boolean erÅpnetForEndring() {
        return åpnetForEndring;
    }

    public void setÅpnetForEndring(boolean åpnetForEndring) {
        guardTilstandPåBehandling();
        this.åpnetForEndring = åpnetForEndring;
    }

    public Fagsystem getMigrertKilde() {
        return migrertKilde;
    }

    public void setMigrertKilde(Fagsystem migrertKilde) {
        guardTilstandPåBehandling();
        this.migrertKilde = migrertKilde;
    }

    private void guardTilstandPåBehandling() {
        if (erSaksbehandlingAvsluttet()) {
            throw new IllegalStateException("Utvikler-feil: kan ikke endre tilstand på en behandling som er avsluttet.");
        }
    }

    @PreRemove
    protected void onDelete() {
        // FIXME: FPFEIL-2799 (FrodeC): Fjern denne når FPFEIL-2799 er godkjent
        throw new IllegalStateException("Skal aldri kunne slette behandling. [id=" + id + ", status=" + getStatus() + ", type=" + getType() + "]");
    }

    public BehandlingResultatType getBehandlingResultatType() {
        return behandlingResultatType;
    }

    public void setBehandlingResultatType(BehandlingResultatType behandlingResultatType) {
        guardTilstandPåBehandling();
        this.behandlingResultatType = behandlingResultatType;
    }

    public List<BehandlingÅrsakType> getBehandlingÅrsakerTyper() {
        return getBehandlingÅrsaker().stream().map(BehandlingÅrsak::getBehandlingÅrsakType).collect(Collectors.toList());
    }

    public static class Builder {

        private final BehandlingType behandlingType;
        private Fagsak fagsak;
        private Behandling forrigeBehandling;

        private LocalDateTime opprettetDato;
        private LocalDateTime avsluttetDato;

        private String behandlendeEnhet;
        private String behandlendeEnhetNavn;
        private String behandlendeEnhetÅrsak;

        private LocalDate behandlingstidFrist = LocalDate.now().plusWeeks(6);

        private BehandlingÅrsak.Builder behandlingÅrsakBuilder;
        private BehandlingResultatType behandlingResultatType;
        private BehandlingStatus status;

        private Builder(Fagsak fagsak, BehandlingType behandlingType) {
            this(behandlingType);
            Objects.requireNonNull(fagsak, "fagsak"); //$NON-NLS-1$
            this.fagsak = fagsak;
        }

        private Builder(Behandling forrigeBehandling, BehandlingType behandlingType) {
            this(behandlingType);
            this.forrigeBehandling = forrigeBehandling;
        }

        private Builder(BehandlingType behandlingType) {
            Objects.requireNonNull(behandlingType, "behandlingType"); //$NON-NLS-1$
            this.behandlingType = behandlingType;
        }

        public Builder medBehandlingÅrsak(BehandlingÅrsak.Builder årsakBuilder) {
            this.behandlingÅrsakBuilder = årsakBuilder;
            return this;
        }

        /**
         * Fix opprettet dato.
         */
        public Builder medOpprettetDato(LocalDateTime tid) {
            this.opprettetDato = tid == null ? null : tid.withNano(0);
            return this;
        }

        public Builder medBehandlingStatus(BehandlingStatus status) {
            this.status = status;
            return this;
        }

        public Builder medBehandlingResultatType(BehandlingResultatType behandlingResultatType) {
            this.behandlingResultatType = Objects.requireNonNull(behandlingResultatType, "behandlingResultatType");
            return this;
        }

        /**
         * Fix avsluttet dato.
         */
        public Builder medAvsluttetDato(LocalDateTime tid) {
            this.avsluttetDato = tid == null ? null : tid.withNano(0);
            return this;
        }

        public Builder medBehandlendeEnhet(OrganisasjonsEnhet enhet) {
            this.behandlendeEnhet = enhet.getEnhetId();
            this.behandlendeEnhetNavn = enhet.getEnhetNavn();
            return this;
        }

        public Builder medBehandlendeEnhetÅrsak(String behandlendeEnhetÅrsak) {
            this.behandlendeEnhetÅrsak = behandlendeEnhetÅrsak;
            return this;
        }

        public Builder medBehandlingstidFrist(LocalDate frist) {
            this.behandlingstidFrist = frist;
            return this;
        }

        /**
         * Bygger en Behandling.
         * <p>
         * Husk: Har du brukt riktig Factory metode for å lage en Builder? :
         * <ul>
         * <li>{@link Behandling#fraTidligereBehandling(Behandling, BehandlingType)} (&lt;- BRUK DENNE HVIS DET ER
         * TIDLIGERE BEHANDLINGER PÅ SAMME FAGSAK)</li>
         * <li>{@link Behandling#forFørstegangssøknad(Fagsak)}</li>
         * </ul>
         */
        public Behandling build() {
            Behandling behandling;

            if (forrigeBehandling != null) {
                behandling = new Behandling(forrigeBehandling.getFagsak(), behandlingType);
                behandling.originalBehandlingId = forrigeBehandling.getId();
                behandling.behandlendeEnhet = forrigeBehandling.behandlendeEnhet;
                behandling.behandlendeEnhetNavn = forrigeBehandling.behandlendeEnhetNavn;
                behandling.behandlendeEnhetÅrsak = forrigeBehandling.behandlendeEnhetÅrsak;
                if (behandlingstidFrist != null) {
                    behandling.behandlingstidFrist = behandlingstidFrist;
                } else {
                    behandling.behandlingstidFrist = forrigeBehandling.behandlingstidFrist;
                }
            } else {
                behandling = new Behandling(fagsak, behandlingType);
                behandling.behandlendeEnhet = behandlendeEnhet;
                behandling.behandlendeEnhetNavn = behandlendeEnhetNavn;
                behandling.behandlendeEnhetÅrsak = behandlendeEnhetÅrsak;
                behandling.behandlingstidFrist = behandlingstidFrist;
            }

            if (this.behandlingResultatType != null) {
                behandling.setBehandlingResultatType(this.behandlingResultatType);
            }
            if (this.opprettetDato != null) {
                behandling.opprettetDato = this.opprettetDato.withNano(0);
            } else {
                behandling.opprettetDato = behandling.getOpprettetTidspunkt().withNano(0);
            }
            if (this.avsluttetDato != null) {
                behandling.avsluttetDato = this.avsluttetDato.withNano(0);
            }

            if (this.behandlingÅrsakBuilder != null) {
                this.behandlingÅrsakBuilder.buildFor(behandling);
            }
            if (this.status != null) {
                behandling.status = this.status;
            }

            return behandling;
        }

    }

}
