package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import java.time.LocalDate;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

public abstract class VarselRevurderingDto extends BekreftetAksjonspunktDto {
    private boolean sendVarsel;

    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message="'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String fritekst;

    private LocalDate frist;

    private Venteårsak ventearsak;

    public VarselRevurderingDto(String begrunnelse, boolean sendVarsel,
            String fritekst, LocalDate frist, Venteårsak ventearsak) {
        super(begrunnelse);
        this.sendVarsel = sendVarsel;
        this.fritekst = fritekst;
        this.frist = frist;
        this.ventearsak = ventearsak;
    }

    protected VarselRevurderingDto() {
        super();
    }

    public boolean isSendVarsel() {
        return sendVarsel;
    }

    public String getFritekst() {
        return fritekst;
    }

    public LocalDate getFrist() {
        return frist;
    }

    public Venteårsak getVentearsak() {
        return ventearsak;
    }
}
