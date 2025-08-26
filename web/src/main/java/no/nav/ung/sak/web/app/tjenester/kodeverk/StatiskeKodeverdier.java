package no.nav.ung.sak.web.app.tjenester.kodeverk;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.kodeverk.behandling.*;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.ung.kodeverk.geografisk.Språkkode;
import no.nav.ung.kodeverk.historikk.*;
import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.ung.sak.kontrakt.krav.ÅrsakTilVurdering;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public record StatiskeKodeverdier(
    @NotNull Set<FagsakStatus> fagsakStatuser,
    @NotNull Set<FagsakYtelseType> fagsakYtelseTyper,
    @NotNull Set<BehandlingÅrsakType> behandlingÅrsakTyper,
    @NotNull Set<OppgaveÅrsak> oppgaveÅrsaker,
    @NotNull Set<BehandlingResultatType> behandlingResultatTyper,
    @NotNull Set<Venteårsak> venteårsaker,
    @NotNull Set<BehandlingType> behandlingTyper,
    @NotNull Set<ArbeidType> arbeidTyper,
    @NotNull Set<RevurderingVarslingÅrsak> revurderingVarslingÅrsaker,
    @NotNull Set<Fagsystem> fagsystemer,
    @NotNull Set<SkjermlenkeType> skjermlenkeTyper,
    @NotNull Set<HistorikkAktør> historikkAktører,
    @NotNull Set<BehandlingStatus> behandlingStatuser,
    @NotNull Set<Avslagsårsak> avslagsårsaker,
    @NotNull Set<VilkårType> vilkårTyper,
    @NotNull Set<TilbakekrevingVidereBehandling> tilbakekrevingVidereBehandlinger,
    @NotNull Set<VurderÅrsak> vurderingsÅrsaker,
    @NotNull Set<Språkkode> språkkoder,
    @NotNull Set<VedtakResultatType> vedtakResultatTyper,
    @NotNull Set<ÅrsakTilVurdering> årsakerTilVurdering
) {

    // Eigentleg ikkje så nødvendig, men signaliserer tydleg at dei fleste verdier er enums som implementerer Kodeverdi.
    public static <K extends Enum<K> & Kodeverdi> Set<K> alleEnumVerdier(Class<K> k) {
        return EnumSet.allOf(k);
    }

    public static final StatiskeKodeverdier alle;

    static {
        alle = new StatiskeKodeverdier(
            alleEnumVerdier(FagsakStatus.class),
            alleEnumVerdier(FagsakYtelseType.class),
            alleEnumVerdier(BehandlingÅrsakType.class),
            alleEnumVerdier(OppgaveÅrsak.class),
            alleEnumVerdier(BehandlingResultatType.class),
            EnumSet.allOf(Venteårsak.class),
            alleEnumVerdier(BehandlingType.class),
            alleEnumVerdier(ArbeidType.class).stream().filter(v -> v.erAnnenOpptjening()).collect(Collectors.toSet()),
            alleEnumVerdier(RevurderingVarslingÅrsak.class),
            alleEnumVerdier(Fagsystem.class),
            alleEnumVerdier(SkjermlenkeType.class),
            alleEnumVerdier(HistorikkAktør.class),
            alleEnumVerdier(BehandlingStatus.class),
            alleEnumVerdier(Avslagsårsak.class),
            alleEnumVerdier(VilkårType.class),
            alleEnumVerdier(TilbakekrevingVidereBehandling.class),
            alleEnumVerdier(VurderÅrsak.class),
            new HashSet<>(Språkkode.kodeMap().values()),
            alleEnumVerdier(VedtakResultatType.class),
            alleEnumVerdier(ÅrsakTilVurdering.class)
        );
    }
}
