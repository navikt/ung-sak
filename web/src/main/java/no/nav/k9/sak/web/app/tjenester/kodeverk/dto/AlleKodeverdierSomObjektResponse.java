package no.nav.k9.sak.web.app.tjenester.kodeverk.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.*;
import no.nav.k9.kodeverk.behandling.*;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.historikk.*;
import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.k9.kodeverk.medlem.MedlemskapType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.kodeverk.uttak.UtenlandsoppholdÅrsak;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.k9.sak.kontrakt.krav.ÅrsakTilVurdering;

import java.util.SortedMap;
import java.util.SortedSet;

public record AlleKodeverdierSomObjektResponse(
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<RelatertYtelseTilstand>> relatertYtelseTilstander,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<FagsakStatus>> fagsakStatuser,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<FagsakYtelseType>> fagsakYtelseTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<BehandlingÅrsakType>> behandlingÅrsakTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<HistorikkBegrunnelseType>> historikkBegrunnelseTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<OppgaveÅrsak>> oppgaveÅrsaker,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<MedlemskapManuellVurderingType>> medlemskapManuellVurderingTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<BehandlingResultatType>> behandlingResultatTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<PersonstatusType>> personstatusTyper,
    @NotNull @Valid @Size SortedSet<VenteårsakSomObjekt> venteårsaker,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<BehandlingType>> behandlingTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<ArbeidType>> arbeidTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<OpptjeningAktivitetType>> opptjeningAktivitetTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<RevurderingVarslingÅrsak>> revurderingVarslingÅrsaker,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<Inntektskategori>> inntektskategorier,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<AktivitetStatus>> aktivitetStatuser,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<Arbeidskategori>> arbeidskategorier,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<Fagsystem>> fagsystemer,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<SivilstandType>> sivilstandTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<FaktaOmBeregningTilfelle>> faktaOmBeregningTilfeller,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<SkjermlenkeType>> skjermlenkeTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<ArbeidsforholdHandlingType>> arbeidsforholdHandlingTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<HistorikkOpplysningType>> historikkOpplysningTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<HistorikkEndretFeltType>> historikkEndretFeltTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<HistorikkEndretFeltVerdiType>> historikkEndretFeltVerdiTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<HistorikkinnslagType>> historikkinnslagTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<HistorikkAktør>> historikkAktører,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<HistorikkAvklartSoeknadsperiodeType>> historikkAvklartSoeknadsperiodeTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<HistorikkResultatType>> historikkResultatTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<BehandlingStatus>> behandlingStatuser,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<MedlemskapDekningType>> medlemskapDekningTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<MedlemskapType>> medlemskapTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<Avslagsårsak>> avslagsårsaker,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<KonsekvensForYtelsen>> konsekvenserForYtelsen,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<VilkårType>> vilkårTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<PermisjonsbeskrivelseType>> permisjonsbeskrivelseTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<VurderArbeidsforholdHistorikkinnslag>> vurderArbeidsforholdHistorikkinnslag,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<TilbakekrevingVidereBehandling>> tilbakekrevingVidereBehandlinger,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<VurderÅrsak>> vurderingsÅrsaker,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<Region>> regioner,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<Landkoder>> landkoder,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<Språkkode>> språkkoder,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<VedtakResultatType>> vedtakResultatTyper,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<DokumentTypeId>> dokumentTypeIder,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<UtenlandsoppholdÅrsak>> utenlandsoppholdÅrsaker,
    @NotNull @Valid @Size SortedSet<KodeverdiSomObjekt<ÅrsakTilVurdering>> årsakerTilVurdering,
    // avslagsårsakerPrVilkårTypeKode er eit spesialtilfelle der ein returnerer mapping frå VilkårType til tilknytta Avslagsårsak
    @NotNull @Valid @Size SortedMap<String, SortedSet<KodeverdiSomObjekt<Avslagsårsak>>> avslagårsakerPrVilkårTypeKode
    ) {
}
