package no.nav.ung.sak.web.app.tjenester.kodeverk.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.KodeverdiSomObjekt;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.kodeverk.behandling.*;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.ung.kodeverk.geografisk.Språkkode;
import no.nav.ung.kodeverk.historikk.*;
import no.nav.ung.kodeverk.klage.KlageAvvistÅrsak;
import no.nav.ung.kodeverk.klage.KlageMedholdÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.ung.sak.kontrakt.krav.ÅrsakTilVurdering;

import java.util.SortedMap;
import java.util.SortedSet;

public record AlleKodeverdierSomObjektResponse(
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<FagsakStatus>> fagsakStatuser,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<FagsakYtelseType>> fagsakYtelseTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<BehandlingÅrsakType>> behandlingÅrsakTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<OppgaveÅrsak>> oppgaveÅrsaker,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<BehandlingResultatType>> behandlingResultatTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<VenteårsakSomObjekt> venteårsaker,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<BehandlingType>> behandlingTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<ArbeidType>> arbeidTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<RevurderingVarslingÅrsak>> revurderingVarslingÅrsaker,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<Fagsystem>> fagsystemer,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<SkjermlenkeType>> skjermlenkeTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<HistorikkAktør>> historikkAktører,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<BehandlingStatus>> behandlingStatuser,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<Avslagsårsak>> avslagsårsaker,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<VilkårType>> vilkårTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<TilbakekrevingVidereBehandling>> tilbakekrevingVidereBehandlinger,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<VurderÅrsak>> vurderingsÅrsaker,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<Språkkode>> språkkoder,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<VedtakResultatType>> vedtakResultatTyper,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<ÅrsakTilVurdering>> årsakerTilVurdering,
    // avslagsårsakerPrVilkårTypeKode er eit spesialtilfelle der ein returnerer mapping frå VilkårType til tilknytta Avslagsårsak
    @NotNull @Valid @Size(min = 1, max = 1000) SortedMap<String, SortedSet<KodeverdiSomObjekt<Avslagsårsak>>> avslagårsakerPrVilkårTypeKode,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<KlageMedholdÅrsak>> klageMedholdÅrsak,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<KlageAvvistÅrsak>> klageAvvistÅrsaker,
    @NotNull @Valid @Size(min = 1, max = 1000) SortedSet<KodeverdiSomObjekt<KlageVurderingType>> klagevurderingType
) {
}
