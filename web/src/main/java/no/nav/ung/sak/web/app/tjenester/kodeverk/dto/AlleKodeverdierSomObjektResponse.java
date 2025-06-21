package no.nav.ung.sak.web.app.tjenester.kodeverk.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.Fagsystem;

import no.nav.ung.kodeverk.KodeverdiSomObjekt;
import no.nav.ung.kodeverk.arbeidsforhold.*;
import no.nav.ung.kodeverk.behandling.*;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.ung.kodeverk.dokument.DokumentTypeId;
import no.nav.ung.kodeverk.geografisk.Språkkode;
import no.nav.ung.kodeverk.historikk.*;
import no.nav.ung.kodeverk.medlem.MedlemskapDekningType;
import no.nav.ung.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.ung.kodeverk.medlem.MedlemskapType;
import no.nav.ung.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.ung.sak.kontrakt.krav.ÅrsakTilVurdering;

import java.util.SortedMap;
import java.util.SortedSet;

public record AlleKodeverdierSomObjektResponse(
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<RelatertYtelseTilstand>> relatertYtelseTilstander,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<FagsakStatus>> fagsakStatuser,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<FagsakYtelseType>> fagsakYtelseTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<BehandlingÅrsakType>> behandlingÅrsakTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<HistorikkBegrunnelseType>> historikkBegrunnelseTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<OppgaveÅrsak>> oppgaveÅrsaker,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<MedlemskapManuellVurderingType>> medlemskapManuellVurderingTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<BehandlingResultatType>> behandlingResultatTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<VenteårsakSomObjekt> venteårsaker,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<BehandlingType>> behandlingTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<ArbeidType>> arbeidTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<OpptjeningAktivitetType>> opptjeningAktivitetTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<RevurderingVarslingÅrsak>> revurderingVarslingÅrsaker,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<Inntektskategori>> inntektskategorier,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<AktivitetStatus>> aktivitetStatuser,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<Arbeidskategori>> arbeidskategorier,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<Fagsystem>> fagsystemer,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<SkjermlenkeType>> skjermlenkeTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<HistorikkOpplysningType>> historikkOpplysningTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<HistorikkEndretFeltType>> historikkEndretFeltTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<HistorikkEndretFeltVerdiType>> historikkEndretFeltVerdiTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<HistorikkinnslagType>> historikkinnslagTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<HistorikkAktør>> historikkAktører,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<HistorikkAvklartSoeknadsperiodeType>> historikkAvklartSoeknadsperiodeTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<HistorikkResultatType>> historikkResultatTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<BehandlingStatus>> behandlingStatuser,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<MedlemskapDekningType>> medlemskapDekningTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<MedlemskapType>> medlemskapTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<Avslagsårsak>> avslagsårsaker,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<VilkårType>> vilkårTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<VurderArbeidsforholdHistorikkinnslag>> vurderArbeidsforholdHistorikkinnslag,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<TilbakekrevingVidereBehandling>> tilbakekrevingVidereBehandlinger,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<VurderÅrsak>> vurderingsÅrsaker,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<Språkkode>> språkkoder,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<VedtakResultatType>> vedtakResultatTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<DokumentTypeId>> dokumentTypeIder,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<ÅrsakTilVurdering>> årsakerTilVurdering,
    // avslagsårsakerPrVilkårTypeKode er eit spesialtilfelle der ein returnerer mapping frå VilkårType til tilknytta Avslagsårsak
    @NotNull @Valid @Size(min = 1, max = 1000) SortedMap<String, SortedSet<KodeverdiSomObjekt<Avslagsårsak>>> avslagårsakerPrVilkårTypeKode
) {
}
