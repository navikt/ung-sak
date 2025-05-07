package no.nav.ung.kodeverk.behandling.aksjonspunkt;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import no.nav.ung.kodeverk.TempAvledeKode;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.vilkår.VilkårType;

import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.*;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.Ventekategori.*;

/**
 * Definerer mulige Aksjonspunkter inkludert hvilket Vurderingspunkt de må løses i.
 * Inkluderer også konstanter for å enklere kunne referere til dem i eksisterende logikk.
 */
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum AksjonspunktDefinisjon implements Kodeverdi {

    // Gruppe : 5xxx
    @Deprecated(forRemoval = true)
    AVKLAR_TILLEGGSOPPLYSNINGER(
        AksjonspunktKodeDefinisjon.AVKLAR_TILLEGGSOPPLYSNINGER_KODE, AksjonspunktType.MANUELL, "Avklar tilleggsopplysninger",
        BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_FAKTA, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),

    FORESLÅ_VEDTAK(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_KODE,
        AksjonspunktType.MANUELL, "Foreslå vedtak", BehandlingStatus.UTREDES, BehandlingStegType.FORESLÅ_VEDTAK,
        UTEN_VILKÅR, SkjermlenkeType.VEDTAK, ENTRINN, AVVENTER_SAKSBEHANDLER),

    FATTER_VEDTAK(AksjonspunktKodeDefinisjon.FATTER_VEDTAK_KODE,
        AksjonspunktType.MANUELL, "Fatter vedtak",
        Set.of(BehandlingStatus.FATTER_VEDTAK, BehandlingStatus.UTREDES), BehandlingStegType.FATTE_VEDTAK,
        UTEN_VILKÅR,
        SkjermlenkeType.VEDTAK,
        ENTRINN, TILBAKE, AVBRYTES, AVVENTER_SAKSBEHANDLER),

    SØKERS_OPPLYSNINGSPLIKT_MANU(
        AksjonspunktKodeDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU_KODE, AksjonspunktType.MANUELL,
        "Vurder søkers opplysningsplikt ved ufullstendig/ikke-komplett søknad", BehandlingStatus.UTREDES,
        BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT, VilkårType.SØKERSOPPLYSNINGSPLIKT, SkjermlenkeType.OPPLYSNINGSPLIKT, ENTRINN, AVVENTER_SAKSBEHANDLER),
    VEDTAK_UTEN_TOTRINNSKONTROLL(
        AksjonspunktKodeDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL_KODE, AksjonspunktType.MANUELL, "Foreslå vedtak uten totrinnskontroll",
        BehandlingStatus.UTREDES, BehandlingStegType.FORESLÅ_VEDTAK, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),

    VARSEL_REVURDERING_MANUELL(
        AksjonspunktKodeDefinisjon.VARSEL_REVURDERING_MANUELL_KODE, AksjonspunktType.MANUELL, "Varsel om revurdering opprettet manuelt",
        BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_UNGDOMSPROGRAM, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    FORESLÅ_VEDTAK_MANUELT(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_MANUELT_KODE,
        AksjonspunktType.MANUELL, "Foreslå vedtak manuelt", BehandlingStatus.UTREDES, BehandlingStegType.FORESLÅ_VEDTAK, UTEN_VILKÅR,
        SkjermlenkeType.VEDTAK, ENTRINN, AVVENTER_SAKSBEHANDLER),


    VURDERE_ANNEN_YTELSE_FØR_VEDTAK(
        AksjonspunktKodeDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK_KODE, AksjonspunktType.MANUELL, "Vurdere annen ytelse før vedtak",
        BehandlingStatus.UTREDES, BehandlingStegType.FORESLÅ_VEDTAK, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    VURDERE_DOKUMENT_FØR_VEDTAK(
        AksjonspunktKodeDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK_KODE, AksjonspunktType.MANUELL, "Vurdere dokument før vedtak",
        BehandlingStatus.UTREDES,
        BehandlingStegType.FORESLÅ_VEDTAK, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    VURDERE_OVERLAPPENDE_YTELSER_FØR_VEDTAK(
        AksjonspunktKodeDefinisjon.VURDERE_OVERLAPPENDE_YTELSER_FØR_VEDTAK_KODE, AksjonspunktType.MANUELL, "Vurdere overlappende ytelse før vedtak",
        BehandlingStatus.UTREDES,
        BehandlingStegType.FORESLÅ_VEDTAK, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),


    KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST(
        AksjonspunktKodeDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST_KODE, AksjonspunktType.MANUELL,
        "Vurder varsel ved vedtak til ugunst",
        BehandlingStatus.UTREDES, BehandlingStegType.FORESLÅ_VEDTAK, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING(
        AksjonspunktKodeDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING_KODE, AksjonspunktType.MANUELL,
        "Kontroll av manuelt opprettet revurderingsbehandling", Set.of(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES), BehandlingStegType.FORESLÅ_VEDTAK,
        UTEN_VILKÅR, UTEN_SKJERMLENKE,
        ENTRINN, AVVENTER_SAKSBEHANDLER),


    VURDER_FEILUTBETALING(AksjonspunktKodeDefinisjon.VURDER_FEILUTBETALING_KODE,
        AksjonspunktType.MANUELL, "Vurder feilutbetaling", BehandlingStatus.UTREDES, BehandlingStegType.SIMULER_OPPDRAG, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    SJEKK_TILBAKEKREVING(AksjonspunktKodeDefinisjon.SJEKK_TILBAKEKREVING_KODE,
        AksjonspunktType.MANUELL, "Sjekk om ytelsesbehandlingen skal utføres før eller etter tilbakekrevingsbehandlingen", BehandlingStatus.UTREDES, BehandlingStegType.FORESLÅ_VEDTAK, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    VURDER_TILBAKETREKK(AksjonspunktKodeDefinisjon.VURDER_TILBAKETREKK_KODE,
        AksjonspunktType.MANUELL, "Vurder tilbaketrekk", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_TILBAKETREKK,
        UTEN_VILKÅR, SkjermlenkeType.TILKJENT_YTELSE, TOTRINN, AVVENTER_SAKSBEHANDLER),
    KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST(AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST_KODE,
        AksjonspunktType.MANUELL, "Vurder søknadsfrist", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_SØKNADSFRIST,
        VilkårType.SØKNADSFRIST, SkjermlenkeType.SOEKNADSFRIST, TOTRINN, TILBAKE, null, AVVENTER_SAKSBEHANDLER),

    // Gruppe : 60xx
    OVERSTYRING_AV_SØKNADSFRISTVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_SØKNADSFRISTVILKÅRET_KODE,
        AksjonspunktType.SAKSBEHANDLEROVERSTYRING, "Overstyring av Søknadsfrist",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_SØKNADSFRIST, VilkårType.SØKNADSFRIST,
        SkjermlenkeType.SOEKNADSFRIST, TOTRINN, AVVENTER_SAKSBEHANDLER),
    OVERSTYRING_AV_INNTEKT(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_INNTEKT_KODE,
        AksjonspunktType.OVERSTYRING, "Overstyring av intekt", BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT,
        UTEN_VILKÅR, UTEN_SKJERMLENKE, TOTRINN, AVVENTER_SAKSBEHANDLER),

    // Gruppe : 70xx

    AUTO_MANUELT_SATT_PÅ_VENT(AksjonspunktKodeDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT_KODE, AksjonspunktType.AUTOPUNKT,
        "Manuelt satt på vent", BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_FAKTA, UTEN_VILKÅR, UTEN_SKJERMLENKE,
        ENTRINN, FORBLI, "P4W", AVVENTER_ANNET),
    AUTO_VENTER_PÅ_KOMPLETT_SØKNAD(AksjonspunktKodeDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD_KODE, AksjonspunktType.AUTOPUNKT,
        "Venter på komplett søknad", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_KOMPLETTHET, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, FORBLI, "P4W", AVVENTER_SØKER),

    AUTO_SATT_PÅ_VENT_REVURDERING(AksjonspunktKodeDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING_KODE, AksjonspunktType.AUTOPUNKT,
        "Satt på vent etter varsel om revurdering", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_KOMPLETTHET, UTEN_VILKÅR,
        UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P2W", AVVENTER_SØKER),

    AUTO_SATT_PÅ_VENT_RAPPORTERINGSFRIST(AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST_KODE, AksjonspunktType.AUTOPUNKT,
        "Satt på vent etter kontroll av inntekt til rapporteringsfrist har passert", BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT, UTEN_VILKÅR,
        UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P2W", AVVENTER_ARBEIDSGIVER),


    AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE(AksjonspunktKodeDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKT_UTTALELSE_KODE, AksjonspunktType.AUTOPUNKT,
        "Satt på vent etter kontroll av inntekt til rapporteringsfrist har passert", BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT, UTEN_VILKÅR,
        UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P2W", AVVENTER_SØKER),

    // Gruppe: 80xx
    KONTROLLER_INNTEKT(
        AksjonspunktKodeDefinisjon.KONTROLLER_INNTEKT_KODE, AksjonspunktType.MANUELL, "Kontroller inntekt",
        BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT,
        UTEN_VILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, AVVENTER_SAKSBEHANDLER),

    // Gruppe : 999x
    AUTO_VENT_FILTER_MANGLENDE_FUNKSJONALITET(AksjonspunktKodeDefinisjon.AUTO_VENT_FILTER_MANGLENDE_FUNKSJONALITET, AksjonspunktType.AUTOPUNKT, "Venter på manglende funksjonalitet.",
        BehandlingStatus.UTREDES, BehandlingStegType.VARIANT_FILTER, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P26W", AVVENTER_TEKNISK_FEIL),

    UNDEFINED,

    ;

    static final String KODEVERK = "AKSJONSPUNKT_DEF";

    /**
     * Liste av utgåtte aksjonspunkt. Ikke gjenbruk samme kode.
     */
    private static final Map<String, String> UTGÅTT = Map.of(
        "5022", "AVKLAR_FAKTA_FOR_PERSONSTATUS",
        "7007", "VENT_PÅ_SCANNING");

    private static final Map<String, AksjonspunktDefinisjon> KODER = new LinkedHashMap<>();


    static {
        for (var v : UTGÅTT.keySet()) {
            if (KODER.putIfAbsent(v, UNDEFINED) != null) {
                throw new IllegalArgumentException("Duplikat : " + v);
            }
        }
        // valider ingen unmapped koder
        var sjekkKodeBrukMap = new TreeMap<>(AksjonspunktKodeDefinisjon.KODER);

        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode + ", mulig utgått?");
            }
            if (v.kode != null) {
                sjekkKodeBrukMap.remove(v.kode);
            }
        }

        if (!sjekkKodeBrukMap.isEmpty()) {
            System.out.printf("Ubrukt sjekk: Har koder definert i %s som ikke er i bruk i %s: %s\n", AksjonspunktKodeDefinisjon.class, AksjonspunktDefinisjon.class, sjekkKodeBrukMap);
        }
    }

    @JsonIgnore
    private AksjonspunktType aksjonspunktType = AksjonspunktType.UDEFINERT;

    /**
     * Definerer hvorvidt Aksjonspunktet default krever totrinnsbehandling. Dvs. Beslutter må godkjenne hva
     * Saksbehandler har utført.
     */
    @JsonIgnore
    private boolean defaultTotrinnBehandling = false;

    @JsonIgnore
    private boolean kanOverstyreTotrinnEtterLukking = false;

    /**
     * Hvorvidt aksjonspunktet har en frist før det må være løst. Brukes i forbindelse med når Behandling er lagt til
     * Vent.
     */
    @JsonIgnore
    private String fristPeriode;

    @JsonIgnore
    private VilkårType vilkårType;

    @JsonIgnore
    private SkjermlenkeType skjermlenkeType;

    @JsonIgnore
    private boolean tilbakehoppVedGjenopptakelse;

    @JsonIgnore
    private BehandlingStegType behandlingStegType;

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private boolean erUtgått = false;

    private String kode;

    @JsonIgnore
    private Set<BehandlingStatus> behandlingStatus;

    @JsonIgnore
    private boolean skalAvbrytesVedTilbakeføring = true;

    @JsonIgnore
    private Ventekategori defaultVentekategori;

    AksjonspunktDefinisjon() {
        // for hibernate
    }

    /**
     * Brukes for utgåtte aksjonspunkt. Disse skal ikke kunne gjenoppstå, men må kunne leses
     */
    private AksjonspunktDefinisjon(String kode, AksjonspunktType type, String navn) {
        this.kode = kode;
        this.aksjonspunktType = type;
        this.navn = navn;
        erUtgått = true;
    }

    // Bruk for ordinære aksjonspunkt og overstyring
    private AksjonspunktDefinisjon(String kode,
                                   AksjonspunktType aksjonspunktType,
                                   String navn,
                                   BehandlingStatus behandlingStatus,
                                   BehandlingStegType behandlingStegType,
                                   VilkårType vilkårType,
                                   SkjermlenkeType skjermlenkeType,
                                   boolean defaultTotrinnBehandling,
                                   Ventekategori defaultVentekategori) {
        this.kode = Objects.requireNonNull(kode);
        this.behandlingStatus = Set.of(behandlingStatus);
        this.navn = navn;
        this.aksjonspunktType = aksjonspunktType;
        this.behandlingStegType = behandlingStegType;
        this.vilkårType = vilkårType;
        this.defaultTotrinnBehandling = defaultTotrinnBehandling;
        this.skjermlenkeType = skjermlenkeType;
        this.tilbakehoppVedGjenopptakelse = false;
        this.fristPeriode = null;
        this.defaultVentekategori = defaultVentekategori;
    }

    // Bruk for ordinære aksjonspunkt og overstyring
    private AksjonspunktDefinisjon(String kode,
                                   AksjonspunktType aksjonspunktType,
                                   String navn,
                                   Set<BehandlingStatus> behandlingStatus,
                                   BehandlingStegType behandlingStegType,
                                   VilkårType vilkårType,
                                   SkjermlenkeType skjermlenkeType,
                                   boolean defaultTotrinnBehandling,
                                   Ventekategori defaultVentekategori) {
        this.kode = Objects.requireNonNull(kode);
        this.behandlingStatus = behandlingStatus;
        this.navn = navn;
        this.aksjonspunktType = aksjonspunktType;
        this.behandlingStegType = behandlingStegType;
        this.vilkårType = vilkårType;
        this.defaultTotrinnBehandling = defaultTotrinnBehandling;
        this.skjermlenkeType = skjermlenkeType;
        this.tilbakehoppVedGjenopptakelse = false;
        this.fristPeriode = null;
        this.defaultVentekategori = defaultVentekategori;
    }

    // Bruk for autopunkt i 7nnn serien
    private AksjonspunktDefinisjon(String kode,
                                   AksjonspunktType aksjonspunktType,
                                   String navn,
                                   BehandlingStatus behandlingStatus,
                                   BehandlingStegType behandlingStegType,
                                   VilkårType vilkårType,
                                   SkjermlenkeType skjermlenkeType,
                                   boolean defaultTotrinnBehandling,
                                   boolean tilbakehoppVedGjenopptakelse,
                                   String fristPeriode,
                                   Ventekategori defaultVentekategori) {
        this.kode = Objects.requireNonNull(kode);
        this.navn = navn;
        this.aksjonspunktType = aksjonspunktType;
        this.behandlingStatus = Set.of(behandlingStatus);
        this.behandlingStegType = behandlingStegType;
        this.vilkårType = vilkårType;
        this.defaultTotrinnBehandling = defaultTotrinnBehandling;
        this.skjermlenkeType = skjermlenkeType;
        this.tilbakehoppVedGjenopptakelse = tilbakehoppVedGjenopptakelse;
        this.fristPeriode = fristPeriode;
        this.defaultVentekategori = defaultVentekategori;
    }

    private AksjonspunktDefinisjon(String kode,
                                   AksjonspunktType aksjonspunktType,
                                   String navn,
                                   Set<BehandlingStatus> behandlingStatus,
                                   BehandlingStegType behandlingStegType,
                                   VilkårType vilkårType,
                                   SkjermlenkeType skjermlenkeType,
                                   boolean defaultTotrinnBehandling,
                                   boolean tilbakehoppVedGjenopptakelse,
                                   boolean skalAvbrytesVedTilbakeføring,
                                   Ventekategori defaultVentekategori) {
        this.kode = Objects.requireNonNull(kode);
        this.navn = navn;
        this.aksjonspunktType = aksjonspunktType;
        this.behandlingStatus = behandlingStatus;
        this.behandlingStegType = behandlingStegType;
        this.vilkårType = vilkårType;
        this.defaultTotrinnBehandling = defaultTotrinnBehandling;
        this.skjermlenkeType = skjermlenkeType;
        this.tilbakehoppVedGjenopptakelse = tilbakehoppVedGjenopptakelse;
        this.skalAvbrytesVedTilbakeføring = skalAvbrytesVedTilbakeføring;
        this.defaultVentekategori = defaultVentekategori;
    }

    private AksjonspunktDefinisjon(String kode,
                                   AksjonspunktType aksjonspunktType,
                                   String navn,
                                   BehandlingStatus behandlingStatus,
                                   BehandlingStegType behandlingStegType,
                                   VilkårType vilkårType,
                                   SkjermlenkeType skjermlenkeType,
                                   boolean defaultTotrinnBehandling,
                                   boolean kanOverstyreTotrinnEtterLukking,
                                   boolean tilbakehoppVedGjenopptakelse,
                                   boolean skalAvbrytesVedTilbakeføring,
                                   Ventekategori defaultVentekategori) {
        this.kode = Objects.requireNonNull(kode);
        this.navn = navn;
        this.aksjonspunktType = aksjonspunktType;
        this.behandlingStatus = Set.of(behandlingStatus);
        this.behandlingStegType = behandlingStegType;
        this.vilkårType = vilkårType;
        this.defaultTotrinnBehandling = defaultTotrinnBehandling;
        this.kanOverstyreTotrinnEtterLukking = kanOverstyreTotrinnEtterLukking;
        this.skjermlenkeType = skjermlenkeType;
        this.tilbakehoppVedGjenopptakelse = tilbakehoppVedGjenopptakelse;
        this.skalAvbrytesVedTilbakeføring = skalAvbrytesVedTilbakeføring;
        this.defaultVentekategori = defaultVentekategori;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static AksjonspunktDefinisjon fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(AksjonspunktDefinisjon.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent AksjonspunktDefinisjon: " + kode);
        }
        return ad;
    }

    public static Map<String, AksjonspunktDefinisjon> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static List<AksjonspunktDefinisjon> finnAksjonspunktDefinisjoner(BehandlingStegType behandlingStegType) {
        return KODER.values().stream()
            .filter(ad -> Objects.equals(ad.getBehandlingSteg(), behandlingStegType))
            .collect(Collectors.toList());
    }

    public static void main(String[] args) {

        var sb = new StringBuilder(100 * 1000);

        sb.append("kode,type,navn,defaultTotrinn,behandlingSteg\n");

        for (var v : values()) {
            var k = v.getKode();

            var sb2 = new StringBuilder(300);
            sb2.append(k).append(",");
            sb2.append(v.aksjonspunktType.getKode()).append(",");
            String navn = v.navn == null ? "" : "\"" + v.navn + "\"";
            sb2.append(navn).append(",");
            sb2.append(v.defaultTotrinnBehandling).append(",");
            sb2.append(v.behandlingStegType == null ? "" : v.behandlingStegType.getKode());

            sb.append(sb2).append("\n");

        }

        System.out.println(sb);
    }

    /**
     * @deprecated Bruk heller
     * {@link no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder#medSkjermlenke(SkjermlenkeType)}
     * direkte og unngå å slå opp fra aksjonspunktdefinisjon
     */
    @Deprecated
    public SkjermlenkeType getSkjermlenkeType() {
        return skjermlenkeType;
    }

    public AksjonspunktType getAksjonspunktType() {
        return Objects.equals(AksjonspunktType.UDEFINERT, aksjonspunktType) ? null : aksjonspunktType;
    }

    public boolean erAutopunkt() {
        return AksjonspunktType.AUTOPUNKT.equals(getAksjonspunktType());
    }

    public boolean getDefaultTotrinnBehandling() {
        return defaultTotrinnBehandling;
    }

    public Period getFristPeriod() {
        return (fristPeriode == null ? null : Period.parse(fristPeriode));
    }

    public VilkårType getVilkårType() {
        return (Objects.equals(VilkårType.UDEFINERT, vilkårType) ? null : vilkårType);
    }

    public boolean tilbakehoppVedGjenopptakelse() {
        return tilbakehoppVedGjenopptakelse;
    }

    /**
     * Returnerer kode verdi for aksjonspunkt utelukket av denne.
     */
    public Set<String> getUtelukkendeApdef() {
        return Set.of();
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    public Ventekategori getDefaultVentekategori() {
        return defaultVentekategori;
    }


    @Override
    public String getNavn() {
        return navn;
    }

    public boolean validerGyldigStatusEndring(AksjonspunktStatus aksjonspunktStatus, BehandlingStatus status) {
        return behandlingStatus.contains(status) || isFatterVedtak(aksjonspunktStatus, status);
    }

    private boolean isFatterVedtak(AksjonspunktStatus aksjonspunktStatus, BehandlingStatus status) {
        // I FatterVedtak kan beslutter reåpne (derav OPPRETTET) eksisterende aksjonspunkter før det sendes tilbake til saksbehandler
        return Objects.equals(BehandlingStatus.FATTER_VEDTAK, status)
            && Objects.equals(aksjonspunktStatus, AksjonspunktStatus.OPPRETTET);
    }

    @JsonProperty(value = "kode")
    @Override
    public String getKode() {
        return kode;
    }

    public boolean getSkalAvbrytesVedTilbakeføring() {
        return skalAvbrytesVedTilbakeføring;
    }

    public BehandlingStegType getBehandlingSteg() {
        return behandlingStegType;
    }

    public Set<BehandlingStatus> getGyldigBehandlingStatus() {
        return behandlingStatus;
    }

    public boolean kanOverstyreTotrinnEtterLukking() {
        return kanOverstyreTotrinnEtterLukking;
    }

    @JsonProperty(value = "kodeverk", access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    /**
     * Aksjonspunkt tidligere brukt, nå utgått (kan ikke gjenoppstå).
     */
    public boolean erUtgått() {
        return erUtgått;
    }

    /**
     * toString is set to output the kode value of the enum instead of the default that is the enum name.
     * This makes the generated openapi spec correct when the enum is used as a query param. Without this the generated
     * spec incorrectly specifies that it is the enum name string that should be used as input.
     */
    @Override
    public String toString() {
        return this.getKode();
    }

}
