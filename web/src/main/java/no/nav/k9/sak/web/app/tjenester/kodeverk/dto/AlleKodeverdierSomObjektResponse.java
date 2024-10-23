package no.nav.k9.sak.web.app.tjenester.kodeverk.dto;

import jakarta.validation.constraints.NotNull;
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
    @NotNull SortedSet<KodeverdiSomObjekt<RelatertYtelseTilstand>> relatertYtelseTilstander,
    @NotNull SortedSet<KodeverdiSomObjekt<FagsakStatus>> fagsakStatuser,
    @NotNull SortedSet<KodeverdiSomObjekt<FagsakYtelseType>> fagsakYtelseTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<BehandlingÅrsakType>> behandlingÅrsakTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<HistorikkBegrunnelseType>> historikkBegrunnelseTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<OppgaveÅrsak>> oppgaveÅrsaker,
    @NotNull SortedSet<KodeverdiSomObjekt<MedlemskapManuellVurderingType>> medlemskapManuellVurderingTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<BehandlingResultatType>> behandlingResultatTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<PersonstatusType>> personstatusTyper,
    @NotNull SortedSet<VenteårsakSomObjekt> venteårsaker,
    @NotNull SortedSet<KodeverdiSomObjekt<BehandlingType>> behandlingTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<ArbeidType>> arbeidTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<OpptjeningAktivitetType>> opptjeningAktivitetTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<RevurderingVarslingÅrsak>> revurderingVarslingÅrsaker,
    @NotNull SortedSet<KodeverdiSomObjekt<Inntektskategori>> inntektskategorier,
    @NotNull SortedSet<KodeverdiSomObjekt<AktivitetStatus>> aktivitetStatuser,
    @NotNull SortedSet<KodeverdiSomObjekt<Arbeidskategori>> arbeidskategorier,
    @NotNull SortedSet<KodeverdiSomObjekt<Fagsystem>> fagsystemer,
    @NotNull SortedSet<KodeverdiSomObjekt<SivilstandType>> sivilstandTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<FaktaOmBeregningTilfelle>> faktaOmBeregningTilfeller,
    @NotNull SortedSet<KodeverdiSomObjekt<SkjermlenkeType>> skjermlenkeTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<ArbeidsforholdHandlingType>> arbeidsforholdHandlingTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<HistorikkOpplysningType>> historikkOpplysningTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<HistorikkEndretFeltType>> historikkEndretFeltTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<HistorikkEndretFeltVerdiType>> historikkEndretFeltVerdiTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<HistorikkinnslagType>> historikkinnslagTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<HistorikkAktør>> historikkAktører,
    @NotNull SortedSet<KodeverdiSomObjekt<HistorikkAvklartSoeknadsperiodeType>> historikkAvklartSoeknadsperiodeTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<HistorikkResultatType>> historikkResultatTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<BehandlingStatus>> behandlingStatuser,
    @NotNull SortedSet<KodeverdiSomObjekt<MedlemskapDekningType>> medlemskapDekningTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<MedlemskapType>> medlemskapTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<Avslagsårsak>> avslagsårsaker,
    @NotNull SortedSet<KodeverdiSomObjekt<KonsekvensForYtelsen>> konsekvenserForYtelsen,
    @NotNull SortedSet<KodeverdiSomObjekt<VilkårType>> vilkårTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<PermisjonsbeskrivelseType>> permisjonsbeskrivelseTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<VurderArbeidsforholdHistorikkinnslag>> vurderArbeidsforholdHistorikkinnslag,
    @NotNull SortedSet<KodeverdiSomObjekt<TilbakekrevingVidereBehandling>> tilbakekrevingVidereBehandlinger,
    @NotNull SortedSet<KodeverdiSomObjekt<VurderÅrsak>> vurderingsÅrsaker,
    @NotNull SortedSet<KodeverdiSomObjekt<Region>> regioner,
    @NotNull SortedSet<KodeverdiSomObjekt<Landkoder>> landkoder,
    @NotNull SortedSet<KodeverdiSomObjekt<Språkkode>> språkkoder,
    @NotNull SortedSet<KodeverdiSomObjekt<VedtakResultatType>> vedtakResultatTyper,
    @NotNull SortedSet<KodeverdiSomObjekt<DokumentTypeId>> dokumentTypeIder,
    @NotNull SortedSet<KodeverdiSomObjekt<UtenlandsoppholdÅrsak>> utenlandsoppholdÅrsaker,
    @NotNull SortedSet<KodeverdiSomObjekt<ÅrsakTilVurdering>> årsakerTilVurdering,
    // avslagsårsakerPrVilkårTypeKode er eit spesialtilfelle der ein returnerer mapping frå VilkårType til tilknytta Avslagsårsak
    @NotNull SortedMap<String, SortedSet<KodeverdiSomObjekt<Avslagsårsak>>> avslagårsakerPrVilkårTypeKode
    ) {
}
