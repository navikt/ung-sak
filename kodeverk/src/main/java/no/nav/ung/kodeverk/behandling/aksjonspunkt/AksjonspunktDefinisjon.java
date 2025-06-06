package no.nav.ung.kodeverk.behandling.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonValue;
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
public enum AksjonspunktDefinisjon implements Kodeverdi {

    // Gruppe : 5xxx
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

    FORESLÅ_VEDTAK_MANUELT(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_MANUELT_KODE,
        AksjonspunktType.MANUELL, "Foreslå vedtak manuelt", BehandlingStatus.UTREDES, BehandlingStegType.FORESLÅ_VEDTAK, UTEN_VILKÅR,
        SkjermlenkeType.VEDTAK, ENTRINN, AVVENTER_SAKSBEHANDLER),

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
        "Manuelt satt på vent", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_KOMPLETTHET, UTEN_VILKÅR, UTEN_SKJERMLENKE,
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
        "Satt på vent etter kontroll av inntekt til rapporteringsfrist har passert", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_KOMPLETTHET, UTEN_VILKÅR,
        UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P2W", AVVENTER_SØKER),

    // Gruppe: 80xx
    KONTROLLER_INNTEKT(
        AksjonspunktKodeDefinisjon.KONTROLLER_INNTEKT_KODE, AksjonspunktType.MANUELL, "Kontroller inntekt",
        BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT,
        UTEN_VILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, AVVENTER_SAKSBEHANDLER),

    UNDEFINED,

    ;

    static final String KODEVERK = "AKSJONSPUNKT_DEF";

    private static final Map<String, AksjonspunktDefinisjon> KODER = new LinkedHashMap<>();


    static {
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

    private AksjonspunktType aksjonspunktType = AksjonspunktType.UDEFINERT;

    /**
     * Definerer hvorvidt Aksjonspunktet default krever totrinnsbehandling. Dvs. Beslutter må godkjenne hva
     * Saksbehandler har utført.
     */
    private boolean defaultTotrinnBehandling = false;

    private boolean kanOverstyreTotrinnEtterLukking = false;

    /**
     * Hvorvidt aksjonspunktet har en frist før det må være løst. Brukes i forbindelse med når Behandling er lagt til
     * Vent.
     */
    private String fristPeriode;

    private VilkårType vilkårType;

    private SkjermlenkeType skjermlenkeType;

    private boolean tilbakehoppVedGjenopptakelse;

    private BehandlingStegType behandlingStegType;

    private String navn;

    private boolean erUtgått = false;

    private String kode;

    private Set<BehandlingStatus> behandlingStatus;

    private boolean skalAvbrytesVedTilbakeføring = true;

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

    public static AksjonspunktDefinisjon fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
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

    @JsonValue
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

}
