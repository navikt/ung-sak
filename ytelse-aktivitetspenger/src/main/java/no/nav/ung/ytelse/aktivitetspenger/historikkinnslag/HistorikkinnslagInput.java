package no.nav.ung.ytelse.aktivitetspenger.historikkinnslag;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;

import java.util.Optional;

public class HistorikkinnslagInput {
    private Long behandlingId;
    private SkjermlenkeType skjermlenkeType;
    private VilkårType vilkårType;
    private Boolean gjelderOpphør;
    private LocalDateTimeline<HistorikkinnslagData> eksisterendeVurderinger;
    private LocalDateTimeline<HistorikkinnslagData> nyeVurderinger;
    private HistorikkAktør historikkAktør;
    private String saksbehandlerIdent;

    public HistorikkinnslagInput setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
        return this;
    }

    public HistorikkinnslagInput setSkjermlenkeType(SkjermlenkeType skjermlenkeType) {
        this.skjermlenkeType = skjermlenkeType;
        return this;
    }

    public HistorikkinnslagInput setVilkårType(VilkårType vilkårType) {
        this.vilkårType = vilkårType;
        return this;
    }

    public HistorikkinnslagInput setGjelderOpphør(Boolean gjelderOpphør) {
        this.gjelderOpphør = gjelderOpphør;
        return this;
    }

    public HistorikkinnslagInput setEksisterendeVilkårVurderinger(LocalDateTimeline<VilkårsvurderingResultat> eksisterendeVurderinger) {
        this.eksisterendeVurderinger = eksisterendeVurderinger
            .filterValue(vr -> !erAvkortet(vr))
            .mapValue(vr -> new HistorikkinnslagData(vr.godkjent() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT, vr.ikkeOppfyltÅrsak()));
        return this;
    }

    private static boolean erAvkortet(VilkårsvurderingResultat vilkårsvurderingResultat){
        return vilkårsvurderingResultat.ikkeOppfyltÅrsak() != null && Optional.of(Avslagsårsak.AVKORTET).equals(vilkårsvurderingResultat.ikkeOppfyltÅrsak().avslagsårsak());
    }

    public HistorikkinnslagInput setEksisterendeVurderinger(LocalDateTimeline<HistorikkinnslagData> eksisterendeVurderinger) {
        this.eksisterendeVurderinger = eksisterendeVurderinger;
        return this;
    }

    public HistorikkinnslagInput setNyeVilkårVurderinger(LocalDateTimeline<VilkårsvurderingResultat> nyeVurderinger) {
        this.nyeVurderinger = nyeVurderinger
            .filterValue(vr -> !erAvkortet(vr))
            .mapValue(vr -> new HistorikkinnslagData(vr.godkjent() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT, vr.ikkeOppfyltÅrsak()));
        return this;
    }

    public HistorikkinnslagInput setNyeVurderinger(LocalDateTimeline<HistorikkinnslagData> nyeVurderinger) {
        this.nyeVurderinger = nyeVurderinger;
        return this;
    }

    public HistorikkinnslagInput setHistorikkAktør(HistorikkAktør historikkAktør) {
        this.historikkAktør = historikkAktør;
        return this;
    }

    public HistorikkinnslagInput setSaksbehandlerIdent(String saksbehandlerIdent) {
        this.saksbehandlerIdent = saksbehandlerIdent;
        return this;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public SkjermlenkeType getSkjermlenkeType() {
        return skjermlenkeType;
    }

    public VilkårType getVilkårType() {
        return vilkårType;
    }

    public Boolean getGjelderOpphør() {
        return gjelderOpphør;
    }

    public LocalDateTimeline<HistorikkinnslagData> getEksisterendeVurderinger() {
        return eksisterendeVurderinger;
    }

    public LocalDateTimeline<HistorikkinnslagData> getNyeVurderinger() {
        return nyeVurderinger;
    }

    public HistorikkAktør getHistorikkAktør() {
        return historikkAktør;
    }

    public String getSaksbehandlerIdent() {
        return saksbehandlerIdent;
    }
}
