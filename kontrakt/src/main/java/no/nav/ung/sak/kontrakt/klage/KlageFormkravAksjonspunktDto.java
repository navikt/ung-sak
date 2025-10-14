package no.nav.ung.sak.kontrakt.klage;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.ung.sak.kontrakt.behandling.part.PartDto;

import java.util.Objects;
import java.util.UUID;

public abstract class KlageFormkravAksjonspunktDto extends BekreftetAksjonspunktDto {

    @NotNull
    @JsonProperty(value = "erKlagerPart", required = true)
    private boolean erKlagerPart;
    @NotNull
    @JsonProperty(value = "erFristOverholdt", required = true)
    private boolean erFristOverholdt;
    @NotNull
    @JsonProperty(value = "erKonkret", required = true)
    private boolean erKonkret;

    @NotNull
    @JsonProperty(value = "erSignert", required = true)
    private boolean erSignert;

    @JsonProperty("erTilbakekreving")
    private boolean erTilbakekreving;

    @JsonProperty("påklagdBehandlingInfo")
    @Valid
    private PåklagdBehandlingDto påklagdBehandlingInfo;

    @JsonProperty(value = "begrunnelse")
    @Size(max = 2000)
    @Pattern(regexp = TekstValideringRegex.FRITEKST)
    private String begrunnelse;

    @Valid
    @JsonProperty("valgtKlagePart")
    private PartDto valgtKlagePart;


    KlageFormkravAksjonspunktDto() { // NOSONAR
        // For Jackson
    }

    public KlageFormkravAksjonspunktDto(boolean erKlagerPart,
                                        boolean erFristOverholdt,
                                        boolean erKonkret,
                                        boolean erSignert,
                                        String begrunnelse,
                                        PartDto valgtKlagePart,
                                        boolean erTilbakekreving,
                                        PåklagdBehandlingDto påklagdBehandlingInfo) {
        super(begrunnelse);
        this.erKlagerPart = erKlagerPart;
        this.erFristOverholdt = erFristOverholdt;
        this.erKonkret = erKonkret;
        this.erSignert = erSignert;
        this.erTilbakekreving = erTilbakekreving;
        this.påklagdBehandlingInfo = påklagdBehandlingInfo;
        this.valgtKlagePart = valgtKlagePart;
        this.begrunnelse = Objects.requireNonNull(begrunnelse, "begrunnelse");
    }

    public boolean erKlagerPart() {
        return erKlagerPart;
    }

    public boolean erFristOverholdt() {
        return erFristOverholdt;
    }

    public boolean erKonkret() {
        return erKonkret;
    }

    public boolean erSignert() {
        return erSignert;
    }

    public UUID hentPåklagdBehandlingUuid() {
        return påklagdBehandlingInfo != null ? påklagdBehandlingInfo.getPåklagBehandlingUuid() : null;
    }

    @Override
    public String getBegrunnelse() {
        return begrunnelse;
    }

    public boolean erTilbakekreving(){
        return erTilbakekreving;
    }

    public PåklagdBehandlingDto getPåklagdBehandlingInfo() {
        return påklagdBehandlingInfo;
    }

    public PartDto getValgtKlagePart() {
        return valgtKlagePart;
    }

    public void setValgtKlagePart(PartDto valgtKlagePart) {
        this.valgtKlagePart = valgtKlagePart;
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_KODE)
    public static class KlageFormkravNfpAksjonspunktDto extends KlageFormkravAksjonspunktDto {


        KlageFormkravNfpAksjonspunktDto() {
            super();
        }

        public KlageFormkravNfpAksjonspunktDto(boolean erKlagerPart,
                                               boolean erFristOverholdt,
                                               boolean erKonkret,
                                               boolean erSignert,
                                               String begrunnelse,
                                               PartDto valgtKlagepart,
                                               boolean erTilbakekreving,
                                               PåklagdBehandlingDto påklagdBehandling) {
            super(erKlagerPart, erFristOverholdt, erKonkret, erSignert, begrunnelse, valgtKlagepart, erTilbakekreving, påklagdBehandling);
        }
    }
}
