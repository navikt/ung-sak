package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår;

import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;

import com.fasterxml.jackson.annotation.JsonRawValue;

public class VilkårDto {

    private VilkårType vilkarType;
    private Utfall vilkarStatus;
    private Properties merknadParametere;
    private String avslagKode;
    private String lovReferanse;
    private Boolean overstyrbar;

    @JsonRawValue
    @JsonInclude(Include.NON_NULL)
    private String evaluering;

    @JsonRawValue
    @JsonInclude(Include.NON_NULL)
    private String input;

    public VilkårDto(
                     VilkårType vilkårType,
                     Utfall utfall,
                     Properties merknadParametere,
                     String avslagKode,
                     String lovReferanse) {
        this.vilkarType = vilkårType;
        this.vilkarStatus = utfall;
        this.merknadParametere = merknadParametere;
        this.avslagKode = avslagKode;
        this.lovReferanse = lovReferanse;
    }

    public VilkårDto() {
    }

    public VilkårType getVilkarType() {
        return vilkarType;
    }

    public Utfall getVilkarStatus() {
        return vilkarStatus;
    }

    public Properties getMerknadParametere() {
        return merknadParametere;
    }

    public String getAvslagKode() {
        return avslagKode;
    }

    public String getLovReferanse() {
        return lovReferanse;
    }

    public String getEvaluering() {
        return evaluering;
    }

    public String getInput() {
        return input;
    }

    public void setVilkarType(VilkårType vilkarType) {
        this.vilkarType = vilkarType;
    }

    public void setVilkarStatus(Utfall vilkarStatus) {
        this.vilkarStatus = vilkarStatus;
    }

    public void setMerknadParametere(Properties merknadParametere) {
        this.merknadParametere = merknadParametere;
    }

    public void setAvslagKode(String avslagKode) {
        this.avslagKode = avslagKode;
    }

    public void setEvaluering(String evaluering) {
        this.evaluering = evaluering;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public void setLovReferanse(String lovReferanse) {
        this.lovReferanse = lovReferanse;
    }

    public void setOverstyrbar(boolean overstyrbar) {
        this.overstyrbar = overstyrbar;
    }

    public boolean isOverstyrbar() {
        return overstyrbar;
    }
}
